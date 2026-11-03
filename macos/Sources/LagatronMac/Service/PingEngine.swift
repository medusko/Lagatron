import Foundation
import Darwin

public enum PingEngine {
    private static let pingTimeRegex = try! NSRegularExpression(pattern: "time=([0-9.]+)\\s*ms", options: [])

    public static func ping(host: String) async -> PingResult {
        return await Task.detached(priority: .utility) {
            if let icmpResult = executeNativePing(host: host) {
                return icmpResult
            }
            return executeTcpPing(host: host)
        }.value
    }

    private static func executeNativePing(host: String) -> PingResult? {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/sbin/ping")
        // -c 1: count 1 packet
        // -W 1000: wait 1000ms (1 second) for a reply on macOS
        process.arguments = ["-c", "1", "-W", "1000", host]

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

            let nsRange = NSRange(output.startIndex..<output.endIndex, in: output)
            if let match = pingTimeRegex.firstMatch(in: output, options: [], range: nsRange),
               let groupRange = Range(match.range(at: 1), in: output),
               let latency = Float(output[groupRange]) {
                return PingResult(latencyMs: latency, isSuccess: true, host: host)
            }
            return nil
        } catch {
            return nil
        }
    }

    private static func executeTcpPing(host: String) -> PingResult {
        let ports: [UInt16] = [53, 443, 80]
        for port in ports {
            if let durationMs = measureTcpConnect(host: host, port: port, timeoutMs: 1000) {
                return PingResult(latencyMs: durationMs, isSuccess: true, host: host)
            }
        }
        return PingResult(latencyMs: nil, isSuccess: false, host: host)
    }

    private static func measureTcpConnect(host: String, port: UInt16, timeoutMs: Int32) -> Float? {
        var hints = addrinfo()
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        hints.ai_protocol = IPPROTO_TCP

        var res: UnsafeMutablePointer<addrinfo>?
        let status = getaddrinfo(host, String(port), &hints, &res)
        guard status == 0, let firstAddr = res else { return nil }
        defer { freeaddrinfo(res) }

        let sock = socket(firstAddr.pointee.ai_family, firstAddr.pointee.ai_socktype, firstAddr.pointee.ai_protocol)
        guard sock >= 0 else { return nil }
        defer { close(sock) }

        // Set non-blocking mode for 1s timeout
        let flags = fcntl(sock, F_GETFL, 0)
        _ = fcntl(sock, F_SETFL, flags | O_NONBLOCK)

        let start = DispatchTime.now().uptimeNanoseconds
        let connectRes = connect(sock, firstAddr.pointee.ai_addr, firstAddr.pointee.ai_addrlen)

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
