# Lagatron 🚀

Lagatron is an ultra-lightweight, real-time network latency monitor for **Android** and **macOS** that displays live ping telemetry directly in your **status bar / menu bar** or as an always-on-top **draggable floating HUD overlay**.

---

## 📥 Downloads & Installation

### 🍺 Install via Homebrew (macOS)

The fastest and easiest way to install and keep Lagatron updated on macOS:

```bash
brew install --cask medusko/tap/lagatron
```

*Or add the tap first:*
```bash
brew tap medusko/tap
brew install --cask lagatron
```

---

### 📦 Pre-built Binaries (GitHub Releases)

Grab the latest pre-built binaries from the **[GitHub Releases page](https://github.com/medusko/Lagatron/releases)**:

| Platform | Download | Requirements | Size |
| :--- | :--- | :--- | :--- |
| 📱 **Android** | [`Lagatron-v1.1-RC1.1.apk`](https://github.com/medusko/Lagatron/releases/download/v1.1-RC1.1/Lagatron-v1.1-RC1.1.apk) | Android 8.0+ (API 26 – Android 15+) | **1.4 MB** |
| 🍎 **macOS (DMG)** | [`Lagatron-macOS-v1.1-RC1.1.dmg`](https://github.com/medusko/Lagatron/releases/download/v1.1-RC1.1/Lagatron-macOS-v1.1-RC1.1.dmg) | macOS 13.0+ (Ventura / Sonoma / Sequoia+) | **1.7 MB** |
| 🍎 **macOS (ZIP)** | [`Lagatron-macOS-v1.1-RC1.1.zip`](https://github.com/medusko/Lagatron/releases/download/v1.1-RC1.1/Lagatron-macOS-v1.1-RC1.1.zip) | macOS 13.0+ (`Lagatron.app` bundle) | **1.3 MB** |

---

## ✨ Features (Android & macOS Parity)

- **⚡ Ultra-Low Battery & CPU Overhead**:
  - **Streaming Subprocess Engine**: Replaced per-second subprocess spawning with long-running streaming ping sessions, cutting background CPU usage by >95%.
  - **Screen & Sleep Aware**: Automatically suspends probing during macOS system sleep and Android screen-off states to let cellular and Wi-Fi modems stay in low-power idle.
  - **Differential UI Rendering**: View hierarchy and typography caching avoids layout invalidations on every tick.
  - **R8 Optimized**: 92% smaller APK size and faster startup.
- **Preset & Custom Ping Targets**:
  - Built-in public DNS presets: **Google DNS** (`8.8.8.8`), **Cloudflare DNS** (`1.1.1.1`), and **Quad9 DNS** (`9.9.9.9`).
  - **Custom Host**: Enter any IP address or domain name (e.g., `192.168.1.1`, `github.com`).
- **4 Customizable Display Modes**:
  - **Dot Only**: Minimalist colored status indicator dot.
  - **Latency (ms)**: Numeric latency reading colored by status threshold.
  - **Dot & Latency**: Colored status dot alongside the numeric latency reading.
  - **White Text Only**: Clean, borderless monochrome white text without a pill container or color coding.
- **Typography & Unit Controls**:
  - **5 Typefaces**: `Poppins` (bundled), `Roboto`, `Monospace`, `Condensed`, and `Serif`.
  - **Styling Toggles**: `Bold`, `Italics`, `Shadow (1px)`, and a **`Show "ms"`** toggle to show or hide the `ms` unit suffix for an ultra-compact readout.
  - **Font Size Scaler**: Adjustable from `9.0 sp` to `18.0 sp` with a live 3-stage overlay preview.
- **Customizable G / Y / R Latency Thresholds**:
  - **Quick Presets**:
    - **Default**: 🟢 Green `< 10 ms` · 🟠 Yellow `10–100 ms` · 🔴 Red `> 100 ms`
    - **Gaming**: 🟢 Green `< 30 ms` · 🟠 Yellow `30–80 ms` · 🔴 Red `> 80 ms`
    - **Relaxed**: 🟢 Green `< 50 ms` · 🟠 Yellow `50–150 ms` · 🔴 Red `> 150 ms`
  - **Custom Cutoff Sliders**: Independently tune your exact Green (`5–150 ms`) and Yellow/Orange (`15–300 ms`) millisecond cutoffs.
- **Live Rolling Latency Graph**:
  - Plots the last 50 ping samples with incrementally computed `MIN`, `AVG`, `MAX`, and `SAMPLES` statistics, dynamic R/Y/G line & gradient coloring, and dashed interruption markers when targets change or monitoring restarts.
- **ICMP Ping with Automatic TCP Fallback**:
  - Native system ICMP `ping` with port affinity and DNS caching, automatically falling back to TCP handshake latency measurement (`53`, `443`, `80`) on networks that block ICMP packets.
- **Adaptive Themes & Auto-Start**:
  - Switch between **Dark Mode** (`#101216`) and **Warm Cream Light Mode** (`#F6F1E8`).
  - **Start on Boot** (Android) / **Start at Login** (macOS).

---

## 🍎 macOS Menu Bar & HUD Edition (`macos/`)

Built natively with **Swift, SwiftUI, and AppKit** for ultra-low memory and CPU overhead:
- **Native macOS Menu Bar Item (`NSStatusItem`)**: Displays your live ping directly in the macOS system status bar using your chosen display mode, font family, size, shadow, and G/Y/R color thresholds.
  - **Left-click** the menu bar item to open the Lagatron dashboard popover.
  - **Right-click** the menu bar item for quick actions (`Start/Stop Monitor`, `Show/Hide On-Screen Floating HUD`, `Open Settings`, `Quit`).
- **Optional Draggable Floating HUD (`NSPanel`)**: Enable an always-on-top, non-activating floating pill (`NSWindow.Level.statusBar` + `.fullScreenAuxiliary`) that stays visible even over full-screen games and apps. Drag it anywhere on screen or fine-tune its coordinates with the D-Pad nudger and placement presets.

### 🍏 Installing & Building on macOS

#### Option 1: Homebrew (Recommended)
```bash
brew install --cask medusko/tap/lagatron
```

#### Option 2: Pre-built DMG / ZIP
Download `Lagatron-macOS-v1.1-RC1.1.dmg` from [GitHub Releases](https://github.com/medusko/Lagatron/releases), mount the DMG, and drag `Lagatron.app` into `/Applications`.

#### Option 3: Build from Source
```bash
./macos/build-app.sh
cp -R macos/build/Lagatron.app /Applications/
open /Applications/Lagatron.app
```

---

## 📱 Android Edition (`app/`)

Built with **100% Kotlin & Jetpack Compose**:
- **Status Bar & Cutout Overlay**: Supports dragging directly over the Android status bar and camera cutout areas (`FLAG_LAYOUT_NO_LIMITS`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`).
- **Dual Orientation Memory**: Remembers independent `(X, Y)` screen coordinates for **Portrait** and **Landscape** orientations and automatically snaps when you rotate your device.
- **Persistent Notification**: Throttled foreground service (`FOREGROUND_SERVICE_SPECIAL_USE`, Android 14/15 compliant) with live latency readout and a 1-tap **Stop** action.

### Building & Installing on Android
```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```
