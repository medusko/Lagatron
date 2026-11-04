import SwiftUI
import AppKit

public struct MainScreen: View {
    @ObservedObject private var prefs = AppPreferences.shared
    @ObservedObject private var monitor = PingMonitorService.shared

    public init() {}

    private var theme: ThemePalette {
        ThemePalette(isDark: prefs.isDarkMode)
    }

    public var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                topBar

                ScrollView(.vertical, showsIndicators: true) {
                    VStack(spacing: 14) {
                        targetSelectorCard
                        pingGraphCard
                        displayModeSelectorCard
                        overlayPreviewCard
                        floatingOverlayPositionCard
                        startOnBootCard
                        infoFooterCard
                    }
                    .padding(14)
                }

                bottomActionBar
            }

            if prefs.showAboutDialog {
                aboutDialogOverlay
            }
        }
        .frame(minWidth: 420, idealWidth: 430, minHeight: 680, idealHeight: 740)
        .preferredColorScheme(prefs.isDarkMode ? .dark : .light)
    }

    // MARK: - Top App Bar
    private var topBar: some View {
        HStack(spacing: 10) {
            Text("Lagatron")
                .font(.system(size: 20, weight: .black))
                .foregroundColor(theme.onSurface)

            let isRunning = prefs.isServiceRunning
            let badgeColor = isRunning ? LagatronColors.pingGreen : LagatronColors.pingRed

            HStack(spacing: 5) {
                Circle()
                    .fill(badgeColor)
                    .frame(width: 7, height: 7)
                Text(isRunning ? "ACTIVE" : "STOPPED")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(badgeColor)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(badgeColor.opacity(0.15))
            .overlay(
                Capsule().stroke(badgeColor, lineWidth: 1)
            )
            .clipShape(Capsule())

            Spacer()

            Button {
                prefs.showAboutDialog = true
            } label: {
                Image(systemName: "info.circle.fill")
                    .font(.system(size: 16))
                    .foregroundColor(theme.primary)
            }
            .buttonStyle(.plain)
            .help("About Lagatron")

            Button {
                prefs.isDarkMode.toggle()
            } label: {
                Image(systemName: prefs.isDarkMode ? "sun.max.fill" : "moon.fill")
                    .font(.system(size: 16))
                    .foregroundColor(theme.primary)
            }
            .buttonStyle(.plain)
            .help("Toggle Light/Dark Theme")

            Button {
                NSApp.terminate(nil)
            } label: {
                Image(systemName: "power")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(theme.onSurfaceVariant)
            }
            .buttonStyle(.plain)
            .help("Quit Lagatron")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(theme.surface)
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(theme.outline),
            alignment: .bottom
        )
    }

    // MARK: - Target Selector Card
    private var targetSelectorCard: some View {
        CardContainer(theme: theme) {
            VStack(alignment: .leading, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Ping Target")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text("Select a public DNS server or specify your own IP / web address")
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 135), spacing: 8)], spacing: 8) {
                    ForEach(PresetServer.allCases) { preset in
                        let isSelected = (prefs.selectedPreset == preset)
                        ChipButton(
                            title: preset.chipLabel,
                            isSelected: isSelected,
                            showCheckmark: true,
                            theme: theme
                        ) {
                            prefs.selectedPreset = preset
                            monitor.notifyConfigChanged()
                        }
                    }
                }

                if prefs.selectedPreset == .custom {
                    HStack(spacing: 8) {
                        Image(systemName: "network")
                            .foregroundColor(theme.primary)
                        TextField("e.g. 192.168.1.1 or github.com", text: $prefs.customHost)
                            .textFieldStyle(.plain)
                            .font(.system(size: 13, design: .monospaced))
                            .foregroundColor(theme.onSurface)
                            .onChange(of: prefs.customHost) { _ in
                                monitor.notifyConfigChanged()
                            }
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(theme.surfaceVariant)
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(theme.primary.opacity(0.7), lineWidth: 1)
                    )
                }
            }
        }
    }

    // MARK: - Live Latency Graph Card
    private var pingGraphCard: some View {
        let isRunning = prefs.isServiceRunning
        let latest = monitor.latestPing
        let currentStatus = latest?.statusForThresholds(
            greenThresholdMs: prefs.greenThresholdMs,
            orangeThresholdMs: prefs.orangeThresholdMs
        )
        let history = monitor.history
        let minPing = monitor.minPing
        let maxPing = monitor.maxPing
        let avgPing = monitor.avgPing
        let samplesCount = monitor.validSamplesCount

        return CardContainer(theme: theme) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Live Latency Graph")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(theme.onSurface)
                        Text(isRunning ? "Rolling latency (Green <\(prefs.greenThresholdMs)ms, Yellow ≤\(prefs.orangeThresholdMs)ms, Red >\(prefs.orangeThresholdMs)ms)" : "Inactive - start monitor to record")
                            .font(.system(size: 11.5))
                            .foregroundColor(theme.onSurfaceVariant)
                    }

                    Spacer()

                    if isRunning, let latest = latest, let currentStatus = currentStatus {
                        let statusColor = currentStatus.swiftUIColor
                        Text(latest.formattedLatency(showMs: prefs.showMsUnit))
                            .font(.system(size: 13, weight: .heavy, design: .monospaced))
                            .foregroundColor(statusColor)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(statusColor.opacity(0.15))
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(statusColor.opacity(0.5), lineWidth: 1)
                            )
                            .cornerRadius(8)
                    }
                }

                // Stats summary row
                HStack {
                    Spacer()
                    StatItemView(label: "MIN", value: samplesCount == 0 ? "--" : String(format: "%.0f ms", minPing), theme: theme)
                    Spacer()
                    StatItemView(label: "AVG", value: samplesCount == 0 ? "--" : String(format: "%.1f ms", avgPing), theme: theme)
                    Spacer()
                    StatItemView(label: "MAX", value: samplesCount == 0 ? "--" : String(format: "%.0f ms", maxPing), theme: theme)
                    Spacer()
                    StatItemView(label: "SAMPLES", value: "\(samplesCount)", theme: theme)
                    Spacer()
                }

                // Canvas Graph
                let activeColor = currentStatus?.swiftUIColor ?? theme.primary
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(theme.surfaceVariant.opacity(0.35))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(theme.outline.opacity(0.4), lineWidth: 1)
                        )

                    if history.isEmpty {
                        Text(isRunning ? "Waiting for ping packets..." : "Start monitor to begin plotting")
                            .font(.system(size: 12))
                            .foregroundColor(theme.onSurfaceVariant)
                    } else {
                        PingGraphCanvas(
                            history: history,
                            maxPing: maxPing,
                            activeColor: activeColor,
                            gridColor: theme.outline.opacity(0.25)
                        )
                        .padding(.horizontal, 8)
                        .padding(.vertical, 12)
                    }
                }
                .frame(height: 130)
            }
        }
    }

    // MARK: - Display Mode & Typography Card
    private var displayModeSelectorCard: some View {
        CardContainer(theme: theme) {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Display Style")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text("Choose how ping appears in the macOS Menu Bar & Floating HUD")
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }

                VStack(spacing: 6) {
                    ForEach(DisplayMode.allCases) { mode in
                        let isSelected = (prefs.displayMode == mode)
                        Button {
                            prefs.displayMode = mode
                            monitor.notifyConfigChanged()
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                                    .font(.system(size: 15))
                                    .foregroundColor(isSelected ? theme.primary : theme.onSurfaceVariant)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(mode.displayName)
                                        .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(theme.onSurface)
                                    Text(mode.description)
                                        .font(.system(size: 11.5))
                                        .foregroundColor(theme.onSurfaceVariant)
                                }
                                Spacer()
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(isSelected ? theme.surfaceVariant : theme.surface)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(isSelected ? theme.primary : theme.outline.opacity(0.5), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }

                Divider()
                    .background(theme.outline.opacity(0.4))

                VStack(alignment: .leading, spacing: 2) {
                    Text("Overlay Typography & Styling")
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text("Typeface, weight, and size for status bar & overlay readings")
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }

                // Font family chips
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 95), spacing: 8)], spacing: 8) {
                    ForEach(AppFont.allCases) { font in
                        let isSelected = (prefs.appFont == font)
                        ChipButton(
                            title: font.title,
                            isSelected: isSelected,
                            showCheckmark: true,
                            customFont: font.swiftUIFont(size: 12.5, isBold: isSelected, isItalic: false),
                            theme: theme
                        ) {
                            prefs.appFont = font
                            monitor.notifyConfigChanged()
                        }
                    }
                }

                // Bold, Italics, Shadow & Show "ms" toggles
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                    StyleToggleChip(
                        title: "Bold",
                        icon: "bold",
                        isSelected: prefs.isBold,
                        theme: theme
                    ) {
                        prefs.isBold.toggle()
                        monitor.notifyConfigChanged()
                    }

                    StyleToggleChip(
                        title: "Italics",
                        icon: "italic",
                        isSelected: prefs.isItalic,
                        theme: theme
                    ) {
                        prefs.isItalic.toggle()
                        monitor.notifyConfigChanged()
                    }

                    StyleToggleChip(
                        title: "Shadow (1px)",
                        icon: "shadow",
                        isSelected: prefs.hasTextShadow,
                        theme: theme
                    ) {
                        prefs.hasTextShadow.toggle()
                        monitor.notifyConfigChanged()
                    }

                    StyleToggleChip(
                        title: "Show \"ms\"",
                        icon: prefs.showMsUnit ? "checkmark.circle.fill" : "circle",
                        isSelected: prefs.showMsUnit,
                        theme: theme
                    ) {
                        prefs.showMsUnit.toggle()
                        monitor.notifyConfigChanged()
                    }
                }

                // Font Size Slider
                VStack(spacing: 6) {
                    HStack {
                        Text("Font Size")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(theme.onSurface)
                        Spacer()
                        Text(String(format: "%.1f sp", prefs.fontSizeSp))
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundColor(theme.primary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(theme.surfaceVariant)
                            .cornerRadius(6)
                    }

                    Slider(
                        value: Binding(
                            get: { Double(prefs.fontSizeSp) },
                            set: { newVal in
                                prefs.fontSizeSp = Float((newVal * 2).rounded() / 2)
                                monitor.notifyConfigChanged()
                            }
                        ),
                        in: 9.0...18.0,
                        step: 0.5
                    )
                    .tint(theme.primary)
                }

                Divider()
                    .background(theme.outline.opacity(0.4))

                // Latency Color Thresholds (G / Y / R)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Latency Color Thresholds (G / Y / R)")
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text("Customize millisecond cutoffs for Green, Yellow, and Red status")
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }

                HStack(spacing: 6) {
                    PresetPositionButton(title: "Default (10/100)", theme: theme) {
                        prefs.greenThresholdMs = 10
                        prefs.orangeThresholdMs = 100
                        monitor.notifyConfigChanged()
                    }
                    PresetPositionButton(title: "Gaming (30/80)", theme: theme) {
                        prefs.greenThresholdMs = 30
                        prefs.orangeThresholdMs = 80
                        monitor.notifyConfigChanged()
                    }
                    PresetPositionButton(title: "Relaxed (50/150)", theme: theme) {
                        prefs.greenThresholdMs = 50
                        prefs.orangeThresholdMs = 150
                        monitor.notifyConfigChanged()
                    }
                }

                HStack(spacing: 6) {
                    ThresholdBadgeView(
                        color: LagatronColors.pingGreen,
                        label: "GREEN",
                        range: "< \(prefs.greenThresholdMs) ms",
                        theme: theme
                    )
                    ThresholdBadgeView(
                        color: LagatronColors.pingOrange,
                        label: "YELLOW",
                        range: "\(prefs.greenThresholdMs)–\(prefs.orangeThresholdMs) ms",
                        theme: theme
                    )
                    ThresholdBadgeView(
                        color: LagatronColors.pingRed,
                        label: "RED",
                        range: "> \(prefs.orangeThresholdMs) ms",
                        theme: theme
                    )
                }

                // Green Cutoff Slider
                VStack(spacing: 4) {
                    HStack {
                        Text("Green Cutoff (< ms)")
                            .font(.system(size: 12.5, weight: .semibold))
                            .foregroundColor(theme.onSurface)
                        Spacer()
                        Text("< \(prefs.greenThresholdMs) ms")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundColor(LagatronColors.pingGreen)
                    }
                    Slider(
                        value: Binding(
                            get: { Double(prefs.greenThresholdMs) },
                            set: { val in
                                let newGreen = max(5, Int((val / 5.0).rounded()) * 5)
                                prefs.greenThresholdMs = newGreen
                                if prefs.orangeThresholdMs < newGreen + 10 {
                                    prefs.orangeThresholdMs = newGreen + 10
                                }
                                monitor.notifyConfigChanged()
                            }
                        ),
                        in: 5.0...150.0,
                        step: 5.0
                    )
                    .tint(LagatronColors.pingGreen)
                }

                // Yellow Cutoff Slider
                VStack(spacing: 4) {
                    HStack {
                        Text("Yellow Cutoff (≤ ms)")
                            .font(.system(size: 12.5, weight: .semibold))
                            .foregroundColor(theme.onSurface)
                        Spacer()
                        Text("\(prefs.greenThresholdMs) – \(prefs.orangeThresholdMs) ms")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                            .foregroundColor(LagatronColors.pingOrange)
                    }
                    Slider(
                        value: Binding(
                            get: { Double(prefs.orangeThresholdMs) },
                            set: { val in
                                let newOrange = max(prefs.greenThresholdMs + 5, Int((val / 5.0).rounded()) * 5)
                                prefs.orangeThresholdMs = newOrange
                                monitor.notifyConfigChanged()
                            }
                        ),
                        in: 15.0...300.0,
                        step: 5.0
                    )
                    .tint(LagatronColors.pingOrange)
                }
            }
        }
    }

    // MARK: - Live Overlay Preview Card
    private var overlayPreviewCard: some View {
        let isMonochrome = (prefs.displayMode == .monochromeText)
        let defaultText = theme.onSurface
        let greenColor = isMonochrome ? defaultText : LagatronColors.pingGreen
        let orangeColor = isMonochrome ? defaultText : LagatronColors.pingOrange
        let redColor = isMonochrome ? defaultText : LagatronColors.pingRed

        let suffix = prefs.showMsUnit ? " ms" : ""
        let sampleGreen = "\(max(1, prefs.greenThresholdMs / 2))\(suffix)"
        let sampleOrange = "\((prefs.greenThresholdMs + prefs.orangeThresholdMs) / 2)\(suffix)"
        let sampleRed = "\(prefs.orangeThresholdMs + 40)\(suffix)"

        return CardContainer(theme: theme) {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Live Overlay Preview")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text(String(format: "How the indicator looks (%@, %.1f sp)", prefs.appFont.title, prefs.fontSizeSp))
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }

                HStack {
                    Spacer()
                    PreviewBadgeItem(
                        mode: prefs.displayMode,
                        font: prefs.appFont,
                        isBold: prefs.isBold,
                        isItalic: prefs.isItalic,
                        hasShadow: prefs.hasTextShadow,
                        fontSizeSp: CGFloat(prefs.fontSizeSp),
                        textColor: greenColor,
                        dotColor: LagatronColors.pingGreen,
                        sampleText: sampleGreen,
                        sublabel: "< \(prefs.greenThresholdMs)ms",
                        theme: theme
                    )
                    Spacer()
                    PreviewBadgeItem(
                        mode: prefs.displayMode,
                        font: prefs.appFont,
                        isBold: prefs.isBold,
                        isItalic: prefs.isItalic,
                        hasShadow: prefs.hasTextShadow,
                        fontSizeSp: CGFloat(prefs.fontSizeSp),
                        textColor: orangeColor,
                        dotColor: LagatronColors.pingOrange,
                        sampleText: sampleOrange,
                        sublabel: "\(prefs.greenThresholdMs)-\(prefs.orangeThresholdMs)ms",
                        theme: theme
                    )
                    Spacer()
                    PreviewBadgeItem(
                        mode: prefs.displayMode,
                        font: prefs.appFont,
                        isBold: prefs.isBold,
                        isItalic: prefs.isItalic,
                        hasShadow: prefs.hasTextShadow,
                        fontSizeSp: CGFloat(prefs.fontSizeSp),
                        textColor: redColor,
                        dotColor: LagatronColors.pingRed,
                        sampleText: sampleRed,
                        sublabel: "> \(prefs.orangeThresholdMs)ms",
                        theme: theme
                    )
                    Spacer()
                }
                .padding(.vertical, 14)
                .padding(.horizontal, 10)
                .background(theme.surfaceVariant)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(theme.outline.opacity(0.5), lineWidth: 1)
                )
            }
        }
    }

    // MARK: - Floating HUD & Position Adjuster Card
    private var floatingOverlayPositionCard: some View {
        CardContainer(theme: theme) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Draggable Floating HUD Overlay")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(theme.onSurface)
                        Text("Show an on-screen draggable badge in addition to the macOS Menu Bar item")
                            .font(.system(size: 12))
                            .foregroundColor(theme.onSurfaceVariant)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { prefs.showFloatingOverlay },
                        set: { val in
                            prefs.showFloatingOverlay = val
                            FloatingOverlayController.shared.updateVisibilityAndContent(result: monitor.latestPing)
                        }
                    ))
                    .labelsHidden()
                    .toggleStyle(.switch)
                    .tint(theme.primary)
                }

                if prefs.showFloatingOverlay {
                    Divider()
                        .background(theme.outline.opacity(0.4))

                    Text("Screen Placement Presets")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(theme.onSurfaceVariant)

                    let screenW = Int(NSScreen.main?.frame.width ?? 1440)
                    let screenH = Int(NSScreen.main?.frame.height ?? 900)

                    HStack(spacing: 6) {
                        PresetPositionButton(title: "Top Right", theme: theme) {
                            updateFloatingPosition(x: max(40, screenW - 140), y: 36)
                        }
                        PresetPositionButton(title: "Top Left", theme: theme) {
                            updateFloatingPosition(x: 30, y: 36)
                        }
                        PresetPositionButton(title: "Top Center", theme: theme) {
                            updateFloatingPosition(x: screenW / 2 - 40, y: 36)
                        }
                        PresetPositionButton(title: "Bottom Right", theme: theme) {
                            updateFloatingPosition(x: max(40, screenW - 140), y: max(40, screenH - 80))
                        }
                    }

                    HStack(spacing: 16) {
                        // D-Pad Nudge Controls
                        VStack(spacing: 4) {
                            NudgeButton(icon: "chevron.up", theme: theme) {
                                updateFloatingPosition(x: prefs.overlayX, y: max(0, prefs.overlayY - 25))
                            }
                            HStack(spacing: 6) {
                                NudgeButton(icon: "chevron.left", theme: theme) {
                                    updateFloatingPosition(x: max(0, prefs.overlayX - 25), y: prefs.overlayY)
                                }
                                Image(systemName: "arrow.up.and.down.and.arrow.left.and.right")
                                    .font(.system(size: 14))
                                    .foregroundColor(theme.primary)
                                    .frame(width: 24, height: 24)
                                NudgeButton(icon: "chevron.right", theme: theme) {
                                    updateFloatingPosition(x: prefs.overlayX + 25, y: prefs.overlayY)
                                }
                            }
                            NudgeButton(icon: "chevron.down", theme: theme) {
                                updateFloatingPosition(x: prefs.overlayX, y: prefs.overlayY + 25)
                            }
                        }
                        .frame(maxWidth: .infinity)

                        // Coordinates & Reset Card
                        VStack(spacing: 8) {
                            Text("Screen Coordinates")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(theme.onSurfaceVariant)

                            HStack(spacing: 24) {
                                VStack(spacing: 2) {
                                    Text("X")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(theme.primary)
                                    Text("\(prefs.overlayX)")
                                        .font(.system(size: 15, weight: .heavy, design: .monospaced))
                                        .foregroundColor(theme.onSurface)
                                }
                                VStack(spacing: 2) {
                                    Text("Y")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(theme.primary)
                                    Text("\(prefs.overlayY)")
                                        .font(.system(size: 15, weight: .heavy, design: .monospaced))
                                        .foregroundColor(theme.onSurface)
                                }
                            }

                            Button {
                                updateFloatingPosition(x: max(40, screenW - 140), y: 36)
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "arrow.counterclockwise")
                                    Text("Default")
                                }
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(theme.onSurface)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 5)
                                .background(theme.surface)
                                .cornerRadius(6)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6)
                                        .stroke(theme.outline, lineWidth: 1)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity)
                        .background(theme.surfaceVariant)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(theme.outline.opacity(0.5), lineWidth: 1)
                        )
                    }
                }
            }
        }
    }

    private func updateFloatingPosition(x: Int, y: Int) {
        prefs.overlayX = x
        prefs.overlayY = y
        FloatingOverlayController.shared.applyPosition(x: x, y: y)
    }

    // MARK: - Start on Boot Card
    private var startOnBootCard: some View {
        CardContainer(theme: theme) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Start at Login")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(theme.onSurface)
                    Text("Automatically launch Lagatron in the macOS Menu Bar when you log in")
                        .font(.system(size: 12))
                        .foregroundColor(theme.onSurfaceVariant)
                }
                Spacer()
                Toggle("", isOn: $prefs.startOnBoot)
                    .labelsHidden()
                    .toggleStyle(.switch)
                    .tint(theme.primary)
            }
        }
    }

    // MARK: - Info Footer Card
    private var infoFooterCard: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "menubar.rectangle")
                .font(.system(size: 16))
                .foregroundColor(theme.primary)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 2) {
                Text("macOS Menu Bar & Full-Screen HUD")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundColor(theme.onSurface)
                Text("Lagatron displays your live ping directly in the macOS status bar. Enable the Draggable Floating HUD above if you also want an always-on-top overlay that stays visible over full-screen games and apps.")
                    .font(.system(size: 11.5))
                    .foregroundColor(theme.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
        .padding(12)
        .background(theme.surfaceVariant.opacity(0.5))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(theme.outline, lineWidth: 1)
        )
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        let isRunning = prefs.isServiceRunning
        let activeHost = prefs.getActiveHost()

        return VStack(spacing: 0) {
            Rectangle()
                .frame(height: 1)
                .foregroundColor(theme.outline)

            Button {
                monitor.toggle()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: isRunning ? "stop.fill" : "play.fill")
                        .font(.system(size: 15, weight: .bold))
                    Text(isRunning ? "Stop Monitor" : "Start Monitor (\(activeHost))")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(isRunning ? LagatronColors.pingRed : theme.primary)
                .cornerRadius(12)
            }
            .buttonStyle(.plain)
            .padding(14)
            .background(theme.surface)
        }
    }

    // MARK: - About Dialog
    private var aboutDialogOverlay: some View {
        ZStack {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture {
                    prefs.showAboutDialog = false
                }

            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Image(systemName: "info.circle.fill")
                        .font(.system(size: 18))
                        .foregroundColor(theme.primary)
                    Text("About Lagatron")
                        .font(.system(size: 16, weight: .heavy))
                        .foregroundColor(theme.onSurface)
                }

                Text("About")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(theme.primary)

                Text("Lagatron is an ultra-lightweight telemetry HUD and macOS status bar monitor engineered to provide continuous, real-time round-trip latency visibility for network engineers, gamers, and power users.")
                    .font(.system(size: 12.5))
                    .foregroundColor(theme.onSurface)
                    .fixedSize(horizontal: false, vertical: true)

                Text("Lagatron v1.1 • Native macOS Menu Bar Edition")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(theme.onSurfaceVariant.opacity(0.8))

                HStack {
                    Spacer()
                    Button {
                        prefs.showAboutDialog = false
                    } label: {
                        Text("Close")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(theme.primary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(theme.surfaceVariant)
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(20)
            .frame(width: 340)
            .background(theme.surface)
            .cornerRadius(18)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(theme.outline, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.4), radius: 16, x: 0, y: 8)
        }
    }
}

// MARK: - Reusable Subcomponents

private struct CardContainer<Content: View>: View {
    let theme: ThemePalette
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.surface)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(theme.outline, lineWidth: 1)
            )
    }
}

private struct ChipButton: View {
    let title: String
    let isSelected: Bool
    var showCheckmark: Bool = false
    var customFont: Font? = nil
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                if isSelected && showCheckmark {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                }
                Text(title)
                    .font(customFont ?? .system(size: 12.5, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? .white : theme.onSurface)
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .frame(maxWidth: .infinity)
            .background(isSelected ? theme.primary : theme.surfaceVariant)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isSelected ? theme.primary : theme.outline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct StyleToggleChip: View {
    let title: String
    let icon: String
    let isSelected: Bool
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .bold))
                Text(title)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
            }
            .foregroundColor(isSelected ? theme.primary : theme.onSurface)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .frame(maxWidth: .infinity)
            .background(isSelected ? theme.primary.opacity(0.2) : theme.surfaceVariant)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isSelected ? theme.primary : theme.outline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct StatItemView: View {
    let label: String
    let value: String
    let theme: ThemePalette

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(theme.onSurfaceVariant)
            Text(value)
                .font(.system(size: 13, weight: .heavy, design: .monospaced))
                .foregroundColor(theme.onSurface)
        }
    }
}

private struct PreviewBadgeItem: View {
    let mode: DisplayMode
    let font: AppFont
    let isBold: Bool
    let isItalic: Bool
    let hasShadow: Bool
    let fontSizeSp: CGFloat
    let textColor: Color
    let dotColor: Color
    let sampleText: String
    let sublabel: String
    let theme: ThemePalette

    var body: some View {
        let isMonochrome = (mode == .monochromeText)
        VStack(spacing: 6) {
            HStack(spacing: mode == .both ? 5 : 0) {
                if mode == .dotOnly || mode == .both {
                    Circle()
                        .fill(dotColor)
                        .frame(width: 9, height: 9)
                }
                if mode != .dotOnly {
                    Text(sampleText)
                        .font(font.swiftUIFont(size: fontSizeSp, isBold: isBold, isItalic: isItalic))
                        .foregroundColor(textColor)
                        .shadow(
                            color: hasShadow ? Color.black.opacity(0.85) : .clear,
                            radius: hasShadow ? 1.5 : 0,
                            x: hasShadow ? -1.5 : 0,
                            y: hasShadow ? 1.5 : 0
                        )
                }
            }
            .padding(.horizontal, isMonochrome ? 6 : 8)
            .padding(.vertical, 4)
            .background(
                Group {
                    if !isMonochrome {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color(hex: 0x121212, alpha: 0.8))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
                            )
                    }
                }
            )

            Text(sublabel)
                .font(.system(size: 11))
                .foregroundColor(theme.onSurfaceVariant)
        }
    }
}

private struct PresetPositionButton: View {
    let title: String
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundColor(theme.onSurface)
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .frame(maxWidth: .infinity)
                .background(theme.surfaceVariant)
                .cornerRadius(6)
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(theme.outline, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct ThresholdBadgeView: View {
    let color: Color
    let label: String
    let range: String
    let theme: ThemePalette

    var body: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(color)
                .frame(width: 7, height: 7)
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.system(size: 9, weight: .heavy))
                    .foregroundColor(color)
                Text(range)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(theme.onSurface)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.12))
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(color.opacity(0.4), lineWidth: 1)
        )
    }
}

private struct NudgeButton: View {
    let icon: String
    let theme: ThemePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(theme.onSurface)
                .frame(width: 36, height: 32)
                .background(theme.surfaceVariant)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(theme.outline.opacity(0.6), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct PingGraphCanvas: View {
    let history: [PingPoint]
    let maxPing: Float
    let activeColor: Color
    let gridColor: Color

    var body: some View {
        Canvas { context, size in
            let width = size.width
            let height = size.height

            // Horizontal baseline grid lines
            for yFrac in [0.0, 0.5, 1.0] {
                var gridPath = Path()
                let y = height * yFrac
                gridPath.move(to: CGPoint(x: 0, y: y))
                gridPath.addLine(to: CGPoint(x: width, y: y))
                context.stroke(gridPath, with: .color(gridColor), lineWidth: 1)
            }

            let peakY = CGFloat(max(maxPing, 100.0) * 1.15)
            let stepX = history.count > 1 ? width / CGFloat(history.count - 1) : width

            var linePath = Path()
            var fillPath = Path()
            var firstPoint = true

            for (index, point) in history.enumerated() {
                let x = CGFloat(index) * stepX

                if point.isInterruption {
                    if !firstPoint {
                        fillPath.addLine(to: CGPoint(x: x - stepX, y: height))
                        fillPath.closeSubpath()
                    }

                    var dashLine = Path()
                    dashLine.move(to: CGPoint(x: x, y: 0))
                    dashLine.addLine(to: CGPoint(x: x, y: height))
                    context.stroke(
                        dashLine,
                        with: .color(LagatronColors.interruptGray),
                        style: StrokeStyle(lineWidth: 1.5, dash: [6, 4])
                    )

                    let pipRect = CGRect(x: x - 3, y: 1, width: 6, height: 6)
                    context.fill(Path(ellipseIn: pipRect), with: .color(LagatronColors.interruptGray))

                    firstPoint = true
                } else {
                    let pingVal = CGFloat(point.latencyMs)
                    let safeVal = pingVal < 0 ? peakY : pingVal
                    let normalized = (safeVal / peakY) * height
                    let clamped = min(max(normalized, 4.0), height - 4.0)
                    let y = height - clamped

                    if firstPoint {
                        linePath.move(to: CGPoint(x: x, y: y))
                        fillPath.move(to: CGPoint(x: x, y: height))
                        fillPath.addLine(to: CGPoint(x: x, y: y))
                        firstPoint = false
                    } else {
                        linePath.addLine(to: CGPoint(x: x, y: y))
                        fillPath.addLine(to: CGPoint(x: x, y: y))
                    }
                }
            }

            if !firstPoint {
                let lastX = CGFloat(history.count - 1) * stepX
                fillPath.addLine(to: CGPoint(x: lastX, y: height))
                fillPath.closeSubpath()
            }

            context.fill(
                fillPath,
                with: .linearGradient(
                    Gradient(colors: [activeColor.opacity(0.35), .clear]),
                    startPoint: CGPoint(x: 0, y: 0),
                    endPoint: CGPoint(x: 0, y: height)
                )
            )

            context.stroke(
                linePath,
                with: .color(activeColor),
                style: StrokeStyle(lineWidth: 2.2, lineCap: .round, lineJoin: .round)
            )

            if let lastPoint = history.last, !lastPoint.isInterruption {
                let lastVal = CGFloat(lastPoint.latencyMs)
                let lastSafe = lastVal < 0 ? peakY : lastVal
                let lastX = CGFloat(history.count - 1) * stepX
                let clamped = min(max((lastSafe / peakY) * height, 4.0), height - 4.0)
                let lastY = height - clamped

                let outerRect = CGRect(x: lastX - 6, y: lastY - 6, width: 12, height: 12)
                let innerRect = CGRect(x: lastX - 3, y: lastY - 3, width: 6, height: 6)
                context.fill(Path(ellipseIn: outerRect), with: .color(activeColor.opacity(0.3)))
                context.fill(Path(ellipseIn: innerRect), with: .color(activeColor))
            }
        }
    }
}
