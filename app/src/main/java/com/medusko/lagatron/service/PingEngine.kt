package com.medusko.lagatron.service

import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object PingEngine {

    // Port affinity cache to remember which port answered for a given host
    private val preferredPorts = ConcurrentHashMap<String, Int>()

    // DNS resolution cache to avoid resolving hostname on every single ping
    private val dnsCache = ConcurrentHashMap<String, Pair<InetAddress, Long>>()

    private fun sanitizeHost(host: String): String {
        val trimmed = host.trim()
        return if (trimmed.isEmpty() || trimmed.startsWith("-")) "8.8.8.8" else trimmed
    }

    fun stream(host: String, intervalMs: Long): Flow<PingResult> = flow {
        val safeHost = sanitizeHost(host)
        var process: Process? = null
        var completionHandle: DisposableHandle? = null
        try {
            val intervalSec = maxOf(0.25f, intervalMs / 1000f)
            val builder = ProcessBuilder("/system/bin/ping", "-i", String.format(Locale.US, "%.2f", intervalSec), safeHost)
            builder.redirectErrorStream(true)
            val startedProcess = builder.start()
            process = startedProcess

            completionHandle = currentCoroutineContext()[Job]?.invokeOnCompletion {
                try {
                    startedProcess.destroy()
                } catch (_: Exception) {}
            }

            var receivedAnyValid = false
            BufferedReader(InputStreamReader(startedProcess.inputStream)).use { reader ->
                while (currentCoroutineContext().isActive) {
                    val line = reader.readLine() ?: break
                    val result = parsePingLine(line, safeHost)
                    if (result != null) {
                        receivedAnyValid = true
                        emit(result)
                    }
                }
            }

            if (!receivedAnyValid && currentCoroutineContext().isActive) {
                // Fall back to periodic TCP if ICMP ping failed to start or produced no output
                while (currentCoroutineContext().isActive) {
                    emit(executeTcpPing(safeHost))
                    delay(intervalMs)
                }
            }
        } catch (_: Exception) {
            while (currentCoroutineContext().isActive) {
                emit(executeTcpPing(safeHost))
                delay(intervalMs)
            }
        } finally {
            completionHandle?.dispose()
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    suspend fun ping(host: String): PingResult = withContext(Dispatchers.IO) {
        val safeHost = sanitizeHost(host)
        val icmpResult = executeNativePingOnce(safeHost)
        if (icmpResult != null) {
            return@withContext icmpResult
        }
        executeTcpPing(safeHost)
    }

    private fun executeNativePingOnce(host: String): PingResult? {
        val safeHost = sanitizeHost(host)
        var process: Process? = null
        return try {
            val startedProcess = ProcessBuilder(
                "/system/bin/ping",
                "-c", "1",
                "-W", "1",
                safeHost
            ).redirectErrorStream(true).start()
            process = startedProcess

            var result: PingResult? = null
            BufferedReader(InputStreamReader(startedProcess.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val parsed = parsePingLine(it, safeHost)
                        if (parsed != null && parsed.isSuccess) {
                            result = parsed
                        }
                    }
                }
            }

            val exitCode = startedProcess.waitFor()
            if (exitCode == 0 && result != null) {
                result
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    public fun parsePingLine(line: String, host: String): PingResult? {
        if (line.contains("(DUP!)") || line.contains("Warning: time of day")) {
            return null
        }
        val timeIdx = line.indexOf("time=")
        if (timeIdx != -1) {
            val msIdx = line.indexOf(" ms", startIndex = timeIdx)
            if (msIdx != -1) {
                val numStr = line.substring(timeIdx + 5, msIdx).trim()
                val latency = numStr.toFloatOrNull()
                if (latency != null && latency >= 0f && latency < 10000f) {
                    return PingResult(latencyMs = latency, isSuccess = true, host = host)
                }
            }
        }
        if (line.contains("Request timeout") ||
            line.contains("100% packet loss") ||
            line.contains("100.0% packet loss") ||
            line.contains("Unreachable") ||
            line.contains("Host is down")
        ) {
            return PingResult(latencyMs = null, isSuccess = false, host = host)
        }
        return null
    }

    private fun getOrResolveAddress(host: String): InetAddress? {
        val now = System.currentTimeMillis()
        val cached = dnsCache[host]
        if (cached != null && cached.second > now) {
            return cached.first
        }
        return try {
            val addr = InetAddress.getByName(host)
            dnsCache[host] = Pair(addr, now + 60_000L) // 60-second DNS TTL
            addr
        } catch (_: Exception) {
            null
        }
    }

    public fun executeTcpPing(host: String): PingResult {
        val addr = getOrResolveAddress(host) ?: return PingResult(latencyMs = null, isSuccess = false, host = host)
        val preferred = preferredPorts[host]
        val ports = if (preferred != null) {
            listOf(preferred) + listOf(53, 443, 80).filter { it != preferred }
        } else {
            listOf(53, 443, 80)
        }

        for (port in ports) {
            var socket: Socket? = null
            try {
                socket = Socket()
                val socketAddress = InetSocketAddress(addr, port)
                val start = System.nanoTime()
                socket.connect(socketAddress, 1000)
                val durationMs = ((System.nanoTime() - start) / 1_000_000f).coerceIn(0f, 10000f)
                socket.close()
                preferredPorts[host] = port
                return PingResult(latencyMs = durationMs, isSuccess = true, host = host)
            } catch (_: Exception) {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }

        return PingResult(latencyMs = null, isSuccess = false, host = host)
    }
}
