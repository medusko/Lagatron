package com.medusko.lagatron.data

import androidx.compose.runtime.Immutable
import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class PingPoint(
    val latencyMs: Float, // -1 for drop, -2 for interruption marker
    val isInterruption: Boolean = false,
    val label: String = ""
)

@Immutable
data class PingStats(
    val minMs: Float = 0f,
    val maxMs: Float = 0f,
    val avgMs: Float = 0f,
    val sampleCount: Int = 0
)

object PingHistoryRepository {
    private const val MAX_HISTORY_SIZE = 50
    private val lock = Any()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _history = MutableStateFlow<List<PingPoint>>(emptyList())
    val history: StateFlow<List<PingPoint>> = _history.asStateFlow()

    private val _latestPing = MutableStateFlow<PingResult?>(null)
    val latestPing: StateFlow<PingResult?> = _latestPing.asStateFlow()

    private val _stats = MutableStateFlow(PingStats())
    val stats: StateFlow<PingStats> = _stats.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun recordPing(result: PingResult) {
        synchronized(lock) {
            _latestPing.value = result

            val currentList = _history.value.toMutableList()
            val latency = result.latencyMs ?: -1f // -1 represents packet drop / timeout
            currentList.add(PingPoint(latencyMs = latency, isInterruption = false))

            if (currentList.size > MAX_HISTORY_SIZE) {
                currentList.removeAt(0)
            }
            _history.value = currentList
            recomputeStats(currentList)
        }
    }

    fun recordInterruption(reason: String) {
        synchronized(lock) {
            val currentList = _history.value.toMutableList()
            if (currentList.isNotEmpty() && !currentList.last().isInterruption) {
                currentList.add(PingPoint(latencyMs = -2f, isInterruption = true, label = reason))
                if (currentList.size > MAX_HISTORY_SIZE) {
                    currentList.removeAt(0)
                }
                _history.value = currentList
                recomputeStats(currentList)
            }
        }
    }

    private fun recomputeStats(list: List<PingPoint>) {
        var minVal = Float.MAX_VALUE
        var maxVal = 0f
        var sum = 0f
        var count = 0

        for (point in list) {
            if (!point.isInterruption && point.latencyMs >= 0) {
                val ms = point.latencyMs
                if (ms < minVal) minVal = ms
                if (ms > maxVal) maxVal = ms
                sum += ms
                count++
            }
        }

        if (count > 0) {
            _stats.value = PingStats(
                minMs = minVal,
                maxMs = maxVal,
                avgMs = sum / count,
                sampleCount = count
            )
        } else {
            _stats.value = PingStats()
        }
    }

    fun clear() {
        synchronized(lock) {
            _history.value = emptyList()
            _latestPing.value = null
            _stats.value = PingStats()
        }
    }
}
