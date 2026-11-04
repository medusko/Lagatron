import AppKit
import SwiftUI

@MainActor
public final class FloatingOverlayController {
    public static let shared = FloatingOverlayController()

    private var panel: NSPanel?
    private var hostingView: NSHostingView<FloatingOverlayBadgeView>?
    private let prefs = AppPreferences.shared

    private init() {}

    public func updateVisibilityAndContent(result: PingResult?) {
        if prefs.isServiceRunning && prefs.showFloatingOverlay {
            showOrUpdate(result: result)
        } else {
            hide()
        }
    }

    public func showOrUpdate(result: PingResult?) {
        let badgeView = FloatingOverlayBadgeView(
            result: result,
            mode: prefs.displayMode,
            font: prefs.appFont,
            isBold: prefs.isBold,
            isItalic: prefs.isItalic,
            hasShadow: prefs.hasTextShadow,
            showMsUnit: prefs.showMsUnit,
            fontSizeSp: CGFloat(prefs.fontSizeSp),
            greenThresholdMs: prefs.greenThresholdMs,
            orangeThresholdMs: prefs.orangeThresholdMs
        )

        if let panel = panel, let hostingView = hostingView {
            hostingView.rootView = badgeView
            let fittingSize = hostingView.fittingSize
            let targetWidth = max(36, fittingSize.width)
            let targetHeight = max(24, fittingSize.height)

            var frame = panel.frame
            if abs(frame.width - targetWidth) > 1 || abs(frame.height - targetHeight) > 1 {
                frame.size = CGSize(width: targetWidth, height: targetHeight)
                panel.setFrame(frame, display: true)
            }
            if !panel.isVisible {
                panel.orderFrontRegardless()
            }
            return
        }

        let hosting = DraggableHostingView(rootView: badgeView) { [weak self] newX, newY in
            guard let self = self else { return }
            self.prefs.overlayX = newX
            self.prefs.overlayY = newY
        }
        let size = hosting.fittingSize
        let contentRect = NSRect(x: 0, y: 0, width: max(48, size.width), height: max(26, size.height))

        let newPanel = NSPanel(
            contentRect: contentRect,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        newPanel.isOpaque = false
        newPanel.backgroundColor = .clear
        newPanel.hasShadow = false
        newPanel.level = .statusBar
        newPanel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        newPanel.isMovableByWindowBackground = false
        newPanel.contentView = hosting

        self.hostingView = hosting
        self.panel = newPanel

        applyPosition(x: prefs.overlayX, y: prefs.overlayY)
        newPanel.orderFrontRegardless()
    }

    public func applyPosition(x: Int, y: Int) {
        guard let panel = panel else { return }
        let screenFrame = NSScreen.main?.frame ?? NSRect(x: 0, y: 0, width: 1440, height: 900)
        // Convert top-left (x, y) to AppKit bottom-left origin
        let appKitX = screenFrame.minX + CGFloat(x)
        let appKitY = screenFrame.maxY - CGFloat(y) - panel.frame.height
        panel.setFrameOrigin(NSPoint(x: appKitX, y: appKitY))
    }

    public func hide() {
        panel?.orderOut(nil)
    }
}

private final class DraggableHostingView<Content: View>: NSHostingView<Content> {
    private var initialWindowOrigin: NSPoint = .zero
    private var initialMouseLocation: NSPoint = .zero
    private let onPositionSaved: (Int, Int) -> Void

    init(rootView: Content, onPositionSaved: @escaping (Int, Int) -> Void) {
        self.onPositionSaved = onPositionSaved
        super.init(rootView: rootView)
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @MainActor required init(rootView: Content) {
        self.onPositionSaved = { _, _ in }
        super.init(rootView: rootView)
    }

    override var mouseDownCanMoveWindow: Bool { false }

    override func mouseDown(with event: NSEvent) {
        guard let window = self.window else { return }
        initialWindowOrigin = window.frame.origin
        initialMouseLocation = NSEvent.mouseLocation
    }

    override func mouseDragged(with event: NSEvent) {
        guard let window = self.window else { return }
        let currentMouse = NSEvent.mouseLocation
        let dx = currentMouse.x - initialMouseLocation.x
        let dy = currentMouse.y - initialMouseLocation.y
        let newOrigin = NSPoint(x: initialWindowOrigin.x + dx, y: initialWindowOrigin.y + dy)
        window.setFrameOrigin(newOrigin)
    }

    override func mouseUp(with event: NSEvent) {
        guard let window = self.window else { return }
        let screenFrame = window.screen?.frame ?? NSScreen.main?.frame ?? NSRect(x: 0, y: 0, width: 1440, height: 900)
        let topLeftX = Int(round(window.frame.minX - screenFrame.minX))
        let topLeftY = Int(round(screenFrame.maxY - window.frame.maxY))
        onPositionSaved(max(0, topLeftX), max(0, topLeftY))
    }
}

public struct FloatingOverlayBadgeView: View {
    public let result: PingResult?
    public let mode: DisplayMode
    public let font: AppFont
    public let isBold: Bool
    public let isItalic: Bool
    public let hasShadow: Bool
    public let showMsUnit: Bool
    public let fontSizeSp: CGFloat
    public let greenThresholdMs: Int
    public let orangeThresholdMs: Int

    public var body: some View {
        let isMonochrome = (mode == .monochromeText)
        let status = result?.statusForThresholds(
            greenThresholdMs: greenThresholdMs,
            orangeThresholdMs: orangeThresholdMs
        ) ?? .green
        let statusColor: Color = {
            if isMonochrome { return .white }
            switch status {
            case .green: return LagatronColors.overlayGreen
            case .orange: return LagatronColors.pingOrange
            case .red: return LagatronColors.pingRed
            }
        }()
        let fallbackText = showMsUnit ? "-- ms" : "--"
        let textValue = result?.formattedLatency(showMs: showMsUnit) ?? fallbackText

        HStack(spacing: mode == .both ? 5 : 0) {
            if mode == .dotOnly || mode == .both {
                Circle()
                    .fill(statusColor)
                    .frame(width: 9, height: 9)
            }
            if mode != .dotOnly {
                Text(textValue)
                    .font(font.swiftUIFont(size: fontSizeSp, isBold: isBold, isItalic: isItalic))
                    .foregroundColor(statusColor)
                    .shadow(
                        color: hasShadow ? Color.black.opacity(0.85) : .clear,
                        radius: hasShadow ? 1.5 : 0,
                        x: hasShadow ? -1.5 : 0,
                        y: hasShadow ? 1.5 : 0
                    )
                    .fixedSize()
            }
        }
        .padding(.horizontal, isMonochrome ? 6 : 9)
        .padding(.vertical, 4)
        .background(
            Group {
                if !isMonochrome {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color(hex: 0x121212, alpha: 0.82))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(Color.white.opacity(0.22), lineWidth: 1)
                        )
                }
            }
        )
        .fixedSize()
    }
}
