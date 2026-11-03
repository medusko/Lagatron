package com.medusko.lagatron.model

enum class PresetServer(val title: String, val host: String) {
    GOOGLE("Google DNS", "8.8.8.8"),
    CLOUDFLARE("Cloudflare DNS", "1.1.1.1"),
    QUAD9("Quad9 DNS", "9.9.9.9"),
    CUSTOM("Custom Host", "")
}
