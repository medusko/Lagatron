package com.medusko.lagatron.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.medusko.lagatron.MainActivity
import com.medusko.lagatron.R
import com.medusko.lagatron.data.AppPreferences
import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "lagatron_monitor_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.medusko.lagatron.ACTION_START"
        const val ACTION_STOP = "com.medusko.lagatron.ACTION_STOP"
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

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        overlayController = OverlayController(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
            }
            ACTION_UPDATE_POSITION -> {
                val x = intent.getIntExtra("x", prefs.overlayX)
                val y = intent.getIntExtra("y", prefs.overlayY)
                overlayController.setPosition(x, y)
            }
            ACTION_UPDATE_CONFIG -> {
                lastResult?.let {
                    overlayController.updatePing(it, prefs.displayMode, prefs.appFont)
                }
            }
            else -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        prefs.isServiceRunning = true
        com.medusko.lagatron.data.PingHistoryRepository.recordInterruption("Started")
        val initialNotification = buildNotification("Starting ping monitor...")

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

    private fun startPingLoop() {
        pingJob?.cancel()
        var currentHost = prefs.getActiveHost()
        pingJob = serviceScope.launch {
            while (isActive) {
                val host = prefs.getActiveHost()
                if (host != currentHost) {
                    com.medusko.lagatron.data.PingHistoryRepository.recordInterruption("Target: $host")
                    currentHost = host
                }

                val result = PingEngine.ping(host)
                lastResult = result

                com.medusko.lagatron.data.PingHistoryRepository.recordPing(result)
                overlayController.updatePing(result, prefs.displayMode, prefs.appFont)
                updateNotification(result)

                delay(prefs.intervalMs)
            }
        }
    }

    private fun stopMonitoring() {
        prefs.isServiceRunning = false
        com.medusko.lagatron.data.PingHistoryRepository.recordInterruption("Stopped")
        pingJob?.cancel()
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

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notification, getString(R.string.action_stop), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(result: PingResult) {
        val text = "${result.host}: ${result.formattedLatency()} (${result.status.name})"
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        prefs.isServiceRunning = false
        pingJob?.cancel()
        serviceScope.cancel()
        overlayController.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
