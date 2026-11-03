package com.medusko.lagatron.model

enum class LatencyStatus(val colorHex: Long) {
    GREEN(0xFF00E676),  // < 10ms
    ORANGE(0xFFFF9100), // 10ms - 100ms
    RED(0xFFFF1744);    // > 100ms or timeout

    companion object {
        fun fromLatency(latencyMs: Float?): LatencyStatus {
            return when {
                latencyMs == null -> RED
                latencyMs < 10f -> GREEN
                latencyMs <= 100f -> ORANGE
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
    fun formattedLatency(): String {
        return when {
            latencyMs == null -> "ERR"
            latencyMs < 10f -> "%.1f ms".format(latencyMs)
            else -> "%.0f ms".format(latencyMs)
        }
    }
}
