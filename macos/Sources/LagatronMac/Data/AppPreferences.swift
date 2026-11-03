import Foundation
import Combine
import ServiceManagement

@MainActor
public final class AppPreferences: ObservableObject {
    public static let shared = AppPreferences()

    private let defaults = UserDefaults.standard

    private enum Keys {
        static let presetServer = "preset_server"
        static let customHost = "custom_host"
        static let displayMode = "display_mode"
        static let overlayX = "overlay_x"
        static let overlayY = "overlay_y"
        static let intervalMs = "interval_ms"
        static let serviceActive = "service_active"
        static let themeMode = "theme_mode"
        static let appFont = "app_font"
        static let fontBold = "font_bold"
        static let fontItalic = "font_italic"
        static let fontSizeSp = "font_size_sp"
        static let textShadow = "text_shadow"
        static let startOnBoot = "start_on_boot"
        static let showFloatingOverlay = "show_floating_overlay"
        static let showMsUnit = "show_ms_unit"
        static let greenThresholdMs = "green_threshold_ms"
        static let orangeThresholdMs = "orange_threshold_ms"
    }

    @Published public var selectedPreset: PresetServer {
        didSet { defaults.set(selectedPreset.rawValue, forKey: Keys.presetServer) }
    }

    @Published public var customHost: String {
        didSet { defaults.set(customHost, forKey: Keys.customHost) }
    }

    @Published public var displayMode: DisplayMode {
        didSet { defaults.set(displayMode.rawValue, forKey: Keys.displayMode) }
    }

    @Published public var appFont: AppFont {
        didSet { defaults.set(appFont.rawValue, forKey: Keys.appFont) }
    }

    @Published public var isBold: Bool {
        didSet { defaults.set(isBold, forKey: Keys.fontBold) }
    }

    @Published public var isItalic: Bool {
        didSet { defaults.set(isItalic, forKey: Keys.fontItalic) }
    }

    @Published public var hasTextShadow: Bool {
        didSet { defaults.set(hasTextShadow, forKey: Keys.textShadow) }
    }

    @Published public var showMsUnit: Bool {
        didSet { defaults.set(showMsUnit, forKey: Keys.showMsUnit) }
    }

    @Published public var greenThresholdMs: Int {
        didSet { defaults.set(greenThresholdMs, forKey: Keys.greenThresholdMs) }
    }

    @Published public var orangeThresholdMs: Int {
        didSet { defaults.set(orangeThresholdMs, forKey: Keys.orangeThresholdMs) }
    }

    @Published public var fontSizeSp: Float {
        didSet { defaults.set(fontSizeSp, forKey: Keys.fontSizeSp) }
    }

    @Published public var startOnBoot: Bool {
        didSet {
            defaults.set(startOnBoot, forKey: Keys.startOnBoot)
            updateLoginItem(enabled: startOnBoot)
        }
    }

    @Published public var showFloatingOverlay: Bool {
        didSet { defaults.set(showFloatingOverlay, forKey: Keys.showFloatingOverlay) }
    }

    @Published public var overlayX: Int {
        didSet { defaults.set(overlayX, forKey: Keys.overlayX) }
    }

    @Published public var overlayY: Int {
        didSet { defaults.set(overlayY, forKey: Keys.overlayY) }
    }

    @Published public var intervalMs: Int {
        didSet { defaults.set(intervalMs, forKey: Keys.intervalMs) }
    }

    @Published public var isServiceRunning: Bool {
        didSet { defaults.set(isServiceRunning, forKey: Keys.serviceActive) }
    }

    @Published public var isDarkMode: Bool {
        didSet { defaults.set(isDarkMode ? "DARK" : "LIGHT", forKey: Keys.themeMode) }
    }

    @Published public var showAboutDialog: Bool = false

    private init() {
        let presetRaw = defaults.string(forKey: Keys.presetServer) ?? PresetServer.google.rawValue
        self.selectedPreset = PresetServer(rawValue: presetRaw) ?? .google

        self.customHost = defaults.string(forKey: Keys.customHost) ?? "1.1.1.1"

        let modeRaw = defaults.string(forKey: Keys.displayMode) ?? DisplayMode.both.rawValue
        self.displayMode = DisplayMode(rawValue: modeRaw) ?? .both

        let fontRaw = defaults.string(forKey: Keys.appFont) ?? AppFont.monospace.rawValue
        self.appFont = AppFont(rawValue: fontRaw) ?? .monospace

        if defaults.object(forKey: Keys.fontBold) == nil {
            self.isBold = true
        } else {
            self.isBold = defaults.bool(forKey: Keys.fontBold)
        }

        self.isItalic = defaults.bool(forKey: Keys.fontItalic)
        self.hasTextShadow = defaults.bool(forKey: Keys.textShadow)

        if defaults.object(forKey: Keys.showMsUnit) == nil {
            self.showMsUnit = true
        } else {
            self.showMsUnit = defaults.bool(forKey: Keys.showMsUnit)
        }

        let savedGreen = defaults.integer(forKey: Keys.greenThresholdMs)
        self.greenThresholdMs = savedGreen > 0 ? savedGreen : 10

        let savedOrange = defaults.integer(forKey: Keys.orangeThresholdMs)
        self.orangeThresholdMs = savedOrange > 0 ? savedOrange : 100

        let savedSize = defaults.float(forKey: Keys.fontSizeSp)
        self.fontSizeSp = savedSize > 0 ? savedSize : 12.0

        self.startOnBoot = defaults.bool(forKey: Keys.startOnBoot)
        self.showFloatingOverlay = defaults.bool(forKey: Keys.showFloatingOverlay)

        if defaults.object(forKey: Keys.overlayX) == nil {
            self.overlayX = 1200
        } else {
            self.overlayX = defaults.integer(forKey: Keys.overlayX)
        }

        if defaults.object(forKey: Keys.overlayY) == nil {
            self.overlayY = 40
        } else {
            self.overlayY = defaults.integer(forKey: Keys.overlayY)
        }

        let savedInterval = defaults.integer(forKey: Keys.intervalMs)
        self.intervalMs = savedInterval > 0 ? savedInterval : 1000

        if defaults.object(forKey: Keys.serviceActive) == nil {
            self.isServiceRunning = true // Start active in menu bar by default on macOS
        } else {
            self.isServiceRunning = defaults.bool(forKey: Keys.serviceActive)
        }

        let themeRaw = defaults.string(forKey: Keys.themeMode) ?? "DARK"
        self.isDarkMode = (themeRaw != "LIGHT")
    }

    public func getActiveHost() -> String {
        if selectedPreset == .custom {
            return Self.cleanHostInput(customHost)
        } else {
            return selectedPreset.host
        }
    }

    public static func cleanHostInput(_ input: String) -> String {
        var cleaned = input.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.lowercased().hasPrefix("http://") {
            cleaned = String(cleaned.dropFirst(7))
        } else if cleaned.lowercased().hasPrefix("https://") {
            cleaned = String(cleaned.dropFirst(8))
        }
        if let slashIndex = cleaned.firstIndex(of: "/") {
            cleaned = String(cleaned[..<slashIndex])
        }
        if let colonIndex = cleaned.firstIndex(of: ":") {
            cleaned = String(cleaned[..<colonIndex])
        }
        return cleaned.isEmpty ? "8.8.8.8" : cleaned
    }

    private func updateLoginItem(enabled: Bool) {
        if #available(macOS 13.0, *) {
            do {
                if enabled {
                    try SMAppService.mainApp.register()
                } else {
                    try SMAppService.mainApp.unregister()
                }
            } catch {
                // Fallback to User LaunchAgent plist if running as an unsigned local app bundle
                updateLaunchAgentFallback(enabled: enabled)
            }
        }
    }

    private func updateLaunchAgentFallback(enabled: Bool) {
        let fm = FileManager.default
        guard let libraryDir = fm.urls(for: .libraryDirectory, in: .userDomainMask).first else { return }
        let launchAgentsDir = libraryDir.appendingPathComponent("LaunchAgents")
        let plistURL = launchAgentsDir.appendingPathComponent("com.medusko.lagatron.mac.plist")

        if enabled {
            guard let execPath = Bundle.main.executablePath else { return }
            try? fm.createDirectory(at: launchAgentsDir, withIntermediateDirectories: true)
            let plist: [String: Any] = [
                "Label": "com.medusko.lagatron.mac",
                "ProgramArguments": [execPath],
                "RunAtLoad": true
            ]
            if let data = try? PropertyListSerialization.data(fromPropertyList: plist, format: .xml, options: 0) {
                try? data.write(to: plistURL)
            }
        } else {
            try? fm.removeItem(at: plistURL)
        }
    }
}
