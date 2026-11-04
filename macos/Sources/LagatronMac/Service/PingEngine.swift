import Foundation
import Darwin

public final class PingEngine {
    // Port affinity cache to avoid trying 3 ports sequentially on every TCP ping
    private static var preferredTcpPort: [String: UInt16] = [:]

    // DNS resolution cache to avoid resolving hostname on every single ping
    private struct CachedAddr {
        let addr: sockaddr_storage
        let len: socklen_t
        let expiresAt: TimeInterval
    }
    private static var addressCache: [String: CachedAddr] = [:]
    private static let cacheLock = NSLock()

    // MARK: - Persistent Streaming Ping
    public static func stream(host: String, intervalMs: Int) -> AsyncStream<PingResult> {
        AsyncStream { continuation in
            let process = Process()
            process.executableURL = URL(fileURLWithPath: "/sbin/ping")
            let intervalSec = String(format: "%.2f", max(0.25, Double(intervalMs) / 1000.0))
            process.arguments = ["-n", "-i", intervalSec, host]

            let pipe = Pipe()
            process.standardOutput = pipe
            process.standardError = pipe

            continuation.onTermination = { @Sendable _ in
                if process.isRunning {
                    process.terminate()
                }
            }

            DispatchQueue.global(qos: .utility).async {
                do {
                    try process.run()
                } catch {
                    runTcpFallbackLoop(host: host, intervalMs: intervalMs, continuation: continuation)
                    return
                }

                let handle = pipe.fileHandleForReading
                var buffer = Data()
                var receivedAnyOutput = false

                while process.isRunning {
                    let chunk = handle.availableData
                    if chunk.isEmpty {
                        break
                    }
                    buffer.append(chunk)

                    while let newlineRange = buffer.range(of: Data([0x0A])) { // '\n'
                        let lineData = buffer.subdata(in: 0..<newlineRange.lowerBound)
                        buffer.removeSubrange(0..<newlineRange.upperBound)

                        if let line = String(data: lineData, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines), !line.isEmpty {
                            if let result = parsePingLine(line, host: host) {
                                receivedAnyOutput = true
                                continuation.yield(result)
                            }
                        }
                    }
                }

                process.waitUntilExit()

                // If process exited without producing valid pings (e.g., DNS error, permission, or blocked ICMP), fall back to TCP
                if !receivedAnyOutput {
                    runTcpFallbackLoop(host: host, intervalMs: intervalMs, continuation: continuation)
                } else {
                    continuation.finish()
                }
            }
        }
    }

    private static func runTcpFallbackLoop(host: String, intervalMs: Int, continuation: AsyncStream<PingResult>.Continuation) {
        while true {
            let res = executeTcpPing(host: host)
            continuation.yield(res)
            Thread.sleep(forTimeInterval: max(0.25, Double(intervalMs) / 1000.0))
        }
    }

    public static func parsePingLine(_ line: String, host: String) -> PingResult? {
        // Sample: "64 bytes from 1.1.1.1: icmp_seq=0 ttl=54 time=10.514 ms"
        if let timeRange = line.range(of: "time=") {
            let afterTime = line[timeRange.upperBound...]
            if let msRange = afterTime.range(of: " ms") {
                let numPart = afterTime[..<msRange.lowerBound].trimmingCharacters(in: .whitespaces)
                if let latency = Float(numPart) {
                    return PingResult(latencyMs: latency, isSuccess: true, host: host)
                }
            }
        }
        if line.contains("Request timeout") || line.contains("100.0% packet loss") || line.contains("Host is down") || line.contains("No route to host") {
            return PingResult(latencyMs: nil, isSuccess: false, host: host)
        }
        return nil
    }

    // Single-shot ping for on-demand checks
    public static func ping(host: String) async -> PingResult {
        return await Task.detached(priority: .utility) {
            if let icmpResult = executeNativePingOnce(host: host) {
                return icmpResult
            }
            return executeTcpPing(host: host)
        }.value
    }

    private static func executeNativePingOnce(host: String) -> PingResult? {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/sbin/ping")
        process.arguments = ["-c", "1", "-W", "1000", "-n", host]

        let pipe = Pipe()
        process.standardOutput = pipe
        process.standardError = pipe

        do {
            try process.run()
            let data = pipe.fileHandleForReading.readDataToEndOfFile()
            process.waitUntilExit()

            guard process.terminationStatus == 0,
                  let output = String(data: data, encoding: .utf8) else {
                return nil
            }

            for line in output.components(separatedBy: .newlines) {
                if let result = parsePingLine(line, host: host), result.isSuccess {
                    return result
                }
            }
            return nil
        } catch {
            return nil
        }
    }

    // MARK: - Optimized TCP Ping with Port Affinity & DNS Cache
    public static func executeTcpPing(host: String) -> PingResult {
        cacheLock.lock()
        let preferred = preferredTcpPort[host]
        cacheLock.unlock()

        var ports: [UInt16] = [53, 443, 80]
        if let p = preferred {
            ports.removeAll(where: { $0 == p })
            ports.insert(p, at: 0)
        }

        for port in ports {
            if let durationMs = measureTcpConnect(host: host, port: port, timeoutMs: 1000) {
                cacheLock.lock()
                preferredTcpPort[host] = port
                cacheLock.unlock()
                return PingResult(latencyMs: durationMs, isSuccess: true, host: host)
            }
        }
        return PingResult(latencyMs: nil, isSuccess: false, host: host)
    }

    private static func getOrResolveAddress(host: String, port: UInt16) -> (sockaddr_storage, socklen_t)? {
        let now = Date().timeIntervalSince1970
        let cacheKey = "\(host):\(port)"

        cacheLock.lock()
        if let cached = addressCache[cacheKey], cached.expiresAt > now {
            cacheLock.unlock()
            return (cached.addr, cached.len)
        }
        cacheLock.unlock()

        var hints = addrinfo()
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        hints.ai_protocol = IPPROTO_TCP

        var res: UnsafeMutablePointer<addrinfo>?
        let status = getaddrinfo(host, String(port), &hints, &res)
        guard status == 0, let firstAddr = res else { return nil }
        defer { freeaddrinfo(res) }

        var storage = sockaddr_storage()
        memcpy(&storage, firstAddr.pointee.ai_addr, Int(firstAddr.pointee.ai_addrlen))
        let len = firstAddr.pointee.ai_addrlen

        cacheLock.lock()
        addressCache[cacheKey] = CachedAddr(addr: storage, len: len, expiresAt: now + 60.0) // 60s DNS TTL
        cacheLock.unlock()

        return (storage, len)
    }

    private static func measureTcpConnect(host: String, port: UInt16, timeoutMs: Int32) -> Float? {
        guard let resolved = getOrResolveAddress(host: host, port: port) else { return nil }
        var addr = resolved.0
        let addrLen = resolved.1

        let sock = socket(Int32(addr.ss_family), SOCK_STREAM, IPPROTO_TCP)
        guard sock >= 0 else { return nil }
        defer { close(sock) }

        // Set non-blocking mode
        let flags = fcntl(sock, F_GETFL, 0)
        _ = fcntl(sock, F_SETFL, flags | O_NONBLOCK)

        let start = DispatchTime.now().uptimeNanoseconds
        let connectRes = withUnsafePointer(to: &addr) { ptr in
            ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                connect(sock, sa, addrLen)
            }
        }

        if connectRes == 0 {
            let elapsed = Float(DispatchTime.now().uptimeNanoseconds - start) / 1_000_000.0
            return elapsed
        }

        guard errno == EINPROGRESS else { return nil }

        var pfd = pollfd(fd: sock, events: Int16(POLLOUT), revents: 0)
        let pollRes = poll(&pfd, 1, timeoutMs)
        guard pollRes > 0 && (pfd.revents & Int16(POLLOUT)) != 0 else {
            return nil
        }

        var soError: Int32 = 0
        var len = socklen_t(MemoryLayout<Int32>.size)
        if getsockopt(sock, SOL_SOCKET, SO_ERROR, &soError, &len) == 0 && soError == 0 {
            let elapsed = Float(DispatchTime.now().uptimeNanoseconds - start) / 1_000_000.0
            return elapsed
        }
        return nil
    }
}
