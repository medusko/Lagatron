package com.medusko.lagatron.data

import android.content.Context
import android.content.SharedPreferences
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.model.PresetServer

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lagatron_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRESET_SERVER = "preset_server"
        private const val KEY_CUSTOM_HOST = "custom_host"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_OVERLAY_LANDSCAPE_X = "overlay_landscape_x"
        private const val KEY_OVERLAY_LANDSCAPE_Y = "overlay_landscape_y"
        private const val KEY_LAST_SAVED_PORTRAIT_X = "last_saved_portrait_x"
        private const val KEY_LAST_SAVED_PORTRAIT_Y = "last_saved_portrait_y"
        private const val KEY_LAST_SAVED_LANDSCAPE_X = "last_saved_landscape_x"
        private const val KEY_LAST_SAVED_LANDSCAPE_Y = "last_saved_landscape_y"
        private const val KEY_INTERVAL_MS = "interval_ms"
        private const val KEY_SERVICE_ACTIVE = "service_active"
        private const val KEY_USE_COLOR_CODING = "use_color_coding"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_FONT = "app_font"
        private const val KEY_FONT_BOLD = "font_bold"
        private const val KEY_FONT_ITALIC = "font_italic"
        private const val KEY_FONT_SIZE_SP = "font_size_sp"
        private const val KEY_TEXT_SHADOW = "text_shadow"
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_SHOW_MS_UNIT = "show_ms_unit"
        private const val KEY_GREEN_THRESHOLD_MS = "green_threshold_ms"
        private const val KEY_ORANGE_THRESHOLD_MS = "orange_threshold_ms"
        private val SAFE_HOST_REGEX = Regex("^[a-zA-Z0-9]([a-zA-Z0-9.-]{0,251}[a-zA-Z0-9])?$")
        private val SAFE_IPV6_REGEX = Regex("^[0-9a-fA-F:]{2,45}$")
    }

    var showMsUnit: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MS_UNIT, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MS_UNIT, value).apply()

    var greenThresholdMs: Int
        get() = prefs.getInt(KEY_GREEN_THRESHOLD_MS, 10)
        set(value) = prefs.edit().putInt(KEY_GREEN_THRESHOLD_MS, value.coerceIn(1, 500)).apply()

    var orangeThresholdMs: Int
        get() = prefs.getInt(KEY_ORANGE_THRESHOLD_MS, 100)
        set(value) = prefs.edit().putInt(KEY_ORANGE_THRESHOLD_MS, value.coerceIn(2, 1000)).apply()

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    var hasTextShadow: Boolean
        get() = prefs.getBoolean(KEY_TEXT_SHADOW, false)
        set(value) = prefs.edit().putBoolean(KEY_TEXT_SHADOW, value).apply()

    var isBold: Boolean
        get() = prefs.getBoolean(KEY_FONT_BOLD, true)
        set(value) = prefs.edit().putBoolean(KEY_FONT_BOLD, value).apply()

    var isItalic: Boolean
        get() = prefs.getBoolean(KEY_FONT_ITALIC, false)
        set(value) = prefs.edit().putBoolean(KEY_FONT_ITALIC, value).apply()

    var fontSizeSp: Float
        get() = prefs.getFloat(KEY_FONT_SIZE_SP, 11.5f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE_SP, value).apply()

    var appFont: com.medusko.lagatron.model.AppFont
        get() {
            val name = prefs.getString(KEY_APP_FONT, com.medusko.lagatron.model.AppFont.MONOSPACE.name)
            return try {
                com.medusko.lagatron.model.AppFont.valueOf(name ?: com.medusko.lagatron.model.AppFont.MONOSPACE.name)
            } catch (_: Exception) {
                com.medusko.lagatron.model.AppFont.MONOSPACE
            }
        }
        set(value) = prefs.edit().putString(KEY_APP_FONT, value.name).apply()

    val isColorCoded: Boolean
        get() = displayMode != DisplayMode.MONOCHROME_TEXT

    var useColorCoding: Boolean
        get() = displayMode != DisplayMode.MONOCHROME_TEXT
        set(value) {
            if (!value) {
                displayMode = DisplayMode.MONOCHROME_TEXT
            } else if (displayMode == DisplayMode.MONOCHROME_TEXT) {
                displayMode = DisplayMode.BOTH
            }
            prefs.edit().putBoolean(KEY_USE_COLOR_CODING, value).apply()
        }

    var themeMode: com.medusko.lagatron.model.ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, com.medusko.lagatron.model.ThemeMode.SYSTEM.name)
            return try {
                com.medusko.lagatron.model.ThemeMode.valueOf(name ?: com.medusko.lagatron.model.ThemeMode.SYSTEM.name)
            } catch (_: Exception) {
                com.medusko.lagatron.model.ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var selectedPreset: PresetServer
        get() {
            val name = prefs.getString(KEY_PRESET_SERVER, PresetServer.GOOGLE.name)
            return try {
                PresetServer.valueOf(name ?: PresetServer.GOOGLE.name)
            } catch (e: Exception) {
                PresetServer.GOOGLE
            }
        }
        set(value) = prefs.edit().putString(KEY_PRESET_SERVER, value.name).apply()

    var customHost: String
        get() = prefs.getString(KEY_CUSTOM_HOST, "1.1.1.1").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_HOST, value).apply()

    fun getActiveHost(): String {
        return if (selectedPreset == PresetServer.CUSTOM) {
            cleanHostInput(customHost)
        } else {
            selectedPreset.host
        }
    }

    var displayMode: DisplayMode
        get() {
            val name = prefs.getString(KEY_DISPLAY_MODE, DisplayMode.BOTH.name)
            return try {
                DisplayMode.valueOf(name ?: DisplayMode.BOTH.name)
            } catch (e: Exception) {
                DisplayMode.BOTH
            }
        }
        set(value) = prefs.edit().putString(KEY_DISPLAY_MODE, value.name).apply()

    var overlayX: Int
        get() = prefs.getInt(KEY_OVERLAY_X, 700)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_X, value).apply()

    var overlayY: Int
        get() = prefs.getInt(KEY_OVERLAY_Y, 30)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_Y, value).apply()

    var landscapeX: Int
        get() = prefs.getInt(KEY_OVERLAY_LANDSCAPE_X, 2150)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_LANDSCAPE_X, value).apply()

    var landscapeY: Int
        get() = prefs.getInt(KEY_OVERLAY_LANDSCAPE_Y, 30)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_LANDSCAPE_Y, value).apply()

    fun getX(isLandscape: Boolean): Int = if (isLandscape) landscapeX else overlayX
    fun getY(isLandscape: Boolean): Int = if (isLandscape) landscapeY else overlayY

    var lastSavedPortraitX: Int
        get() = prefs.getInt(KEY_LAST_SAVED_PORTRAIT_X, overlayX)
        set(value) = prefs.edit().putInt(KEY_LAST_SAVED_PORTRAIT_X, value).apply()

    var lastSavedPortraitY: Int
        get() = prefs.getInt(KEY_LAST_SAVED_PORTRAIT_Y, overlayY)
        set(value) = prefs.edit().putInt(KEY_LAST_SAVED_PORTRAIT_Y, value).apply()

    var lastSavedLandscapeX: Int
        get() = prefs.getInt(KEY_LAST_SAVED_LANDSCAPE_X, landscapeX)
        set(value) = prefs.edit().putInt(KEY_LAST_SAVED_LANDSCAPE_X, value).apply()

    var lastSavedLandscapeY: Int
        get() = prefs.getInt(KEY_LAST_SAVED_LANDSCAPE_Y, landscapeY)
        set(value) = prefs.edit().putInt(KEY_LAST_SAVED_LANDSCAPE_Y, value).apply()

    fun setLastSavedCoordinates(isLandscape: Boolean, x: Int, y: Int) {
        if (isLandscape) {
            prefs.edit()
                .putInt(KEY_LAST_SAVED_LANDSCAPE_X, x)
                .putInt(KEY_LAST_SAVED_LANDSCAPE_Y, y)
                .apply()
        } else {
            prefs.edit()
                .putInt(KEY_LAST_SAVED_PORTRAIT_X, x)
                .putInt(KEY_LAST_SAVED_PORTRAIT_Y, y)
                .apply()
        }
    }

    fun setCoordinates(isLandscape: Boolean, x: Int, y: Int) {
        if (isLandscape) {
            prefs.edit()
                .putInt(KEY_OVERLAY_LANDSCAPE_X, x)
                .putInt(KEY_OVERLAY_LANDSCAPE_Y, y)
                .apply()
        } else {
            prefs.edit()
                .putInt(KEY_OVERLAY_X, x)
                .putInt(KEY_OVERLAY_Y, y)
                .apply()
        }
    }

    var intervalMs: Long
        get() = prefs.getLong(KEY_INTERVAL_MS, 1000L)
        set(value) = prefs.edit().putLong(KEY_INTERVAL_MS, value).apply()

    var isServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, value).apply()

    private fun cleanHostInput(input: String): String {
        var cleaned = input.trim()
        if (cleaned.startsWith("http://", ignoreCase = true)) {
            cleaned = cleaned.substring(7)
        } else if (cleaned.startsWith("https://", ignoreCase = true)) {
            cleaned = cleaned.substring(8)
        }
        val slashIndex = cleaned.indexOf('/')
        if (slashIndex != -1) {
            cleaned = cleaned.substring(0, slashIndex)
        }
        cleaned = cleaned.trim()

        // Handle bracketed IPv6 e.g. [2606:4700:4700::1111]:443
        if (cleaned.startsWith("[")) {
            val closeBracket = cleaned.indexOf(']')
            if (closeBracket > 1) {
                cleaned = cleaned.substring(1, closeBracket)
            }
        } else {
            // Only strip :port if there is a single colon (IPv4 or hostname with port)
            val firstColon = cleaned.indexOf(':')
            val lastColon = cleaned.lastIndexOf(':')
            if (firstColon != -1 && firstColon == lastColon) {
                cleaned = cleaned.substring(0, firstColon)
            }
        }

        cleaned = cleaned.trim()
        if (cleaned.isEmpty() || cleaned.startsWith("-") || cleaned.length > 253) {
            return "8.8.8.8"
        }

        val isValid = if (cleaned.contains(':')) {
            SAFE_IPV6_REGEX.matches(cleaned)
        } else {
            SAFE_HOST_REGEX.matches(cleaned) && !cleaned.contains("..")
        }
        return if (isValid) cleaned else "8.8.8.8"
    }
}
