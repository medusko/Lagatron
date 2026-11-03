package com.medusko.lagatron.model

enum class LatencyStatus(val colorHex: Long) {
    GREEN(0xFF00E676),  // Fast / Low latency
    ORANGE(0xFFFF9100), // Medium / Warning latency
    RED(0xFFFF1744);    // High latency or timeout

    companion object {
        fun fromLatency(
            latencyMs: Float?,
            greenThresholdMs: Float = 10f,
            orangeThresholdMs: Float = 100f
        ): LatencyStatus {
            val safeOrange = maxOf(orangeThresholdMs, greenThresholdMs + 1f)
            return when {
                latencyMs == null -> RED
                latencyMs < greenThresholdMs -> GREEN
                latencyMs <= safeOrange -> ORANGE
                else -> RED
            }
        }
    }
}

data class PingResult(
    val latencyMs: Float?,
    val isSuccess: Boolean,
    val host: String,
    val status: LatencyStatus = LatencyStatus.fromLatency(latencyMs)
) {
    fun statusForThresholds(greenThresholdMs: Int, orangeThresholdMs: Int): LatencyStatus {
        return LatencyStatus.fromLatency(
            latencyMs = latencyMs,
            greenThresholdMs = greenThresholdMs.toFloat(),
            orangeThresholdMs = orangeThresholdMs.toFloat()
        )
    }

    fun formattedLatency(showMs: Boolean = true): String {
        val unitSuffix = if (showMs) " ms" else ""
        return when {
            latencyMs == null -> "ERR"
            latencyMs < 10f -> "%.1f%s".format(latencyMs, unitSuffix)
            else -> "%.0f%s".format(latencyMs, unitSuffix)
        }
    }
}
