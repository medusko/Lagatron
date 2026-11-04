package com.medusko.lagatron.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medusko.lagatron.R
import com.medusko.lagatron.data.PingPoint
import com.medusko.lagatron.model.AppFont
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.model.PingResult
import com.medusko.lagatron.model.PresetServer
import com.medusko.lagatron.ui.components.DisplayModeSelector
import com.medusko.lagatron.ui.components.OverlayPreview
import com.medusko.lagatron.ui.components.PingGraphCard
import com.medusko.lagatron.ui.components.PositionAdjuster
import com.medusko.lagatron.ui.components.TargetSelector
import com.medusko.lagatron.ui.theme.PingGreen
import com.medusko.lagatron.ui.theme.PingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isRunning: Boolean,
    isDarkMode: Boolean,
    selectedPreset: PresetServer,
    customHost: String,
    displayMode: DisplayMode,
    selectedFont: AppFont,
    isBold: Boolean,
    isItalic: Boolean,
    hasShadow: Boolean,
    showMsUnit: Boolean,
    fontSizeSp: Float,
    greenThresholdMs: Int,
    orangeThresholdMs: Int,
    startOnBoot: Boolean,
    portraitX: Int,
    portraitY: Int,
    landscapeX: Int,
    landscapeY: Int,
    isDeviceLandscape: Boolean,
    activeHost: String,
    pingHistory: List<PingPoint>,
    latestPing: PingResult?,
    pingStats: com.medusko.lagatron.data.PingStats = com.medusko.lagatron.data.PingStats(),
    onToggleTheme: () -> Unit,
    onPresetSelected: (PresetServer) -> Unit,
    onCustomHostChanged: (String) -> Unit,
    onModeSelected: (DisplayMode) -> Unit,
    onFontSelected: (AppFont) -> Unit,
    onBoldToggled: (Boolean) -> Unit,
    onItalicToggled: (Boolean) -> Unit,
    onShadowToggled: (Boolean) -> Unit,
    onShowMsToggled: (Boolean) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onThresholdsChanged: (greenMs: Int, orangeMs: Int) -> Unit,
    onStartOnBootToggled: (Boolean) -> Unit,
    onPositionChanged: (isLandscape: Boolean, newX: Int, newY: Int) -> Unit,
    onSwitchOrientation: (toLandscape: Boolean) -> Unit,
    onToggleService: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutLagatronDialog(
            isRunning = isRunning,
            activeHost = activeHost,
            onDismiss = { showAboutDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Lagatron",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isRunning) PingGreen.copy(alpha = 0.15f) else PingRed.copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (isRunning) PingGreen else PingRed
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) PingGreen else PingRed)
                                )
                                Text(
                                    text = if (isRunning) "ACTIVE" else "STOPPED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) PingGreen else PingRed
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Lagatron",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) PingRed else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Stop Overlay" else "Start Overlay ($activeHost)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TargetSelector(
                    selectedPreset = selectedPreset,
                    customHost = customHost,
                    onPresetSelected = onPresetSelected,
                    onCustomHostChanged = onCustomHostChanged
                )
            }

            // Real-time Ping Latency Graph (centered stats, RYG line & gradient)
            item {
                PingGraphCard(
                    history = pingHistory,
                    latestPing = latestPing,
                    isRunning = isRunning,
                    showMsUnit = showMsUnit,
                    greenThresholdMs = greenThresholdMs,
                    orangeThresholdMs = orangeThresholdMs,
                    stats = pingStats
                )
            }

            item {
                DisplayModeSelector(
                    selectedMode = displayMode,
                    selectedFont = selectedFont,
                    isBold = isBold,
                    isItalic = isItalic,
                    hasShadow = hasShadow,
                    showMsUnit = showMsUnit,
                    fontSizeSp = fontSizeSp,
                    greenThresholdMs = greenThresholdMs,
                    orangeThresholdMs = orangeThresholdMs,
                    onModeSelected = onModeSelected,
                    onFontSelected = onFontSelected,
                    onBoldToggled = onBoldToggled,
                    onItalicToggled = onItalicToggled,
                    onShadowToggled = onShadowToggled,
                    onShowMsToggled = onShowMsToggled,
                    onFontSizeChanged = onFontSizeChanged,
                    onThresholdsChanged = onThresholdsChanged
                )
            }

            item {
                OverlayPreview(
                    displayMode = displayMode,
                    font = selectedFont,
                    isBold = isBold,
                    isItalic = isItalic,
                    hasShadow = hasShadow,
                    showMsUnit = showMsUnit,
                    fontSizeSp = fontSizeSp,
                    greenThresholdMs = greenThresholdMs,
                    orangeThresholdMs = orangeThresholdMs
                )
            }

            item {
                PositionAdjuster(
                    portraitX = portraitX,
                    portraitY = portraitY,
                    landscapeX = landscapeX,
                    landscapeY = landscapeY,
                    isDeviceLandscape = isDeviceLandscape,
                    onPositionChanged = onPositionChanged,
                    onSwitchOrientation = onSwitchOrientation
                )
            }

            // Start on Boot Setting Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start on Boot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically resume floating overlay when your phone boots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = startOnBoot,
                            onCheckedChange = onStartOnBootToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Orientation & Status Bar Memory",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lagatron remembers independent screen coordinates for portrait and landscape modes. As you rotate your device, the overlay automatically snaps to your configured position for that orientation.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutLagatronDialog(
    isRunning: Boolean,
    activeHost: String,
    pingIntervalMs: Long = 1000L,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Top Header Row: Icon + Title + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "About Lagatron",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. HERO IDENTITY CARD
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Lagatron",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Android",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Subprocess ICMP & TCP Network Latency HUD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "v1.1-beta",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "Build 2",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            )

                            // Quick Diagnostic Status Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isRunning) PingGreen else PingRed)
                                    )
                                    Text(
                                        text = if (isRunning) "Engine Active ($activeHost)" else "Engine Stopped",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRunning) PingGreen else PingRed
                                    )
                                }

                                Text(
                                    text = "Interval: ${pingIntervalMs / 1000f}s",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Subprocess ICMP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. ARCHITECTURE & TELEMETRY ENGINE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ZERO-OVERHEAD TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Lagatron is an ultra-lightweight mobile telemetry HUD engineered to provide continuous, real-time round-trip latency visibility for network engineers, gamers, and mobile power users.\n\nRuns as an unobtrusive Android foreground service with a persistent floating HUD that stays visible above full-screen games, streaming video, and benchmarks.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp
                        )
                    }

                    // Feature highlights container
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutFeatureItem(
                                title = "Native Subprocess Streaming",
                                description = "Executes /system/bin/ping asynchronously with low-overhead line-buffered parsing and sub-millisecond precision."
                            )
                            AboutFeatureItem(
                                title = "Always-on-Top Floating HUD",
                                description = "SYSTEM_ALERT_WINDOW overlay with real-time X/Y dragging and separate orientation coordinates for portrait & landscape."
                            )
                            AboutFeatureItem(
                                title = "Adaptive Latency Thresholds",
                                description = "Dynamic color-coding (<50ms optimal green, 50-100ms moderate amber, >100ms degraded red) to surface network jitter immediately."
                            )
                            AboutFeatureItem(
                                title = "Custom Typography & Display Modes",
                                description = "4 distinct telemetry display modes (RTT, Jitter, Both, Minimal) paired with customizable Poppins fonts and shadows."
                            )
                            AboutFeatureItem(
                                title = "Zero-Waste Power Management",
                                description = "Optimized background coroutine execution with automated CPU wake-lock conservation and minimal battery consumption."
                            )
                        }
                    }

                    // 3. RELEASE NOTES & WHAT'S NEW
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "RELEASE NOTES & WHAT'S NEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "• Subversion v1.1-beta Engine Tuning",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Cross-platform information parity with Keyboardy",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Sub-millisecond ping regex parsing with reduced memory allocations",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Draggable floating overlay coordinate persistence across orientation switches",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Automated multi-platform GitHub Releases with SHA-256 checksums",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 4. LINKS & RESOURCES
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "LINKS & RESOURCES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AboutResourceLink(
                                text = "Full Changelog ↗",
                                modifier = Modifier.weight(1f)
                            ) {
                                uriHandler.openUri("https://github.com/medusko/Lagatron/blob/main/CHANGELOG.md")
                            }
                            AboutResourceLink(
                                text = "All Releases ↗",
                                modifier = Modifier.weight(1f)
                            ) {
                                uriHandler.openUri("https://github.com/medusko/Lagatron/releases")
                            }
                            AboutResourceLink(
                                text = "Source Code ↗",
                                modifier = Modifier.weight(1f)
                            ) {
                                uriHandler.openUri("https://github.com/medusko/Lagatron")
                            }
                        }
                    }

                    // 5. FOOTER
                    Text(
                        text = "Lagatron v1.1-beta (Build 2) • Built for Android 15 / Pixel • Designed & Engineered by Medusko",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "• $title",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.5.sp
        )
    }
}

@Composable
private fun AboutResourceLink(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}
