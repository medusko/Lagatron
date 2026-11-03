package com.medusko.lagatron.data

import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PingPoint(
    val latencyMs: Float, // -1 for drop, -2 for interruption marker
    val isInterruption: Boolean = false,
    val label: String = ""
)

object PingHistoryRepository {
    private const val MAX_HISTORY_SIZE = 50

    private val _history = MutableStateFlow<List<PingPoint>>(emptyList())
    val history: StateFlow<List<PingPoint>> = _history.asStateFlow()

    private val _latestPing = MutableStateFlow<PingResult?>(null)
    val latestPing: StateFlow<PingResult?> = _latestPing.asStateFlow()

    fun recordPing(result: PingResult) {
        _latestPing.value = result

        val currentList = _history.value.toMutableList()
        val latency = result.latencyMs ?: -1f // -1 represents packet drop / timeout
        currentList.add(PingPoint(latencyMs = latency, isInterruption = false))

        if (currentList.size > MAX_HISTORY_SIZE) {
            currentList.removeAt(0)
        }
        _history.value = currentList
    }

    fun recordInterruption(reason: String) {
        val currentList = _history.value.toMutableList()
        // Only add interruption if the list isn't empty and the last item isn't already an interruption
        if (currentList.isNotEmpty() && !currentList.last().isInterruption) {
            currentList.add(PingPoint(latencyMs = -2f, isInterruption = true, label = reason))
            if (currentList.size > MAX_HISTORY_SIZE) {
                currentList.removeAt(0)
            }
            _history.value = currentList
        }
    }

    fun clear() {
        _history.value = emptyList()
        _latestPing.value = null
    }
}
