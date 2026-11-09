package com.medusko.lagatron.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.medusko.lagatron.MainActivity
import com.medusko.lagatron.R
import com.medusko.lagatron.data.AppPreferences
import com.medusko.lagatron.data.PingHistoryRepository
import com.medusko.lagatron.model.LatencyStatus
import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "lagatron_monitor_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.medusko.lagatron.ACTION_START"
        const val ACTION_STOP = "com.medusko.lagatron.ACTION_STOP"
        const val ACTION_MANUAL_REFRESH = "com.medusko.lagatron.ACTION_MANUAL_REFRESH"
        const val ACTION_UPDATE_CONFIG = "com.medusko.lagatron.ACTION_UPDATE_CONFIG"
        const val ACTION_UPDATE_POSITION = "com.medusko.lagatron.ACTION_UPDATE_POSITION"

        fun start(context: Context) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun manualRefresh(context: Context) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_MANUAL_REFRESH
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateConfig(context: Context) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_UPDATE_CONFIG
            }
            context.startService(intent)
        }

        fun updatePosition(context: Context, x: Int, y: Int) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_UPDATE_POSITION
                putExtra("x", x)
                putExtra("y", y)
            }
            context.startService(intent)
        }
    }

    private lateinit var prefs: AppPreferences
    private lateinit var overlayController: OverlayController
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var pingJob: Job? = null
    private var lastResult: PingResult? = null

    // Cached notification artifacts
    private var openAppPendingIntent: PendingIntent? = null
    private var stopPendingIntent: PendingIntent? = null
    private var refreshPendingIntent: PendingIntent? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var lastNotifiedStatus: LatencyStatus? = null
    private var lastNotifiedHost: String = ""
    private var lastNotifiedInterval: Long = 1000L
    private var lastNotificationTimeMs: Long = 0

    // Screen State Receiver to save battery when screen is locked/off
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    PingHistoryRepository.recordInterruption("Screen Off")
                    pausePingLoop()
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (prefs.isServiceRunning) {
                        PingHistoryRepository.recordInterruption("Screen On")
                        startPingLoop()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        overlayController = OverlayController(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initPendingIntents()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun initPendingIntents() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = Intent(this, PingForegroundService::class.java).apply {
            action = ACTION_MANUAL_REFRESH
        }
        refreshPendingIntent = PendingIntent.getService(
            this,
            2,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
            }
            ACTION_MANUAL_REFRESH -> {
                serviceScope.launch {
                    val host = prefs.getActiveHost()
                    val result = PingEngine.ping(host)
                    lastResult = result
                    PingHistoryRepository.recordPing(result)
                    overlayController.updatePing(result, prefs.displayMode, prefs.appFont)
                    throttledUpdateNotification(result)
                }
            }
            ACTION_UPDATE_POSITION -> {
                val x = intent.getIntExtra("x", prefs.overlayX)
                val y = intent.getIntExtra("y", prefs.overlayY)
                overlayController.setPosition(x, y)
            }
            ACTION_UPDATE_CONFIG -> {
                overlayController.applyTypographyAndMode(prefs.displayMode, prefs.appFont)
                lastResult?.let {
                    overlayController.updatePing(it, prefs.displayMode, prefs.appFont)
                }
                // Check if host or interval changed
                if ((prefs.getActiveHost() != lastNotifiedHost || prefs.intervalMs != lastNotifiedInterval) && prefs.isServiceRunning) {
                    startPingLoop()
                }
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (!Settings.canDrawOverlays(this)) {
            stopMonitoring()
            return
        }
        prefs.isServiceRunning = true
        PingHistoryRepository.setServiceRunning(true)
        PingHistoryRepository.recordInterruption("Started")
        val initialNotification = getOrCreateNotification("Starting ping monitor (${prefs.getActiveHost()})...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        overlayController.show()
        startPingLoop()
    }

    private fun pausePingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun startPingLoop() {
        pausePingLoop()
        val host = prefs.getActiveHost()
        val interval = prefs.intervalMs
        if (host != lastNotifiedHost && lastNotifiedHost.isNotEmpty()) {
            PingHistoryRepository.recordInterruption("Target: $host")
        }
        lastNotifiedHost = host
        lastNotifiedInterval = interval

        pingJob = serviceScope.launch {
            PingEngine.stream(host, interval).collect { result ->
                if (!isActive || !prefs.isServiceRunning) return@collect

                lastResult = result
                PingHistoryRepository.recordPing(result)
                overlayController.updatePing(result, prefs.displayMode, prefs.appFont)
                throttledUpdateNotification(result)
            }
        }
    }

    private fun stopMonitoring() {
        prefs.isServiceRunning = false
        PingHistoryRepository.setServiceRunning(false)
        PingHistoryRepository.recordInterruption("Stopped")
        pausePingLoop()
        overlayController.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getOrCreateNotification(statusText: String): Notification {
        val builder = notificationBuilder ?: NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notification, getString(R.string.action_refresh), refreshPendingIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.action_stop), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .also { notificationBuilder = it }

        return builder.setContentText(statusText).build()
    }

    private fun throttledUpdateNotification(result: PingResult) {
        val now = System.currentTimeMillis()
        val status = result.statusForThresholds(prefs.greenThresholdMs, prefs.orangeThresholdMs)

        // Only issue notify IPC when latency status changes (e.g. Green -> Red), host changes, or after 30s
        val statusChanged = status != lastNotifiedStatus
        val hostChanged = result.host != lastNotifiedHost
        val periodElapsed = (now - lastNotificationTimeMs) > 30_000L

        if (statusChanged || hostChanged || periodElapsed) {
            lastNotifiedStatus = status
            lastNotifiedHost = result.host
            lastNotificationTimeMs = now

            val text = "${result.host}: ${result.formattedLatency(showMs = prefs.showMsUnit)} (${status.name})"
            notificationManager.notify(NOTIFICATION_ID, getOrCreateNotification(text))
        }
    }

    override fun onDestroy() {
        prefs.isServiceRunning = false
        PingHistoryRepository.setServiceRunning(false)
        pausePingLoop()
        serviceScope.cancel()
        overlayController.hide()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
