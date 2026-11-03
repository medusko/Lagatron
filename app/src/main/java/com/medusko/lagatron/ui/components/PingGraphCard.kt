package com.medusko.lagatron.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medusko.lagatron.data.PingPoint
import com.medusko.lagatron.model.LatencyStatus
import com.medusko.lagatron.model.PingResult
import com.medusko.lagatron.ui.theme.PingGreen
import com.medusko.lagatron.ui.theme.PingOrange
import com.medusko.lagatron.ui.theme.PingRed

@Composable
fun PingGraphCard(
    history: List<PingPoint>,
    latestPing: PingResult?,
    isRunning: Boolean,
    showMsUnit: Boolean,
    greenThresholdMs: Int,
    orangeThresholdMs: Int,
    modifier: Modifier = Modifier
) {
    val currentStatus = latestPing?.statusForThresholds(greenThresholdMs, orangeThresholdMs)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Latency Graph",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) "Rolling latency (Green <${greenThresholdMs}ms, Yellow ≤${orangeThresholdMs}ms, Red >${orangeThresholdMs}ms)" else "Inactive - start overlay to record",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isRunning && latestPing != null && currentStatus != null) {
                    val statusColor = when (currentStatus) {
                        LatencyStatus.GREEN -> PingGreen
                        LatencyStatus.ORANGE -> PingOrange
                        LatencyStatus.RED -> PingRed
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = latestPing.formattedLatency(showMs = showMsUnit),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats summary row
            val validPoints = history.filter { !it.isInterruption && it.latencyMs >= 0 }.map { it.latencyMs }
            val minPing = if (validPoints.isNotEmpty()) validPoints.minOrNull() ?: 0f else 0f
            val maxPing = if (validPoints.isNotEmpty()) validPoints.maxOrNull() ?: 0f else 0f
            val avgPing = if (validPoints.isNotEmpty()) validPoints.average().toFloat() else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "MIN", value = if (validPoints.isEmpty()) "--" else "%.0f ms".format(minPing))
                StatItem(label = "AVG", value = if (validPoints.isEmpty()) "--" else "%.1f ms".format(avgPing))
                StatItem(label = "MAX", value = if (validPoints.isEmpty()) "--" else "%.0f ms".format(maxPing))
                StatItem(label = "SAMPLES", value = "${validPoints.size}")
            }

            // Canvas Graph
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            val activeColor = when (currentStatus) {
                LatencyStatus.GREEN -> PingGreen
                LatencyStatus.ORANGE -> PingOrange
                LatencyStatus.RED -> PingRed
                null -> MaterialTheme.colorScheme.primary
            }
            val interruptColor = Color(0xFF7E869B)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "Waiting for ping packets..." else "Start overlay to begin plotting",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Horizontal baseline grid
                        drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

                        // Peak reference scaling
                        val peakY = maxOf(maxPing, 100f) * 1.15f
                        val stepX = if (history.size > 1) width / (history.size - 1) else width

                        val linePath = Path()
                        val fillPath = Path()

                        var firstPoint = true

                        history.forEachIndexed { index, point ->
                            val x = index * stepX

                            if (point.isInterruption) {
                                // Close current segment if any
                                if (!firstPoint) {
                                    fillPath.lineTo(x - stepX, height)
                                    fillPath.close()
                                }

                                // Draw vertical interruption line
                                drawLine(
                                    color = interruptColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, height),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )

                                // Draw small indicator pip at top
                                drawCircle(
                                    color = interruptColor,
                                    radius = 3.dp.toPx(),
                                    center = Offset(x, 4f)
                                )

                                firstPoint = true
                            } else {
                                val pingVal = point.latencyMs
                                val safeVal = if (pingVal < 0) peakY else pingVal
                                val y = height - (safeVal / peakY * height).coerceIn(4f, height - 4f)

                                if (firstPoint) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, height)
                                    fillPath.lineTo(x, y)
                                    firstPoint = false
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                        }

                        if (!firstPoint) {
                            val lastX = (history.size - 1) * stepX
                            fillPath.lineTo(lastX, height)
                            fillPath.close()
                        }

                        // Draw area gradient (follows R / Y / G status)
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(activeColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw line
                        drawPath(
                            path = linePath,
                            color = activeColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw end pulse dot on latest point if not interruption
                        val lastPoint = history.lastOrNull()
                        if (lastPoint != null && !lastPoint.isInterruption) {
                            val lastVal = lastPoint.latencyMs
                            val lastSafeVal = if (lastVal < 0) peakY else lastVal
                            val lastX = (history.size - 1) * stepX
                            val lastY = height - (lastSafeVal / peakY * height).coerceIn(4f, height - 4f)

                            drawCircle(
                                color = activeColor.copy(alpha = 0.3f),
                                radius = 6.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                            drawCircle(
                                color = activeColor,
                                radius = 3.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
