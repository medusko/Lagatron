package com.medusko.lagatron.service

import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.Dispatchers
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

    fun stream(host: String, intervalMs: Long): Flow<PingResult> = flow {
        var process: Process? = null
        try {
            val intervalSec = maxOf(0.25f, intervalMs / 1000f)
            val builder = ProcessBuilder("/system/bin/ping", "-i", String.format(Locale.US, "%.2f", intervalSec), host)
            builder.redirectErrorStream(true)
            process = builder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var receivedAnyValid = false

            while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                val result = parsePingLine(line, host)
                if (result != null) {
                    receivedAnyValid = true
                    emit(result)
                }
            }

            if (!receivedAnyValid) {
                // Fall back to periodic TCP if ICMP ping failed to start or produced no output
                while (currentCoroutineContext().isActive) {
                    emit(executeTcpPing(host))
                    delay(intervalMs)
                }
            }
        } catch (_: Exception) {
            while (currentCoroutineContext().isActive) {
                emit(executeTcpPing(host))
                delay(intervalMs)
            }
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    suspend fun ping(host: String): PingResult = withContext(Dispatchers.IO) {
        val icmpResult = executeNativePingOnce(host)
        if (icmpResult != null) {
            return@withContext icmpResult
        }
        executeTcpPing(host)
    }

    private fun executeNativePingOnce(host: String): PingResult? {
        return try {
            val process = ProcessBuilder(
                "/system/bin/ping",
                "-c", "1",
                "-W", "1",
                host
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var result: PingResult? = null

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val parsed = parsePingLine(it, host)
                    if (parsed != null && parsed.isSuccess) {
                        result = parsed
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0 && result != null) {
                result
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    public fun parsePingLine(line: String, host: String): PingResult? {
        val timeIdx = line.indexOf("time=")
        if (timeIdx != -1) {
            val msIdx = line.indexOf(" ms", startIndex = timeIdx)
            if (msIdx != -1) {
                val numStr = line.substring(timeIdx + 5, msIdx).trim()
                val latency = numStr.toFloatOrNull()
                if (latency != null) {
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
                val durationMs = (System.nanoTime() - start) / 1_000_000f
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
