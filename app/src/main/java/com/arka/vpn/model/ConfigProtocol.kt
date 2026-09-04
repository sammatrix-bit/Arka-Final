package com.arka.vpn.model

/** پروتکل‌های مجاز برای لینک‌های کانفیگ. */
enum class ConfigProtocol(val scheme: String) {
    VLESS("vless://"),
    VMESS("vmess://"),
    TROJAN("trojan://"),
    SHADOWSOCKS("ss://"),
    UNKNOWN("");

    companion object {
        fun fromLink(link: String): ConfigProtocol =
            entries.firstOrNull { it != UNKNOWN && link.startsWith(it.scheme, ignoreCase = true) }
                ?: UNKNOWN
    }
}
