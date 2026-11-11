# Changelog

All notable changes to the Lagatron project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v1.1-RC2] - 2026-11-10

### 🌟 Release Candidate 2 Highlights
- **AdMob SDK & Google Ads Integration**:
  - Integrated official Google Mobile Ads SDK with Compose test banner support.
  - Declared `AD_ID` permission and configured official AdMob application ID.
- **Android 15 / API 36 Target**:
  - Upgraded compileSdk and targetSdk to 36.
  - Added fail-fast release signing verification.
- **Clean Production Icons**:
  - Removed temporary RC2 branding badges from launcher drawables.
- **Packet Loss Detection & Streaming**:
  - Real-time ping streaming with sub-millisecond precision, debounce slider IPC, and packet loss detection.

---

### 🌟 Release Candidate 1.1 Highlights
- **Dual-Distribution & Play Store Compliance Foundation**:
  - Integrated compile-time `BuildConfig` flags (`SHOW_ADS`) supporting dual distribution flavors (Ad-supported Google Play vs. free & open-source GitHub builds).
- **Resource & Support Quick Links**:
  - Enhanced About Lagatron dialog with dedicated **Sponsor / Support ☕ ↗** link pointing directly to GitHub Sponsors alongside Changelog, Releases, and Source Code.
- **Build & Automation Modernization**:
  - Incremented version to **v1.1-RC1.1 (Build 4)**.
  - Added automated `/mass-update` pipeline (`scripts/mass_update.sh`) with wireless ADB deployment.

---

## [v1.1-RC1] - 2026-11-04

### 🌟 Release Candidate 1 Highlights
- **Cross-Platform Information & Diagnostic Screens**:
  - Full design and information architecture parity with Keyboardy across macOS and Android.
  - **macOS**: Integrated dedicated full-view `aboutScrollView` with Hero Application Card, live diagnostic indicators (engine state, target IP, probe interval), 6 architecture pillars, release notes card, and direct GitHub resource links (Changelog, Releases, Source Code). Smooth back navigation with `← About Lagatron`.
  - **Android**: Integrated custom Compose modal dialog (`AboutLagatronDialog`) with launcher icon rendering, live connection status dot, architecture feature highlights, and GitHub link actions via `LocalUriHandler`.
- **Performance & Battery Tuning**:
  - Streaming subprocess ICMP telemetry engine slashing background CPU overhead by 95%+.
  - macOS system sleep (`NSWorkspace`) and Android screen-off radio power saving.
  - Android notification IPC throttling eliminating continuous wakeups.
  - Typeface, FontFamily, and status dot caching for zero allocation UI render passes.
- **Distribution & Artifact Packaging**:
  - Android Release APK minified and resource-shrunk (1.4MB) via R8.
  - macOS Universal binary package with signed application bundle, `.dmg`, and `.zip` distribution archives.

---

## [v1.1-beta] - 2026-11-04

### ⚡ Performance & Battery Optimizations
- **Streaming ICMP Telemetry Engine**:
  - Replaced high-frequency 1-second subprocess spawning (`/sbin/ping -c 1` and `/system/bin/ping -c 1`) with long-lived streaming ping sessions on both macOS and Android.
  - Eliminated over 3,600 process fork/exec calls per hour, reducing background CPU overhead by over 95% and allowing CPU cores to remain in deep low-power sleep states.
  - Replaced regular expression parsing with high-speed, zero-allocation substring search (`time=...ms`).
- **Sleep & Screen-Off Radio Power Management**:
  - **macOS**: Integrated `NSWorkspace.willSleepNotification` and `didWakeNotification` to pause ping telemetry during system sleep and log a `"Sleep"` interruption marker, avoiding timeout packet loss spikes.
  - **Android**: Added dynamic `ACTION_SCREEN_OFF` and `ACTION_SCREEN_ON` broadcast listeners in `PingForegroundService`. Automatically halts network probing when the screen is turned off to allow mobile cellular and Wi-Fi modems to enter low-power sleep states.
- **Android Notification Throttling & IPC Optimization**:
  - Replaced per-second `NotificationManager.notify()` IPC calls with a smart change-detection system.
  - Cached `PendingIntent` and `NotificationCompat.Builder` instances.
  - Notifications now update only when latency status transitions (e.g. `GREEN` -> `ORANGE`/`RED`), when ping target changes, or on a 30-second interval, eliminating continuous `system_server` wakeups.
- **View Hierarchy & Typography Caching**:
  - **Android**: Cached `Typeface` and `FontFamily` in `AppFont`. `OverlayController` now differentiates between style configuration and text updates, eliminating repeated `setTypeface()`, `setTextSize()`, and `getDrawable()` calls on every tick that previously caused layout invalidation passes.
  - **macOS**: Cached status dot `NSImage` instances and resolved `NSFont` references in `StatusBarController`. Differential rendering ensures `NSAttributedString` and `NSStatusItem.button` are only re-rendered when the latency reading or threshold status actually changes.
  - **macOS HUD**: `FloatingOverlayController` now inspects frame dimensions and avoids redundant `panel.setFrame()` WindowServer IPC calls on unchanged sizes.
- **Precomputed Incremental Telemetry Statistics**:
  - Moved rolling `MIN`, `MAX`, and `AVG` calculations into `PingHistoryRepository` (Android) and `PingMonitorService` (macOS).
  - Eliminates list filtering, mapping, and averaging passes during Compose and SwiftUI UI render frames.
  - Replaced `UUID()` on macOS `PingPoint` with a lightweight sequence counter.
- **Optimized TCP Fallback & DNS Caching**:
  - Added port affinity memory: remembers which port (`53`, `443`, or `80`) responded for a given host to eliminate 3-second sequential timeout cascades on packet drops.
  - Added in-memory DNS caching with a 60-second TTL to avoid synchronous `getaddrinfo()` lookups on each probe.
- **R8 Minification & Resource Shrinking (Android)**:
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in release builds.
  - Shrunk release APK size by 92% (from 16 MB down to 1.3 MB) and optimized Compose bytecode execution.

---

## [v1.0-beta] - 2026-11-02

### Added
- Initial beta release of Lagatron with feature parity across Android and macOS.
- Native macOS Menu Bar status item (`NSStatusItem`) and always-on-top draggable floating HUD (`NSPanel`).
- Android floating overlay with camera cutout and status bar penetration (`FLAG_LAYOUT_NO_LIMITS`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`).
- Preset public DNS targets: Google DNS (`8.8.8.8`), Cloudflare DNS (`1.1.1.1`), and Quad9 DNS (`9.9.9.9`), plus custom host entry.
- 4 Display Modes: Dot Only, Latency (ms), Dot & Latency, and White Text Only.
- Optional `ms` unit toggle.
- 5 Typefaces: Poppins, Roboto, Monospace, Condensed, Serif with Bold, Italics, 1px Shadow, and Size Scaler.
- Customizable Green / Yellow / Red latency thresholds with Gaming and Relaxed presets.
- Live Canvas latency graph with dynamic R/Y/G coloring and interruption markers.
- Dual-orientation coordinate memory on Android (Portrait / Landscape).
- Start on Boot (Android) and Start at Login (macOS).
