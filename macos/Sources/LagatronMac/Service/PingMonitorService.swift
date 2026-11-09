import Foundation
import Combine
import AppKit

@MainActor
public final class PingMonitorService: ObservableObject {
    public static let shared = PingMonitorService()

    private let maxHistorySize = 50
    private let prefs = AppPreferences.shared

    @Published public private(set) var history: [PingPoint] = []
    @Published public private(set) var latestPing: PingResult? = nil

    // Precomputed rolling statistics to eliminate repeated O(N) calculations in SwiftUI views
    @Published public private(set) var minPing: Float = 0
    @Published public private(set) var maxPing: Float = 0
    @Published public private(set) var avgPing: Float = 0
    @Published public private(set) var validSamplesCount: Int = 0

    private var pingTask: Task<Void, Never>? = nil
    private var hostDebounceTask: Task<Void, Never>? = nil
    private var sleepObserver: Any? = nil
    private var wakeObserver: Any? = nil
    private var lastObservedHost: String = ""
    private var lastObservedInterval: Int = 0

    public var onPingUpdated: ((PingResult?) -> Void)?

    private init() {
        setupSleepWakeObservers()
    }

    private func setupSleepWakeObservers() {
        sleepObserver = NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.willSleepNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self = self, self.prefs.isServiceRunning else { return }
                self.recordInterruption(reason: "Sleep")
                self.pausePingLoop()
            }
        }

        wakeObserver = NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.didWakeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self = self, self.prefs.isServiceRunning else { return }
                self.recordInterruption(reason: "Wake")
                self.startPingLoop()
            }
        }
    }

    public func start() {
        prefs.isServiceRunning = true
        recordInterruption(reason: "Started")
        startPingLoop()
        onPingUpdated?(latestPing)
    }

    public func stop() {
        prefs.isServiceRunning = false
        recordInterruption(reason: "Stopped")
        pausePingLoop()
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
        hostDebounceTask?.cancel()
        hostDebounceTask = nil
        let activeHost = prefs.getActiveHost()
        let activeInterval = prefs.intervalMs

        if activeHost != lastObservedHost || activeInterval != lastObservedInterval {
            if prefs.isServiceRunning {
                recordInterruption(reason: "Target: \(activeHost)")
                startPingLoop()
            }
        }
        onPingUpdated?(latestPing)
    }

    public func notifyCustomHostChanged() {
        hostDebounceTask?.cancel()
        hostDebounceTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 500_000_000)
            guard let self = self, !Task.isCancelled else { return }
            self.notifyConfigChanged()
        }
    }

    private func pausePingLoop() {
        hostDebounceTask?.cancel()
        hostDebounceTask = nil
        pingTask?.cancel()
        pingTask = nil
    }

    private func startPingLoop() {
        pausePingLoop()

        let host = prefs.getActiveHost()
        let interval = prefs.intervalMs
        lastObservedHost = host
        lastObservedInterval = interval

        pingTask = Task { [weak self] in
            guard let self = self else { return }

            let stream = PingEngine.stream(host: host, intervalMs: interval)
            for await result in stream {
                if Task.isCancelled || !self.prefs.isServiceRunning {
                    break
                }
                self.recordPing(result)
                self.onPingUpdated?(result)
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
        updateStatistics(history: updated)
    }

    public func recordInterruption(reason: String) {
        var updated = history
        if let last = updated.last, !last.isInterruption {
            updated.append(PingPoint(latencyMs: -2.0, isInterruption: true, label: reason))
            if updated.count > maxHistorySize {
                updated.removeFirst(updated.count - maxHistorySize)
            }
            history = updated
            updateStatistics(history: updated)
        }
    }

    private func updateStatistics(history: [PingPoint]) {
        var minVal: Float = .greatestFiniteMagnitude
        var maxVal: Float = 0
        var sum: Float = 0
        var count: Int = 0

        for point in history {
            if !point.isInterruption && point.latencyMs >= 0 {
                let ms = point.latencyMs
                if ms < minVal { minVal = ms }
                if ms > maxVal { maxVal = ms }
                sum += ms
                count += 1
            }
        }

        if count > 0 {
            self.minPing = minVal
            self.maxPing = maxVal
            self.avgPing = sum / Float(count)
            self.validSamplesCount = count
        } else {
            self.minPing = 0
            self.maxPing = 0
            self.avgPing = 0
            self.validSamplesCount = 0
        }
    }

    public func clearHistory() {
        history = []
        latestPing = nil
        minPing = 0
        maxPing = 0
        avgPing = 0
        validSamplesCount = 0
    }
}
