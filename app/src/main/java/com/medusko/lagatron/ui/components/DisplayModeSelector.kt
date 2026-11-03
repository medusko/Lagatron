package com.medusko.lagatron.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medusko.lagatron.model.AppFont
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.ui.theme.PingGreen
import com.medusko.lagatron.ui.theme.PingOrange
import com.medusko.lagatron.ui.theme.PingRed
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplayModeSelector(
    selectedMode: DisplayMode,
    selectedFont: AppFont,
    isBold: Boolean,
    isItalic: Boolean,
    hasShadow: Boolean,
    showMsUnit: Boolean,
    fontSizeSp: Float,
    greenThresholdMs: Int,
    orangeThresholdMs: Int,
    onModeSelected: (DisplayMode) -> Unit,
    onFontSelected: (AppFont) -> Unit,
    onBoldToggled: (Boolean) -> Unit,
    onItalicToggled: (Boolean) -> Unit,
    onShadowToggled: (Boolean) -> Unit,
    onShowMsToggled: (Boolean) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onThresholdsChanged: (greenMs: Int, orangeMs: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Display Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose your preferred overlay presentation style",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // 4 Display Style Options
            DisplayMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onModeSelected(mode) },
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onModeSelected(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Font Selector
            Text(
                text = "Overlay Typography & Styling",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Typeface, weight, unit suffix, and size for overlay readings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFont.entries.forEach { font ->
                    val isSelected = font == selectedFont
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFontSelected(font) },
                        label = {
                            Text(
                                text = font.title,
                                fontFamily = font.getComposeFont(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bold, Italics, Drop Shadow & Show "ms" Unit Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isBold,
                    onClick = { onBoldToggled(!isBold) },
                    leadingIcon = {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.padding(end = 4.dp))
                    },
                    label = { Text("Bold", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = isItalic,
                    onClick = { onItalicToggled(!isItalic) },
                    leadingIcon = {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.padding(end = 4.dp))
                    },
                    label = { Text("Italics", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = hasShadow,
                    onClick = { onShadowToggled(!hasShadow) },
                    label = { Text("Shadow (1px)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = showMsUnit,
                    onClick = { onShowMsToggled(!showMsUnit) },
                    leadingIcon = if (showMsUnit) {
                        {
                            Icon(Icons.Default.Check, contentDescription = "Show ms", modifier = Modifier.padding(end = 4.dp))
                        }
                    } else null,
                    label = { Text("Show \"ms\"") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Size Scaler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "%.1f sp".format(fontSizeSp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Slider(
                value = fontSizeSp,
                onValueChange = onFontSizeChanged,
                valueRange = 9f..18f,
                steps = 17,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // G / Y / R Latency Thresholds Section
            Text(
                text = "Latency Color Thresholds (G / Y / R)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Customize millisecond cutoffs for Green, Yellow/Orange, and Red status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            // Quick Threshold Presets
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { onThresholdsChanged(10, 100) },
                    label = { Text("Default (10 / 100 ms)", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (greenThresholdMs == 10 && orangeThresholdMs == 100)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                AssistChip(
                    onClick = { onThresholdsChanged(30, 80) },
                    label = { Text("Gaming (30 / 80 ms)", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (greenThresholdMs == 30 && orangeThresholdMs == 80)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                AssistChip(
                    onClick = { onThresholdsChanged(50, 150) },
                    label = { Text("Relaxed (50 / 150 ms)", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (greenThresholdMs == 50 && orangeThresholdMs == 150)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Summary Row for G / Y / R
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThresholdBadge(color = PingGreen, label = "GREEN", range = "< $greenThresholdMs ms")
                ThresholdBadge(color = PingOrange, label = "YELLOW", range = "$greenThresholdMs–$orangeThresholdMs ms")
                ThresholdBadge(color = PingRed, label = "RED", range = "> $orangeThresholdMs ms")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Green Max Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Green Cutoff (< ms)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "< $greenThresholdMs ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PingGreen
                )
            }
            Slider(
                value = greenThresholdMs.toFloat(),
                onValueChange = { newVal ->
                    val newGreen = (newVal / 5f).roundToInt().coerceAtLeast(1) * 5
                    val safeOrange = maxOf(orangeThresholdMs, newGreen + 10)
                    onThresholdsChanged(newGreen, safeOrange)
                },
                valueRange = 5f..150f,
                steps = 28,
                colors = SliderDefaults.colors(
                    thumbColor = PingGreen,
                    activeTrackColor = PingGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Yellow/Orange Max Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yellow Cutoff (≤ ms)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$greenThresholdMs – $orangeThresholdMs ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PingOrange
                )
            }
            Slider(
                value = orangeThresholdMs.toFloat(),
                onValueChange = { newVal ->
                    val newOrange = (newVal / 5f).roundToInt() * 5
                    val safeOrange = maxOf(newOrange, greenThresholdMs + 5)
                    onThresholdsChanged(greenThresholdMs, safeOrange)
                },
                valueRange = 15f..300f,
                steps = 56,
                colors = SliderDefaults.colors(
                    thumbColor = PingOrange,
                    activeTrackColor = PingOrange
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThresholdBadge(color: androidx.compose.ui.graphics.Color, label: String, range: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = range,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
