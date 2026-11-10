package com.medusko.lagatron.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PositionAdjuster(
    portraitX: Int,
    portraitY: Int,
    landscapeX: Int,
    landscapeY: Int,
    isDeviceLandscape: Boolean,
    onPositionChanged: (isLandscape: Boolean, newX: Int, newY: Int) -> Unit,
    onSwitchOrientation: (toLandscape: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLandscapeTab by remember(isDeviceLandscape) { mutableStateOf(isDeviceLandscape) }

    val currentX = if (isLandscapeTab) landscapeX else portraitX
    val currentY = if (isLandscapeTab) landscapeY else portraitY

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density

    // Screen dimensions in pixels
    val screenWidthPx = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
    val screenHeightPx = minOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
    val portraitWidthPx = minOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
    val portraitHeightPx = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels)

    // Landscape preset coordinates (accurately spanning the landscape display)
    val landscapeTopRightX = (screenWidthPx - (95 * density).toInt()).coerceAtLeast(0)
    val landscapeTopCenterX = ((screenWidthPx - (80 * density).toInt()) / 2).coerceAtLeast(0)
    val landscapeTopLeftX = (36 * density).toInt()
    val landscapeTopY = 30

    // Portrait preset coordinates
    val portraitRightX = (portraitWidthPx - (90 * density).toInt()).coerceAtLeast(0)
    val portraitCenterX = ((portraitWidthPx - (70 * density).toInt()) / 2).coerceAtLeast(0)
    val portraitLeftX = (28 * density).toInt()
    val portraitTopY = 30

    var xInput by remember(currentX) { mutableStateOf(currentX.toString()) }
    var yInput by remember(currentY) { mutableStateOf(currentY.toString()) }

    LaunchedEffect(currentX) {
        if (xInput.toIntOrNull() != currentX) {
            xInput = currentX.toString()
        }
    }

    LaunchedEffect(currentY) {
        if (yInput.toIntOrNull() != currentY) {
            yInput = currentY.toString()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overlay Position & Orientation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Set independent positions for vertical (portrait) and horizontal (landscape)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Orientation Tabs
            TabRow(
                selectedTabIndex = if (isLandscapeTab) 1 else 0,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (isLandscapeTab) 1 else 0]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !isLandscapeTab,
                    onClick = {
                        isLandscapeTab = false
                        onSwitchOrientation(false)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.StayCurrentPortrait, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Portrait")
                        }
                    }
                )
                Tab(
                    selected = isLandscapeTab,
                    onClick = {
                        isLandscapeTab = true
                        onSwitchOrientation(true)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.StayCurrentLandscape, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Landscape")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Preset Placement Buttons for Active Orientation (Stretching evenly across the row)
            Text(
                text = if (isLandscapeTab) "Landscape Presets" else "Portrait Presets",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLandscapeTab) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onPositionChanged(true, landscapeTopRightX, landscapeTopY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Landscape Top Right",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPositionChanged(true, landscapeTopCenterX, landscapeTopY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Top Center",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPositionChanged(true, landscapeTopLeftX, landscapeTopY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Landscape Top Left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onPositionChanged(false, portraitRightX, portraitTopY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Status Bar Right",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPositionChanged(false, portraitCenterX, 120) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Top Center",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPositionChanged(false, portraitLeftX, portraitTopY) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Status Bar Left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        if (isLandscapeTab) {
                            onPositionChanged(true, 200, 200)
                        } else {
                            onPositionChanged(false, 100, 300)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLandscapeTab) "Reset (200, 200)" else "Reset (100, 300)",
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balanced 2-Column Layout: Arrows D-Pad (Left) and Editable Coordinates Card (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1: D-Pad Nudge Controls (5px steps with haptic hold-to-repeat)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RepeatingNudgeButton(
                        onClick = { onPositionChanged(isLandscapeTab, currentX, (currentY - 5).coerceAtLeast(0)) },
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RepeatingNudgeButton(
                            onClick = { onPositionChanged(isLandscapeTab, (currentX - 5).coerceAtLeast(0), currentY) },
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Move Left"
                        )

                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        RepeatingNudgeButton(
                            onClick = { onPositionChanged(isLandscapeTab, currentX + 5, currentY) },
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Move Right"
                        )
                    }

                    RepeatingNudgeButton(
                        onClick = { onPositionChanged(isLandscapeTab, currentX, currentY + 5) },
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down"
                    )
                }

                // Column 2: Coordinate Display & Editable Input Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLandscapeTab) "Landscape Coordinates" else "Portrait Coordinates",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editable coordinates",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "X",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        BasicTextField(
                                            value = xInput,
                                            onValueChange = { raw ->
                                                val filtered = raw.filter { it.isDigit() }.take(5)
                                                xInput = filtered
                                                filtered.toIntOrNull()?.let { validX ->
                                                    onPositionChanged(isLandscapeTab, validX, currentY)
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    if (xInput.isBlank()) {
                                                        xInput = currentX.toString()
                                                    }
                                                }
                                            ),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Y",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        BasicTextField(
                                            value = yInput,
                                            onValueChange = { raw ->
                                                val filtered = raw.filter { it.isDigit() }.take(5)
                                                yInput = filtered
                                                filtered.toIntOrNull()?.let { validY ->
                                                    onPositionChanged(isLandscapeTab, currentX, validY)
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    if (yInput.isBlank()) {
                                                        yInput = currentY.toString()
                                                    }
                                                }
                                            ),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (isLandscapeTab) {
                                    onPositionChanged(true, landscapeTopRightX, landscapeTopY)
                                } else {
                                    onPositionChanged(false, portraitRightX, portraitTopY)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Default",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatingNudgeButton(
    onClick: () -> Unit,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .size(42.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnClick()
                        val job = scope.launch {
                            delay(350L)
                            while (isActive) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnClick()
                                delay(65L)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
