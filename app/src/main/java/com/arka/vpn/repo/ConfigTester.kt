package com.arka.vpn.repo

import android.util.Base64
import com.arka.vpn.model.ConfigProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

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
