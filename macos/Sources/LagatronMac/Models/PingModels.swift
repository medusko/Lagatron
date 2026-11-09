import Foundation
import SwiftUI
import AppKit
import CoreText

public enum LatencyStatus: String, CaseIterable {
    case green = "GREEN"   // < 10ms
    case orange = "ORANGE" // 10ms - 100ms
    case red = "RED"       // > 100ms or timeout

    public var colorHex: UInt32 {
        switch self {
        case .green: return 0x00E676
        case .orange: return 0xFF9100
        case .red: return 0xFF1744
        }
    }

    public var swiftUIColor: Color {
        switch self {
        case .green: return LagatronColors.pingGreen
        case .orange: return LagatronColors.pingOrange
        case .red: return LagatronColors.pingRed
        }
    }

    public var nsColor: NSColor {
        NSColor(hex: colorHex)
    }

    public static func fromLatency(
        _ latencyMs: Float?,
        greenThresholdMs: Float = 10.0,
        orangeThresholdMs: Float = 100.0
    ) -> LatencyStatus {
        guard let ms = latencyMs else { return .red }
        let safeOrange = max(orangeThresholdMs, greenThresholdMs + 1.0)
        if ms < greenThresholdMs {
            return .green
        } else if ms <= safeOrange {
            return .orange
        } else {
            return .red
        }
    }
}

public struct PingResult: Equatable {
    public let latencyMs: Float?
    public let isSuccess: Bool
    public let host: String
    public let status: LatencyStatus

    public init(latencyMs: Float?, isSuccess: Bool, host: String) {
        self.latencyMs = latencyMs
        self.isSuccess = isSuccess
        self.host = host
        self.status = LatencyStatus.fromLatency(latencyMs)
    }

    public func statusForThresholds(greenThresholdMs: Int, orangeThresholdMs: Int) -> LatencyStatus {
        LatencyStatus.fromLatency(
            latencyMs,
            greenThresholdMs: Float(greenThresholdMs),
            orangeThresholdMs: Float(orangeThresholdMs)
        )
    }

    public func formattedLatency(showMs: Bool = true) -> String {
        guard let ms = latencyMs else { return "ERR" }
        let suffix = showMs ? " ms" : ""
        if ms < 10.0 {
            return String(format: "%.1f%@", ms, suffix)
        } else {
            return String(format: "%.0f%@", ms, suffix)
        }
    }
}

public enum PresetServer: String, CaseIterable, Identifiable {
    case google = "GOOGLE"
    case cloudflare = "CLOUDFLARE"
    case quad9 = "QUAD9"
    case custom = "CUSTOM"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .google: return "Google DNS"
        case .cloudflare: return "Cloudflare DNS"
        case .quad9: return "Quad9 DNS"
        case .custom: return "Custom Host"
        }
    }

    public var host: String {
        switch self {
        case .google: return "8.8.8.8"
        case .cloudflare: return "1.1.1.1"
        case .quad9: return "9.9.9.9"
        case .custom: return ""
        }
    }

    public var chipLabel: String {
        switch self {
        case .custom:
            return "Custom"
        default:
            let shortName = title.components(separatedBy: " ").first ?? title
            return "\(shortName) (\(host))"
        }
    }
}

public enum DisplayMode: String, CaseIterable, Identifiable {
    case dotOnly = "DOT_ONLY"
    case msOnly = "MS_ONLY"
    case both = "BOTH"
    case monochromeText = "MONOCHROME_TEXT"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .dotOnly: return "Dot Only"
        case .msOnly: return "Latency (ms)"
        case .both: return "Dot & Latency"
        case .monochromeText: return "White Text Only"
        }
    }

    public var description: String {
        switch self {
        case .dotOnly: return "Minimalist colored status dot"
        case .msOnly: return "Numeric latency in milliseconds"
        case .both: return "Status dot with latency reading"
        case .monochromeText: return "Clean borderless white text without container or colors"
        }
    }
}

public enum AppFont: String, CaseIterable, Identifiable {
    case poppins = "POPPINS"
    case roboto = "ROBOTO"
    case monospace = "MONOSPACE"
    case condensed = "CONDENSED"
    case serif = "SERIF"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .poppins: return "Poppins"
        case .roboto: return "Roboto"
        case .monospace: return "Monospace"
        case .condensed: return "Condensed"
        case .serif: return "Serif"
        }
    }

    private static var fontCache: [String: NSFont] = [:]
    private static let fontCacheLock = NSLock()

    public func swiftUIFont(size: CGFloat, isBold: Bool, isItalic: Bool) -> Font {
        let nsFont = self.nsFont(size: size, isBold: isBold, isItalic: isItalic)
        return Font(nsFont)
    }

    public func nsFont(size: CGFloat, isBold: Bool, isItalic: Bool) -> NSFont {
        let cacheKey = "\(rawValue)_\(size)_\(isBold)_\(isItalic)"
        Self.fontCacheLock.lock()
        if let cached = Self.fontCache[cacheKey] {
            Self.fontCacheLock.unlock()
            return cached
        }
        Self.fontCacheLock.unlock()

        let weight: NSFont.Weight = isBold ? .bold : .regular
        var baseFont: NSFont

        switch self {
        case .poppins:
            let fontName = isBold ? "Poppins-Bold" : "Poppins-Regular"
            baseFont = NSFont(name: fontName, size: size)
                ?? NSFont(name: "Poppins", size: size)
                ?? NSFont.systemFont(ofSize: size, weight: weight)
        case .roboto:
            baseFont = NSFont(name: isBold ? "HelveticaNeue-Bold" : "HelveticaNeue", size: size)
                ?? NSFont.systemFont(ofSize: size, weight: weight)
        case .monospace:
            baseFont = NSFont.monospacedSystemFont(ofSize: size, weight: weight)
        case .condensed:
            baseFont = NSFont(name: isBold ? "HelveticaNeue-CondensedBold" : "HelveticaNeue-Condensed", size: size)
                ?? NSFont(name: isBold ? "ArialNarrow-Bold" : "ArialNarrow", size: size)
                ?? NSFont.systemFont(ofSize: size, weight: weight)
        case .serif:
            if let desc = NSFont.systemFont(ofSize: size, weight: weight).fontDescriptor.withDesign(.serif),
               let serifFont = NSFont(descriptor: desc, size: size) {
                baseFont = serifFont
            } else {
                baseFont = NSFont(name: isBold ? "TimesNewRomanPS-BoldMT" : "TimesNewRomanPSMT", size: size)
                    ?? NSFont.systemFont(ofSize: size, weight: weight)
            }
        }

        if isItalic {
            let manager = NSFontManager.shared
            baseFont = manager.convert(baseFont, toHaveTrait: .italicFontMask)
        }
        if isBold {
            let manager = NSFontManager.shared
            baseFont = manager.convert(baseFont, toHaveTrait: .boldFontMask)
        }

        Self.fontCacheLock.lock()
        Self.fontCache[cacheKey] = baseFont
        Self.fontCacheLock.unlock()
        return baseFont
    }

    public static func registerBundledFonts() {
        let fontFiles = ["poppins_regular", "poppins_bold"]
        for name in fontFiles {
            if let url = Bundle.module.url(forResource: name, withExtension: "ttf") {
                CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            } else if let mainUrl = Bundle.main.url(forResource: name, withExtension: "ttf") {
                CTFontManagerRegisterFontsForURL(mainUrl as CFURL, .process, nil)
            }
        }
    }
}

public struct PingPoint: Identifiable, Equatable {
    private static var counter: Int = 0
    private static let counterLock = NSLock()

    public let id: Int
    public let latencyMs: Float // -1 for drop, -2 for interruption marker
    public let isInterruption: Bool
    public let label: String

    public init(latencyMs: Float, isInterruption: Bool = false, label: String = "") {
        Self.counterLock.lock()
        Self.counter += 1
        let nextId = Self.counter
        Self.counterLock.unlock()
        self.id = nextId
        self.latencyMs = latencyMs
        self.isInterruption = isInterruption
        self.label = label
    }
}
