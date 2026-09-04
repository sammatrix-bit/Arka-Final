package com.arka.vpn.repo

import android.util.Base64
import com.arka.vpn.model.ConfigProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * تست سبک و سریع محلی: یک TCP handshake واقعی به host:port کانفیگ می‌زنه
 * (نه ICMP ping — به همین خاطر کانفیگ‌هایی که جواب ping نمی‌دن ولی proxy‌شون کار می‌کنه، درست تشخیص داده می‌شن).
 *
 * این تست فقط برای رتبه‌بندی/انتخاب سریع بین چند کانفیگ استفاده می‌شه (قبل از وصل شدن واقعی).
 * تونل واقعی و پینگ نهایی توسط هسته‌ی Xray-core واقعی (ArkaCoreManager, بعد از وصل شدن) انجام می‌شه.
 */
object ConfigTester {

    data class TestResult(val reachable: Boolean, val latencyMs: Long)

    suspend fun testReachability(link: String, timeoutMs: Int = 3000): TestResult =
        withContext(Dispatchers.IO) {
            val target = extractHostPort(link) ?: return@withContext TestResult(false, -1)
            val start = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target.first, target.second), timeoutMs)
                }
                TestResult(reachable = true, latencyMs = System.currentTimeMillis() - start)
            } catch (e: Exception) {
                TestResult(reachable = false, latencyMs = -1)
            }
        }

    /**
     * استخراج host:port بدون تکیه بر java.net.URI —
     * چون خیلی از لینک‌های واقعی vless/trojan/ss (userinfo با base64، کاراکترهای خاص در remark و...)
     * باعث می‌شن URI() اکسپشن بده و کل کانفیگ به‌اشتباه رد بشه، درحالی‌که کانفیگ کاملاً معتبره.
     * این نسخه فقط با substring/regex ساده کار می‌کنه، خیلی مقاوم‌تره.
     */
    private fun extractHostPort(link: String): Pair<String, Int>? = try {
        when (ConfigProtocol.fromLink(link)) {
            ConfigProtocol.VMESS -> {
                val base64Part = link.removePrefix("vmess://").substringBefore('#')
                val json = String(Base64.decode(base64Part, Base64.DEFAULT))
                val host = Regex("\"add\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
                val port = Regex("\"port\"\\s*:\\s*\"?(\\d+)\"?").find(json)?.groupValues?.get(1)?.toIntOrNull()
                if (!host.isNullOrBlank() && port != null) host to port else null
            }
            ConfigProtocol.VLESS, ConfigProtocol.TROJAN, ConfigProtocol.SHADOWSOCKS -> {
                // فرمت رایج: protocol://userinfo@host:port?params#remark
                val afterAt = link.substringAfter('@', missingDelimiterValue = "")
                if (afterAt.isBlank()) return@extractHostPort null
                val hostPortPart = afterAt.substringBefore('?').substringBefore('#').substringBefore('/')
                val lastColon = hostPortPart.lastIndexOf(':')
                if (lastColon <= 0) return@extractHostPort null
                val host = hostPortPart.substring(0, lastColon).trim('[', ']')
                val port = hostPortPart.substring(lastColon + 1).toIntOrNull()
                if (host.isNotBlank() && port != null) host to port else null
            }
            ConfigProtocol.UNKNOWN -> null
        }
    } catch (e: Exception) {
        null
    }
}
