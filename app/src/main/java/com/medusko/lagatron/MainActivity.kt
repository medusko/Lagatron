package com.medusko.lagatron

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.medusko.lagatron.data.AppPreferences
import com.medusko.lagatron.data.PingHistoryRepository
import com.medusko.lagatron.model.AppFont
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.model.PresetServer
import com.medusko.lagatron.service.PingForegroundService
import com.medusko.lagatron.ui.MainScreen
import com.medusko.lagatron.ui.theme.LagatronTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences

    private var isRunning by mutableStateOf(false)
    private var isDarkMode by mutableStateOf(true)
    private var selectedPreset by mutableStateOf(PresetServer.GOOGLE)
    private var customHost by mutableStateOf("")
    private var displayMode by mutableStateOf(DisplayMode.BOTH)
    private var selectedFont by mutableStateOf(AppFont.MONOSPACE)
    private var isBold by mutableStateOf(true)
    private var isItalic by mutableStateOf(false)
    private var hasShadow by mutableStateOf(false)
    private var showMsUnit by mutableStateOf(true)
    private var fontSizeSp by mutableStateOf(11.5f)
    private var greenThresholdMs by mutableStateOf(10)
    private var orangeThresholdMs by mutableStateOf(100)
    private var startOnBoot by mutableStateOf(false)
    private var intervalMs by mutableStateOf(1000L)
    private var portraitX by mutableStateOf(700)
    private var portraitY by mutableStateOf(30)
    private var landscapeX by mutableStateOf(1400)
    private var landscapeY by mutableStateOf(30)
    private var isDeviceLandscape by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayAndStart()
        } else {
            Toast.makeText(this, "Notification permission is needed for the persistent monitor", Toast.LENGTH_LONG).show()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        } else {
            Toast.makeText(this, "Overlay permission is required to display over other apps", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        loadPreferences()
        updateOrientationState()

        // Keep status bar dark with high contrast so white overlay text is always crystal clear
        window.statusBarColor = android.graphics.Color.parseColor("#15181E")
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val pingHistory by PingHistoryRepository.history.collectAsState()
            val latestPing by PingHistoryRepository.latestPing.collectAsState()
            val pingStats by PingHistoryRepository.stats.collectAsState()

            LagatronTheme(darkTheme = isDarkMode) {
                MainScreen(
                    isRunning = isRunning,
                    isDarkMode = isDarkMode,
                    selectedPreset = selectedPreset,
                    customHost = customHost,
                    displayMode = displayMode,
                    selectedFont = selectedFont,
                    isBold = isBold,
                    isItalic = isItalic,
                    hasShadow = hasShadow,
                    showMsUnit = showMsUnit,
                    fontSizeSp = fontSizeSp,
                    greenThresholdMs = greenThresholdMs,
                    orangeThresholdMs = orangeThresholdMs,
                    startOnBoot = startOnBoot,
                    portraitX = portraitX,
                    portraitY = portraitY,
                    landscapeX = landscapeX,
                    landscapeY = landscapeY,
                    isDeviceLandscape = isDeviceLandscape,
                    activeHost = prefs.getActiveHost(),
                    pingHistory = pingHistory,
                    latestPing = latestPing,
                    pingStats = pingStats,
                    onToggleTheme = {
                        isDarkMode = !isDarkMode
                        prefs.themeMode = if (isDarkMode) "DARK" else "LIGHT"
                    },
                    onPresetSelected = { preset ->
                        selectedPreset = preset
                        prefs.selectedPreset = preset
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onCustomHostChanged = { host ->
                        customHost = host
                        prefs.customHost = host
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onModeSelected = { mode ->
                        displayMode = mode
                        prefs.displayMode = mode
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onFontSelected = { font ->
                        selectedFont = font
                        prefs.appFont = font
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onBoldToggled = { bold ->
                        isBold = bold
                        prefs.isBold = bold
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onItalicToggled = { italic ->
                        isItalic = italic
                        prefs.isItalic = italic
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onShadowToggled = { shadow ->
                        hasShadow = shadow
                        prefs.hasTextShadow = shadow
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onShowMsToggled = { showMs ->
                        showMsUnit = showMs
                        prefs.showMsUnit = showMs
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onFontSizeChanged = { size ->
                        fontSizeSp = size
                        prefs.fontSizeSp = size
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onThresholdsChanged = { greenMs, orangeMs ->
                        greenThresholdMs = greenMs
                        orangeThresholdMs = orangeMs
                        prefs.greenThresholdMs = greenMs
                        prefs.orangeThresholdMs = orangeMs
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onStartOnBootToggled = { boot ->
                        startOnBoot = boot
                        prefs.startOnBoot = boot
                    },
                    onPositionChanged = { isLandscape, newX, newY ->
                        if (isLandscape) {
                            landscapeX = newX
                            landscapeY = newY
                        } else {
                            portraitX = newX
                            portraitY = newY
                        }
                        prefs.setCoordinates(isLandscape, newX, newY)

                        val currentIsLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        if (isRunning && currentIsLandscape == isLandscape) {
                            PingForegroundService.updatePosition(this, newX, newY)
                        }
                    },
                    onSwitchOrientation = { toLandscape ->
                        requestedOrientation = if (toLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        isDeviceLandscape = toLandscape
                    },
                    intervalMs = intervalMs,
                    onIntervalChanged = { ms ->
                        intervalMs = ms
                        prefs.intervalMs = ms
                        if (isRunning) {
                            PingForegroundService.updateConfig(this)
                        }
                    },
                    onToggleService = {
                        if (isRunning) {
                            stopOverlayService()
                        } else {
                            requestPermissionsAndStart()
                        }
                    }
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientationState()
    }

    override fun onResume() {
        super.onResume()
        isRunning = prefs.isServiceRunning
        portraitX = prefs.overlayX
        portraitY = prefs.overlayY
        landscapeX = prefs.landscapeX
        landscapeY = prefs.landscapeY
        updateOrientationState()
    }

    private fun updateOrientationState() {
        isDeviceLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun loadPreferences() {
        selectedPreset = prefs.selectedPreset
        customHost = prefs.customHost
        displayMode = prefs.displayMode
        selectedFont = prefs.appFont
        isBold = prefs.isBold
        isItalic = prefs.isItalic
        hasShadow = prefs.hasTextShadow
        showMsUnit = prefs.showMsUnit
        fontSizeSp = prefs.fontSizeSp
        greenThresholdMs = prefs.greenThresholdMs
        orangeThresholdMs = prefs.orangeThresholdMs
        startOnBoot = prefs.startOnBoot
        intervalMs = prefs.intervalMs
        isRunning = prefs.isServiceRunning
        portraitX = prefs.overlayX
        portraitY = prefs.overlayY
        landscapeX = prefs.landscapeX
        landscapeY = prefs.landscapeY
        isDarkMode = prefs.themeMode != "LIGHT"
    }

    private fun requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        checkOverlayAndStart()
    }

    private fun checkOverlayAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Please enable 'Allow display over other apps' for Lagatron",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        startOverlayService()
    }

    private fun startOverlayService() {
        PingForegroundService.start(this)
        isRunning = true
        prefs.isServiceRunning = true
    }

    private fun stopOverlayService() {
        PingForegroundService.stop(this)
        isRunning = false
        prefs.isServiceRunning = false
    }
}
