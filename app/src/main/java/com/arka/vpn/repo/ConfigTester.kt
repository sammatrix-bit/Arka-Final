package com.arka.vpn.repo

import android.content.Context
import android.util.Base64
import com.arka.vpn.model.ConfigProtocol
import com.arka.vpn.vpncore.ArkaCoreManager
import com.arka.vpn.vpncore.ConfigConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * تست ترکیبی: اول تست واقعی با هسته، اگه شکست خورد تست TCP ساده.
 */
object ConfigTester {

    data class TestResult(val reachable: Boolean, val latencyMs: Long)

    suspend fun testReachability(context: Context, link: String): TestResult =
        withContext(Dispatchers.IO) {
            // مرحله ۱: تست واقعی با هسته
            val testConfig = ConfigConverter.buildTestConfig(link)
            if (testConfig != null) {
                val realLatency = ArkaCoreManager.measureOutboundDelay(context, testConfig)
                if (realLatency >= 0) {
                    return@withContext TestResult(reachable = true, latencyMs = realLatency)
                }
            }

            // مرحله ۲: اگه تست واقعی شکست خورد، TCP ساده رو امتحان کن
            val target = extractHostPort(link) ?: return@withContext TestResult(false, -1)
            val start = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(target.first, target.second), 3000)
                }
                TestResult(reachable = true, latencyMs = System.currentTimeMillis() - start)
            } catch (e: Exception) {
                TestResult(reachable = false, latencyMs = -1)
            }
        }

    private fun extractHostPort(link: String): Pair<String, Int>? {
        return try {
            when (ConfigProtocol.fromLink(link)) {
                ConfigProtocol.VMESS -> {
                    val base64Part = link.removePrefix("vmess://").substringBefore('#')
                    val json = String(Base64.decode(base64Part, Base64.DEFAULT))
                    val host = Regex("\"add\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)
                    val port = Regex("\"port\"\\s*:\\s*\"?(\\d+)\"?").find(json)?.groupValues?.get(1)?.toIntOrNull()
                    if (!host.isNullOrBlank() && port != null) host to port else null
                }
                ConfigProtocol.VLESS, ConfigProtocol.TROJAN, ConfigProtocol.SHADOWSOCKS -> {
                    val afterAt = link.substringAfter('@', missingDelimiterValue = "")
                    if (afterAt.isBlank()) return null
                    val hostPortPart = afterAt.substringBefore('?').substringBefore('#').substringBefore('/')
                    val lastColon = hostPortPart.lastIndexOf(':')
                    if (lastColon <= 0) return null
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
}
