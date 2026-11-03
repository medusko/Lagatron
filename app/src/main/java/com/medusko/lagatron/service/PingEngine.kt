package com.medusko.lagatron.service

import com.medusko.lagatron.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.regex.Pattern

object PingEngine {

    private val PING_TIME_REGEX = Pattern.compile("time=([0-9.]+)\\s*ms")

    suspend fun ping(host: String): PingResult = withContext(Dispatchers.IO) {
        // Attempt native ICMP ping first
        val icmpResult = executeNativePing(host)
        if (icmpResult != null) {
            return@withContext icmpResult
        }

        // Fallback to TCP handshake latency if ICMP is blocked or ping binary fails
        executeTcpPing(host)
    }

    private fun executeNativePing(host: String): PingResult? {
        return try {
            // -c 1: count 1 packet
            // -W 1 or -w 1: timeout 1 second
            val process = ProcessBuilder(
                "/system/bin/ping",
                "-c", "1",
                "-W", "1",
                host
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var latency: Float? = null

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val matcher = PING_TIME_REGEX.matcher(it)
                    if (matcher.find()) {
                        latency = matcher.group(1)?.toFloatOrNull()
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0 && latency != null) {
                PingResult(latencyMs = latency, isSuccess = true, host = host)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun executeTcpPing(host: String): PingResult {
        val ports = listOf(53, 443, 80)
        for (port in ports) {
            var socket: Socket? = null
            try {
                val start = System.nanoTime()
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), 1000)
                val durationMs = (System.nanoTime() - start) / 1_000_000f
                socket.close()
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
