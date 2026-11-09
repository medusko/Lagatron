import AppKit
import SwiftUI

@MainActor
public final class StatusBarController: NSObject {
    private var statusItem: NSStatusItem
    private var popover: NSPopover
    private let prefs = AppPreferences.shared
    private let monitor = PingMonitorService.shared

    // Cached drawing artifacts to avoid allocations on every tick
    private var dotImages: [UInt64: NSImage] = [:]
    private var lastRenderedText: String = ""
    private var lastRenderedColorHex: UInt32 = 0
    private var lastRenderedMode: DisplayMode? = nil
    private var lastRenderedFont: AppFont? = nil
    private var lastRenderedFontSize: CGFloat = 0
    private var lastRenderedBold: Bool = false
    private var lastRenderedItalic: Bool = false
    private var lastRenderedShadow: Bool = false
    private var lastRenderedRunning: Bool = false

    public override init() {
        self.statusItem = NSStatusBar.system.statusItem(withLength: 66)
        self.popover = NSPopover()
        super.init()

        configurePopover()
        configureStatusButton()
        updateStatusBar(with: monitor.latestPing)
    }

    private func configurePopover() {
        popover.contentSize = NSSize(width: 430, height: 740)
        popover.behavior = .transient
        popover.animates = true
        popover.contentViewController = NSHostingController(rootView: MainScreen())
    }

    private func configureStatusButton() {
        guard let button = statusItem.button else { return }
        button.target = self
        button.action = #selector(handleStatusBarClick(_:))
        button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        button.alignment = .center
    }

    @objc private func handleStatusBarClick(_ sender: NSStatusBarButton) {
        guard let event = NSApp.currentEvent else {
            togglePopover(sender)
            return
        }

        if event.type == .rightMouseUp || event.modifierFlags.contains(.control) {
            showContextMenu(for: sender)
        } else {
            togglePopover(sender)
        }
    }

    public func togglePopover(_ sender: AnyObject?) {
        if popover.isShown {
            popover.performClose(sender)
        } else if let button = statusItem.button {
            NSApp.activate(ignoringOtherApps: true)
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            popover.contentViewController?.view.window?.makeKey()
        }
    }

    private func showContextMenu(for button: NSStatusBarButton) {
        let menu = NSMenu()

        let statusTitle = prefs.isServiceRunning
            ? "Active (\(prefs.getActiveHost()))"
            : "Stopped"
        let headerItem = NSMenuItem(title: "Lagatron • \(statusTitle)", action: nil, keyEquivalent: "")
        headerItem.isEnabled = false
        menu.addItem(headerItem)
        menu.addItem(.separator())

        let toggleTitle = prefs.isServiceRunning ? "Stop Ping Monitor" : "Start Ping Monitor"
        let toggleItem = NSMenuItem(title: toggleTitle, action: #selector(toggleServiceFromMenu), keyEquivalent: "s")
        toggleItem.target = self
        menu.addItem(toggleItem)

        let hudTitle = prefs.showFloatingOverlay ? "Hide On-Screen Floating HUD" : "Show On-Screen Floating HUD"
        let hudItem = NSMenuItem(title: hudTitle, action: #selector(toggleFloatingHUDFromMenu), keyEquivalent: "f")
        hudItem.target = self
        menu.addItem(hudItem)

        let openItem = NSMenuItem(title: "Open Lagatron Settings...", action: #selector(openPopoverFromMenu), keyEquivalent: ",")
        openItem.target = self
        menu.addItem(openItem)

        menu.addItem(.separator())

        let quitItem = NSMenuItem(title: "Quit Lagatron", action: #selector(quitApp), keyEquivalent: "q")
        quitItem.target = self
        menu.addItem(quitItem)

        statusItem.menu = menu
        button.performClick(nil)
        statusItem.menu = nil
    }

    @objc private func toggleServiceFromMenu() {
        monitor.toggle()
    }

    @objc private func toggleFloatingHUDFromMenu() {
        prefs.showFloatingOverlay.toggle()
        FloatingOverlayController.shared.updateVisibilityAndContent(result: monitor.latestPing)
    }

    @objc private func openPopoverFromMenu() {
        if !popover.isShown {
            togglePopover(statusItem.button)
        }
    }

    @objc private func quitApp() {
        NSApp.terminate(nil)
    }

    public func updateStatusBar(with result: PingResult?) {
        guard let button = statusItem.button else { return }

        let mode = prefs.displayMode
        let targetLength: CGFloat = {
            if !prefs.isServiceRunning {
                return 58
            }
            switch mode {
            case .dotOnly:
                return 28
            case .msOnly, .monochromeText:
                return 58
            case .both:
                return 66
            }
        }()
        if statusItem.length != targetLength {
            statusItem.length = targetLength
        }

        if !prefs.isServiceRunning {
            if lastRenderedRunning == false && lastRenderedText == "OFF" {
                return
            }
            lastRenderedRunning = false
            lastRenderedText = "OFF"
            lastRenderedMode = nil

            button.image = cachedDotImage(color: NSColor.systemGray, colorKey: 0x808080, diameter: 8)
            button.imagePosition = .imageLeading
            let font = NSFont.monospacedSystemFont(ofSize: 11.5, weight: .semibold)
            let attrs: [NSAttributedString.Key: Any] = [
                .font: font,
                .foregroundColor: NSColor.secondaryLabelColor,
                .baselineOffset: -0.5
            ]
            button.attributedTitle = NSAttributedString(string: " OFF", attributes: attrs)
            return
        }

        let isMonochrome = (mode == .monochromeText)
        let status = result?.statusForThresholds(
            greenThresholdMs: prefs.greenThresholdMs,
            orangeThresholdMs: prefs.orangeThresholdMs
        ) ?? .green
        let statusColorHex = isMonochrome ? 0xFFFFFF : status.colorHex
        let statusColor: NSColor = isMonochrome ? NSColor.labelColor : status.nsColor
        let fallbackText = prefs.showMsUnit ? "... ms" : "..."
        let latencyText = result?.formattedLatency(showMs: prefs.showMsUnit) ?? fallbackText

        // Clamp menu bar font size so it always fits the macOS menu bar cleanly
        let clampedSize = CGFloat(min(max(prefs.fontSizeSp, 9.0), 16.0))
        let currentFont = prefs.appFont
        let currentBold = prefs.isBold
        let currentItalic = prefs.isItalic
        let currentShadow = prefs.hasTextShadow

        // Fast-path: avoid re-rendering attributed string if nothing visual changed
        if lastRenderedRunning == true &&
           lastRenderedText == latencyText &&
           lastRenderedColorHex == statusColorHex &&
           lastRenderedMode == mode &&
           lastRenderedFont == currentFont &&
           lastRenderedFontSize == clampedSize &&
           lastRenderedBold == currentBold &&
           lastRenderedItalic == currentItalic &&
           lastRenderedShadow == currentShadow {
            return
        }

        lastRenderedRunning = true
        lastRenderedText = latencyText
        lastRenderedColorHex = statusColorHex
        lastRenderedMode = mode
        lastRenderedFont = currentFont
        lastRenderedFontSize = clampedSize
        lastRenderedBold = currentBold
        lastRenderedItalic = currentItalic
        lastRenderedShadow = currentShadow

        let nsFont = currentFont.nsFont(size: clampedSize, isBold: currentBold, isItalic: currentItalic)

        var attributes: [NSAttributedString.Key: Any] = [
            .font: nsFont,
            .foregroundColor: statusColor,
            .baselineOffset: -0.5
        ]

        if prefs.hasTextShadow {
            let shadow = NSShadow()
            shadow.shadowColor = NSColor.black.withAlphaComponent(0.85)
            shadow.shadowOffset = NSSize(width: -1.2, height: -1.2)
            shadow.shadowBlurRadius = 1.5
            attributes[.shadow] = shadow
        }

        switch mode {
        case .dotOnly:
            button.image = cachedDotImage(color: status.nsColor, colorKey: status.colorHex, diameter: 10)
            button.imagePosition = .imageOnly
            button.attributedTitle = NSAttributedString(string: "")

        case .msOnly:
            button.image = nil
            button.imagePosition = .noImage
            button.attributedTitle = NSAttributedString(string: latencyText, attributes: attributes)

        case .both:
            button.image = cachedDotImage(color: status.nsColor, colorKey: status.colorHex, diameter: 9)
            button.imagePosition = .imageLeading
            button.attributedTitle = NSAttributedString(string: " " + latencyText, attributes: attributes)

        case .monochromeText:
            button.image = nil
            button.imagePosition = .noImage
            button.attributedTitle = NSAttributedString(string: latencyText, attributes: attributes)
        }
    }

    private func cachedDotImage(color: NSColor, colorKey: UInt32, diameter: CGFloat) -> NSImage {
        let key = (UInt64(colorKey) << 16) | UInt64(UInt16(diameter))
        if let cached = dotImages[key] {
            return cached
        }

        let size = NSSize(width: diameter + 2, height: diameter + 2)
        let image = NSImage(size: size, flipped: false) { rect in
            let circleRect = NSRect(x: 1, y: 1, width: diameter, height: diameter)
            let path = NSBezierPath(ovalIn: circleRect)
            color.setFill()
            path.fill()
            return true
        }
        image.isTemplate = false
        dotImages[key] = image
        return image
    }
}
