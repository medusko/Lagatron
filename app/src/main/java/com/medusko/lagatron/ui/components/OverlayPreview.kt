package com.medusko.lagatron.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medusko.lagatron.model.AppFont
import com.medusko.lagatron.model.DisplayMode
import com.medusko.lagatron.ui.theme.PingGreen
import com.medusko.lagatron.ui.theme.PingOrange
import com.medusko.lagatron.ui.theme.PingRed

@Composable
fun OverlayPreview(
    displayMode: DisplayMode,
    font: AppFont,
    isBold: Boolean,
    isItalic: Boolean,
    hasShadow: Boolean,
    showMsUnit: Boolean,
    fontSizeSp: Float,
    greenThresholdMs: Int,
    orangeThresholdMs: Int,
    modifier: Modifier = Modifier
) {
    val suffix = if (showMsUnit) " ms" else ""
    val sampleGreen = "${maxOf(1, greenThresholdMs / 2)}$suffix"
    val sampleOrange = "${(greenThresholdMs + orangeThresholdMs) / 2}$suffix"
    val sampleRed = "${orangeThresholdMs + 40}$suffix"

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Overlay Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "How the floating indicator will look (${font.title}, %.1f sp)".format(fontSizeSp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // Simulated overlay badges in an adaptive container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMonochrome = (displayMode == DisplayMode.MONOCHROME_TEXT)
                    val defaultText = MaterialTheme.colorScheme.onSurface
                    val greenColor = if (!isMonochrome) PingGreen else defaultText
                    val orangeColor = if (!isMonochrome) PingOrange else defaultText
                    val redColor = if (!isMonochrome) PingRed else defaultText

                    PreviewBadge(
                        mode = displayMode,
                        font = font,
                        isBold = isBold,
                        isItalic = isItalic,
                        hasShadow = hasShadow,
                        fontSizeSp = fontSizeSp,
                        textColor = greenColor,
                        dotColor = PingGreen,
                        sampleText = sampleGreen,
                        sublabel = "< ${greenThresholdMs}ms"
                    )
                    PreviewBadge(
                        mode = displayMode,
                        font = font,
                        isBold = isBold,
                        isItalic = isItalic,
                        hasShadow = hasShadow,
                        fontSizeSp = fontSizeSp,
                        textColor = orangeColor,
                        dotColor = PingOrange,
                        sampleText = sampleOrange,
                        sublabel = "${greenThresholdMs}-${orangeThresholdMs}ms"
                    )
                    PreviewBadge(
                        mode = displayMode,
                        font = font,
                        isBold = isBold,
                        isItalic = isItalic,
                        hasShadow = hasShadow,
                        fontSizeSp = fontSizeSp,
                        textColor = redColor,
                        dotColor = PingRed,
                        sampleText = sampleRed,
                        sublabel = "> ${orangeThresholdMs}ms"
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBadge(
    mode: DisplayMode,
    font: AppFont,
    isBold: Boolean,
    isItalic: Boolean,
    hasShadow: Boolean,
    fontSizeSp: Float,
    textColor: Color,
    dotColor: Color,
    sampleText: String,
    sublabel: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val isMonochrome = (mode == DisplayMode.MONOCHROME_TEXT)
        val boxModifier = if (!isMonochrome) {
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC121212))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            // Completely borderless and background-free in monochrome mode
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        }

        Box(
            modifier = boxModifier,
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (mode == DisplayMode.DOT_ONLY || mode == DisplayMode.BOTH) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                if (mode == DisplayMode.BOTH) {
                    Spacer(modifier = Modifier.width(5.dp))
                }
                if (mode != DisplayMode.DOT_ONLY) {
                    Text(
                        text = sampleText,
                        color = textColor,
                        fontFamily = font.getComposeFont(),
                        fontSize = fontSizeSp.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = if (hasShadow) androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = androidx.compose.ui.geometry.Offset(-1.5f, 1.5f),
                                blurRadius = 1.5f
                            ) else null
                        )
                    )
                }
            }
        }

        Text(
            text = sublabel,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
