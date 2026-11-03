import SwiftUI
import AppKit

public enum LagatronColors {
    // Accents
    public static let accentBlue = Color(hex: 0x2E6BD9)
    public static let pingGreen = Color(hex: 0x00C853)
    public static let overlayGreen = Color(hex: 0x00E676)
    public static let pingOrange = Color(hex: 0xFF9100)
    public static let pingRed = Color(hex: 0xFF1744)
    public static let interruptGray = Color(hex: 0x7E869B)

    // Dark Palette
    public static let darkBackground = Color(hex: 0x101216)
    public static let darkSurface = Color(hex: 0x191C23)
    public static let darkSurfaceVariant = Color(hex: 0x222631)
    public static let darkBorder = Color(hex: 0x2E3342)
    public static let darkTextPrimary = Color(hex: 0xF1F3F8)
    public static let darkTextSecondary = Color(hex: 0x9EA3B2)

    // Light Palette (Warm Cream / Linen aesthetic)
    public static let lightBackground = Color(hex: 0xF6F1E8)
    public static let lightSurface = Color(hex: 0xFFFDF9)
    public static let lightSurfaceVariant = Color(hex: 0xECE5D9)
    public static let lightBorder = Color(hex: 0xDCD4C6)
    public static let lightTextPrimary = Color(hex: 0x1E1A16)
    public static let lightTextSecondary = Color(hex: 0x6B6358)
}

public struct ThemePalette {
    public let isDark: Bool

    public var primary: Color { LagatronColors.accentBlue }
    public var background: Color { isDark ? LagatronColors.darkBackground : LagatronColors.lightBackground }
    public var surface: Color { isDark ? LagatronColors.darkSurface : LagatronColors.lightSurface }
    public var surfaceVariant: Color { isDark ? LagatronColors.darkSurfaceVariant : LagatronColors.lightSurfaceVariant }
    public var outline: Color { isDark ? LagatronColors.darkBorder : LagatronColors.lightBorder }
    public var onSurface: Color { isDark ? LagatronColors.darkTextPrimary : LagatronColors.lightTextPrimary }
    public var onSurfaceVariant: Color { isDark ? LagatronColors.darkTextSecondary : LagatronColors.lightTextSecondary }
}

extension Color {
    public init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}

extension NSColor {
    public convenience init(hex: UInt32, alpha: CGFloat = 1.0) {
        let r = CGFloat((hex >> 16) & 0xFF) / 255.0
        let g = CGFloat((hex >> 8) & 0xFF) / 255.0
        let b = CGFloat(hex & 0xFF) / 255.0
        self.init(srgbRed: r, green: g, blue: b, alpha: alpha)
    }
}
