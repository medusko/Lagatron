package com.medusko.lagatron.service

import android.annotation.SuppressLint
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.medusko.lagatron.R
import com.medusko.lagatron.data.AppPreferences
import com.medusko.lagatron.model.AppFont
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.model.LatencyStatus
import com.medusko.lagatron.model.PingResult

class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = AppPreferences(context)

    private var overlayView: View? = null
    private var containerView: View? = null
    private var dotIndicator: View? = null
    private var pingTextView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Cached styling state to avoid expensive view invalidations and resource loading on every ping
    private var lastMode: DisplayMode? = null
    private var lastFont: AppFont? = null
    private var lastBold: Boolean? = null
    private var lastItalic: Boolean? = null
    private var lastShadow: Boolean? = null
    private var lastFontSizeSp: Float? = null
    private var lastStatusColor: Int = 0
    private var lastPingText: String = ""

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private fun isLandscape(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private val componentCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            val isLand = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            val view = overlayView ?: return
            val p = layoutParams ?: return
            p.x = prefs.getX(isLand)
            p.y = prefs.getY(isLand)
            try {
                windowManager.updateViewLayout(view, p)
            } catch (_: Exception) {}
        }

        override fun onLowMemory() {}
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    fun show() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_ping, null)
        overlayView = view

        containerView = view.findViewById(R.id.overlayContainer)
        dotIndicator = view.findViewById(R.id.dotIndicator)
        pingTextView = view.findViewById(R.id.pingText)

        val windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val isLand = isLandscape()
            x = prefs.getX(isLand)
            y = prefs.getY(isLand)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        layoutParams = params

        view.setOnTouchListener { _, event ->
            val p = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(view, p)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val isLand = isLandscape()
                    prefs.setCoordinates(isLand, p.x, p.y)
                    prefs.setLastSavedCoordinates(isLand, p.x, p.y)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, params)
            context.registerComponentCallbacks(componentCallbacks)
            applyTypographyAndMode(prefs.displayMode, prefs.appFont, force = true)
        } catch (_: Exception) {
            overlayView = null
        }
    }

    fun applyTypographyAndMode(mode: DisplayMode, font: AppFont, force: Boolean = false) {
        val view = overlayView ?: return
        view.post {
            if (overlayView == null) return@post
            val bold = prefs.isBold
            val italic = prefs.isItalic
            val shadow = prefs.hasTextShadow
            val size = prefs.fontSizeSp

            if (!force &&
                mode == lastMode &&
                font == lastFont &&
                bold == lastBold &&
                italic == lastItalic &&
                shadow == lastShadow &&
                size == lastFontSizeSp
            ) {
                return@post
            }

            lastMode = mode
            lastFont = font
            lastBold = bold
            lastItalic = italic
            lastShadow = shadow
            lastFontSizeSp = size

            // Update mode visibility
            when (mode) {
                DisplayMode.DOT_ONLY -> {
                    dotIndicator?.visibility = View.VISIBLE
                    pingTextView?.visibility = View.GONE
                }
                DisplayMode.MS_ONLY -> {
                    dotIndicator?.visibility = View.GONE
                    pingTextView?.visibility = View.VISIBLE
                }
                DisplayMode.BOTH -> {
                    dotIndicator?.visibility = View.VISIBLE
                    pingTextView?.visibility = View.VISIBLE
                }
                DisplayMode.MONOCHROME_TEXT -> {
                    dotIndicator?.visibility = View.GONE
                    pingTextView?.visibility = View.VISIBLE
                }
            }

            // Container background
            val isMonochrome = (mode == DisplayMode.MONOCHROME_TEXT)
            containerView?.let { container ->
                if (!isMonochrome) {
                    container.background = ContextCompat.getDrawable(context, R.drawable.bg_overlay)
                } else {
                    container.background = null
                }
            }

            // Font & style
            pingTextView?.let { tv ->
                val style = when {
                    bold && italic -> android.graphics.Typeface.BOLD_ITALIC
                    bold -> android.graphics.Typeface.BOLD
                    italic -> android.graphics.Typeface.ITALIC
                    else -> android.graphics.Typeface.NORMAL
                }
                val baseTypeface = font.getTypeface(context)
                tv.setTypeface(baseTypeface, style)
                tv.textSize = size

                if (shadow) {
                    tv.setShadowLayer(2f, -1.5f, 1.5f, Color.argb(220, 0, 0, 0))
                } else {
                    tv.setShadowLayer(0f, 0f, 0f, 0)
                }
            }
        }
    }

    fun updatePing(result: PingResult, mode: DisplayMode, font: AppFont) {
        val view = overlayView ?: return

        view.post {
            if (overlayView == null) return@post
            // Check if typography or mode changed since last time
            if (mode != lastMode || font != lastFont) {
                applyTypographyAndMode(mode, font)
            }

            val isMonochrome = (mode == DisplayMode.MONOCHROME_TEXT)
            val currentStatus = result.statusForThresholds(
                greenThresholdMs = prefs.greenThresholdMs,
                orangeThresholdMs = prefs.orangeThresholdMs
            )

            val statusColorInt = if (!isMonochrome) {
                when (currentStatus) {
                    LatencyStatus.GREEN -> 0xFF00E676.toInt()
                    LatencyStatus.ORANGE -> 0xFFFF9100.toInt()
                    LatencyStatus.RED -> 0xFFFF1744.toInt()
                }
            } else {
                0xFFFFFFFF.toInt()
            }

            // Update dot indicator color only if color changed
            if (statusColorInt != lastStatusColor) {
                dotIndicator?.let { dot ->
                    val background = dot.background
                    if (background is GradientDrawable) {
                        background.setColor(statusColorInt)
                    }
                }
            }

            val formatted = result.formattedLatency(showMs = prefs.showMsUnit)
            pingTextView?.let { tv ->
                if (formatted != lastPingText) {
                    tv.text = formatted
                    lastPingText = formatted
                }
                if (statusColorInt != lastStatusColor) {
                    tv.setTextColor(statusColorInt)
                }
            }

            lastStatusColor = statusColorInt
        }
    }

    fun setPosition(x: Int, y: Int) {
        val view = overlayView ?: return
        val p = layoutParams ?: return
        p.x = x
        p.y = y
        try {
            windowManager.updateViewLayout(view, p)
            prefs.setCoordinates(isLandscape(), x, y)
        } catch (_: Exception) {}
    }

    fun hide() {
        try {
            context.unregisterComponentCallbacks(componentCallbacks)
        } catch (_: Exception) {}
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
            overlayView = null
        }
        lastMode = null
        lastFont = null
        lastBold = null
        lastItalic = null
        lastShadow = null
        lastFontSizeSp = null
        lastStatusColor = 0
        lastPingText = ""
    }
}
