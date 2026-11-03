import Foundation
import Combine

@MainActor
public final class PingMonitorService: ObservableObject {
    public static let shared = PingMonitorService()

    private let maxHistorySize = 50
    private let prefs = AppPreferences.shared

    @Published public private(set) var history: [PingPoint] = []
    @Published public private(set) var latestPing: PingResult? = nil

    private var pingTask: Task<Void, Never>? = nil
    public var onPingUpdated: ((PingResult?) -> Void)?

    private init() {}

    public func start() {
        prefs.isServiceRunning = true
        recordInterruption(reason: "Started")
        startPingLoop()
        onPingUpdated?(latestPing)
    }

    public func stop() {
        prefs.isServiceRunning = false
        recordInterruption(reason: "Stopped")
        pingTask?.cancel()
        pingTask = nil
        onPingUpdated?(nil)
    }

    public func toggle() {
        if prefs.isServiceRunning {
            stop()
        } else {
            start()
        }
    }

    public func notifyConfigChanged() {
        onPingUpdated?(latestPing)
    }

    private func startPingLoop() {
        pingTask?.cancel()
        var currentHost = prefs.getActiveHost()

        pingTask = Task { [weak self] in
            guard let self = self else { return }
            while !Task.isCancelled && self.prefs.isServiceRunning {
                let host = self.prefs.getActiveHost()
                if host != currentHost {
                    self.recordInterruption(reason: "Target: \(host)")
                    currentHost = host
                }

                let result = await PingEngine.ping(host: host)
                if Task.isCancelled || !self.prefs.isServiceRunning { break }

                self.recordPing(result)
                self.onPingUpdated?(result)

                let intervalNs = UInt64(max(250, self.prefs.intervalMs)) * 1_000_000
                try? await Task.sleep(nanoseconds: intervalNs)
            }
        }
    }

    public func recordPing(_ result: PingResult) {
        latestPing = result
        let latency = result.latencyMs ?? -1.0
        var updated = history
        updated.append(PingPoint(latencyMs: latency, isInterruption: false))
        if updated.count > maxHistorySize {
            updated.removeFirst(updated.count - maxHistorySize)
        }
        history = updated
    }

    public func recordInterruption(reason: String) {
        var updated = history
        if let last = updated.last, !last.isInterruption {
            updated.append(PingPoint(latencyMs: -2.0, isInterruption: true, label: reason))
            if updated.count > maxHistorySize {
                updated.removeFirst(updated.count - maxHistorySize)
            }
            history = updated
        }
    }

    public func clearHistory() {
        history = []
        latestPing = nil
    }
}
