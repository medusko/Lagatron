package com.medusko.lagatron.model

enum class DisplayMode(val displayName: String, val description: String) {
    DOT_ONLY("Dot Only", "Minimalist colored status dot"),
    MS_ONLY("Latency (ms)", "Numeric latency in milliseconds"),
    BOTH("Dot & Latency", "Status dot with latency reading"),
    MONOCHROME_TEXT("White Text Only", "Clean borderless white text without container or colors")
}
