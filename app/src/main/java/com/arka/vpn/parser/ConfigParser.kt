package com.arka.vpn.parser

import android.util.Base64
import com.arka.vpn.model.ConfigProtocol
import java.net.URLDecoder
import java.security.MessageDigest

object ConfigParser {

    /**
     * قانون پارس مشترک برای هر سه روش ورود کانفیگ (assets / کلیپ‌بورد):
     * هر خط = یک لینک، خط خالی و خط‌های شروع‌شده با # نادیده گرفته می‌شن،
     * و فقط پروتکل‌های مجاز (vless / vmess / trojan / ss) قبول می‌شن.
     */
    fun parseLines(raw: String): List<String> =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .filter { ConfigProtocol.fromLink(it) != ConfigProtocol.UNKNOWN }
            .distinct()
            .toList()

    fun protocolOf(link: String): ConfigProtocol = ConfigProtocol.fromLink(link)

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** تلاش best-effort برای استخراج یک اسم نمایشی از لینک (برای نمایش در لیست کانفیگ‌ها در فازهای بعد). */
    fun remarkOf(link: String): String? = try {
        when (ConfigProtocol.fromLink(link)) {
            ConfigProtocol.VMESS -> {
                val base64Part = link.removePrefix("vmess://")
                val json = String(Base64.decode(base64Part, Base64.DEFAULT))
                Regex("\"ps\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
            }
            ConfigProtocol.VLESS, ConfigProtocol.TROJAN, ConfigProtocol.SHADOWSOCKS -> {
                val fragment = link.substringAfter('#', "")
                if (fragment.isBlank()) null else URLDecoder.decode(fragment, "UTF-8")
            }
            ConfigProtocol.UNKNOWN -> null
        }
    } catch (e: Exception) {
        null
    }
}
