# Lagatron 🚀

Lagatron is a lightweight Android utility app that displays a real-time, draggable floating overlay showing ping latency to selected network targets.

## Features

- **Preset Servers**: 
  - Google DNS (`8.8.8.8`)
  - Cloudflare DNS (`1.1.1.1`)
  - Quad9 DNS (`9.9.9.9`)
- **Custom Targets**: Enter any custom IP address or domain name (e.g., `192.168.1.1`, `github.com`).
- **Customizable Display Styles**:
  - **Dot Only**: Compact colored indicator.
  - **Latency Only**: Numeric ms latency value formatted and colored by threshold.
  - **Both**: Dot indicator alongside the numeric ms value.
- **Latency Thresholds**:
  - 🟢 **Green**: `< 10ms` (Fast / Low latency)
  - 🟠 **Orange**: `10ms – 100ms` (Normal)
  - 🔴 **Red**: `> 100ms` or Timeout / Unreachable
- **Draggable Anywhere (Including Status Bar)**:
  - Supports dragging directly over status bar and camera cutout areas (`FLAG_LAYOUT_NO_LIMITS`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`).
  - Saved coordinates: remembers last position automatically across app launches.
- **Persistent Notification with Quick Actions**:
  - Shows real-time latency in the notification shade.
  - Dedicated "Stop" button in notification for 1-tap dismissal.
- **Modern Architecture**:
  - 100% Kotlin + Jetpack Compose UI.
  - Native ICMP process ping with automatic TCP fallback for networks that restrict ICMP packets.
  - Foreground Service compliant with Android 14+ / 15 (`FOREGROUND_SERVICE_SPECIAL_USE`).

## Building & Installing

### Compile Debug APK
```bash
./gradlew assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install to Connected Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## macOS Menu Bar & HUD Edition 🍎

Lagatron also includes a native macOS app (`macos/`) built with Swift, SwiftUI, and AppKit that displays live ping latency directly in the macOS Menu Bar (Status Bar) and optionally as an always-on-top draggable floating HUD overlay.

### Build & Run `Lagatron.app` (macOS 13+)
```bash
./macos/build-app.sh
open macos/build/Lagatron.app
```
To install into `/Applications`:
```bash
cp -R macos/build/Lagatron.app /Applications/
```
