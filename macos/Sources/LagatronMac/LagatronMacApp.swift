import AppKit
import SwiftUI

@main
public struct LagatronMacApp {
    public static func main() {
        let app = NSApplication.shared
        let delegate = AppDelegate()
        app.delegate = delegate
        app.setActivationPolicy(.accessory)
        app.run()
    }
}

@MainActor
public final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusBarController: StatusBarController?
    private let prefs = AppPreferences.shared
    private let monitor = PingMonitorService.shared
    private let floatingOverlay = FloatingOverlayController.shared

    public func applicationDidFinishLaunching(_ notification: Notification) {
        AppFont.registerBundledFonts()

        let statusBar = StatusBarController()
        self.statusBarController = statusBar

        monitor.onPingUpdated = { [weak self] result in
            guard let self = self else { return }
            self.statusBarController?.updateStatusBar(with: result)
            self.floatingOverlay.updateVisibilityAndContent(result: result)
        }

        if prefs.isServiceRunning {
            monitor.start()
        } else {
            statusBar.updateStatusBar(with: nil)
            floatingOverlay.updateVisibilityAndContent(result: nil)
        }
    }

    public func applicationWillTerminate(_ notification: Notification) {
        floatingOverlay.hide()
    }
}
