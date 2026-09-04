package com.arka.vpn.vpncore

import android.util.Base64
import com.arka.vpn.model.ConfigProtocol
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

/**
 * تبدیل لینک‌های vless/vmess/trojan/ss ذخیره‌شده در Room به یک outbound معتبر Xray-core،
 * و بستن آن داخل یک کانفیگ کامل (inbounds شامل tun واقعی + dns + routing).
 *
 * ساختار inbound با tag="tun" و protocol="tun" دقیقاً همون چیزیه که این نسخه‌ی خاص از
 * Xray-core (AndroidLibXrayLite) پشتیبانی می‌کنه — از app/src/main/assets/v2ray_config_with_tun.json
 * پروژه‌ی رسمی v2rayNG استخراج و تایید شده.
 */
object ConfigConverter {

    /** خروجی: (کانفیگ کامل JSON آماده برای CoreController.startLoop, تگ outbound که همیشه "proxy" است) */
    fun buildFullConfig(link: String, mtu: Int = 1500): String? {
        val outbound = toOutboundJson(link) ?: return null

        val root = JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("policy", JSONObject().apply {
                put("levels", JSONObject().put("8", JSONObject().apply {
                    put("handshake", 4)
                    put("connIdle", 300)
                    put("uplinkOnly", 1)
                    put("downlinkOnly", 1)
                }))
                put("system", JSONObject().apply {
                    put("statsOutboundUplink", true)
                    put("statsOutboundDownlink", true)
                })
            })
            put("stats", JSONObject())
            put("inbounds", JSONArray().apply {
                put(tunInbound(mtu))
            })
            put("outbounds", JSONArray().apply {
                put(outbound)
                put(directOutbound())
                put(blockOutbound())
            })
            put("routing", JSONObject().apply {
                put("domainStrategy", "AsIs")
                put("rules", JSONArray().apply {
                    // ترافیک شبکه‌های محلی/loopback نباید وارد تونل بشه
                    put(JSONObject().apply {
                        put("type", "field")
                        put("outboundTag", "direct")
                        put("ip", JSONArray(listOf("geoip:private")))
                    })
                })
            })
            put("dns", JSONObject().apply {
                put("hosts", JSONObject())
                put("servers", JSONArray(listOf("https://1.1.1.1/dns-query", "8.8.8.8")))
            })
        }
        return root.toString()
    }

    private fun tunInbound(mtu: Int): JSONObject = JSONObject().apply {
        put("tag", "tun")
        put("protocol", "tun")
        put("settings", JSONObject().apply {
            put("name", "arka0")
            put("MTU", mtu)
            put("userLevel", 8)
        })
        put("sniffing", JSONObject().apply {
            put("enabled", true)
            put("destOverride", JSONArray(listOf("http", "tls", "quic")))
        })
    }

    private fun directOutbound(): JSONObject = JSONObject().apply {
        put("tag", "direct")
        put("protocol", "freedom")
        put("streamSettings", JSONObject().apply {
            put("sockopt", JSONObject().apply {
                put("domainStrategy", "UseIP")
            })
        })
    }

    private fun blockOutbound(): JSONObject = JSONObject().apply {
        put("tag", "block")
        put("protocol", "blackhole")
        put("settings", JSONObject().put("response", JSONObject().put("type", "http")))
    }

    // ── ساخت outbound بر اساس پروتکل ──

    fun toOutboundJson(link: String): JSONObject? = try {
        when (ConfigProtocol.fromLink(link)) {
            ConfigProtocol.VMESS -> vmessOutbound(link)
            ConfigProtocol.VLESS -> vlessOutbound(link)
            ConfigProtocol.TROJAN -> trojanOutbound(link)
            ConfigProtocol.SHADOWSOCKS -> shadowsocksOutbound(link)
            ConfigProtocol.UNKNOWN -> null
        }
    } catch (e: Exception) {
        null
    }

    private fun vmessOutbound(link: String): JSONObject? {
        val base64Part = link.removePrefix("vmess://").substringBefore('#')
        val json = JSONObject(decodeBase64(base64Part))
        val address = json.optString("add").ifBlank { return null }
        val port = json.optString("port").toIntOrNull() ?: return null
        val uuid = json.optString("id").ifBlank { return null }
        val alterId = json.optString("aid", "0").toIntOrNull() ?: 0
        val security = json.optString("scy", "auto").ifBlank { "auto" }
        val network = json.optString("net", "tcp").ifBlank { "tcp" }
        val headerType = json.optString("type", "none")
        val host = json.optString("host")
        val path = json.optString("path")
        val tls = json.optString("tls")
        val sni = json.optString("sni").ifBlank { host }.ifBlank { address }
        val alpn = json.optString("alpn")
        val fp = json.optString("fp")

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vmess")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", address)
                    put("port", port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", uuid)
                        put("alterId", alterId)
                        put("security", security)
                        put("level", 8)
                    }))
                }))
            })
            put("streamSettings", buildStreamSettings(network, headerType, tls, host, path, sni, alpn, fp))
        }
    }

    private fun vlessOutbound(link: String): JSONObject? {
        val withoutScheme = link.removePrefix("vless://")
        val uuid = withoutScheme.substringBefore('@', "").ifBlank { return null }
        val afterAt = withoutScheme.substringAfter('@', "").ifBlank { return null }
        val hostPort = afterAt.substringBefore('?').substringBefore('#')
        val (host, port) = splitHostPort(hostPort) ?: return null
        val query = afterAt.substringAfter('?', "").substringBefore('#')
        val params = parseQuery(query)

        val flow = params["flow"].orEmpty()
        val network = params["type"] ?: "tcp"
        val headerType = params["headerType"] ?: "none"
        val security = params["security"] ?: "none"
        val sni = params["sni"] ?: params["host"] ?: host
        val streamHost = params["host"] ?: sni
        val path = params["path"].orEmpty()
        val alpn = params["alpn"].orEmpty()
        val fp = params["fp"].orEmpty()
        val pbk = params["pbk"].orEmpty()
        val sid = params["sid"].orEmpty()
        val spx = params["spx"].orEmpty()

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", uuid)
                        put("encryption", "none")
                        if (flow.isNotBlank()) put("flow", flow)
                        put("level", 8)
                    }))
                }))
            })
            put(
                "streamSettings",
                buildStreamSettings(network, headerType, security, streamHost, path, sni, alpn, fp, pbk, sid, spx)
            )
        }
    }

    private fun trojanOutbound(link: String): JSONObject? {
        val withoutScheme = link.removePrefix("trojan://")
        val password = withoutScheme.substringBefore('@', "").ifBlank { return null }
        val afterAt = withoutScheme.substringAfter('@', "").ifBlank { return null }
        val hostPort = afterAt.substringBefore('?').substringBefore('#')
        val (host, port) = splitHostPort(hostPort) ?: return null
        val query = afterAt.substringAfter('?', "").substringBefore('#')
        val params = parseQuery(query)

        val network = params["type"] ?: "tcp"
        val headerType = params["headerType"] ?: "none"
        // تروجان طبق قرارداد پیش‌فرض روی TLS کار می‌کنه مگر صریحاً security=none باشه
        val security = params["security"] ?: "tls"
        val sni = params["sni"] ?: params["peer"] ?: host
        val streamHost = params["host"] ?: sni
        val path = params["path"].orEmpty()
        val alpn = params["alpn"].orEmpty()
        val fp = params["fp"].orEmpty()

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("password", password)
                    put("level", 8)
                }))
            })
            put("streamSettings", buildStreamSettings(network, headerType, security, streamHost, path, sni, alpn, fp))
        }
    }

    private fun shadowsocksOutbound(link: String): JSONObject? {
        val withoutScheme = link.removePrefix("ss://").substringBefore('#')

        var methodPass: String? = null
        var hostPort: String? = null

        if (withoutScheme.contains('@')) {
            val left = withoutScheme.substringBeforeLast('@')
            val right = withoutScheme.substringAfterLast('@').substringBefore('?')
            methodPass = runCatching { decodeBase64(left) }.getOrNull()
                ?.takeIf { it.contains(':') } ?: left.takeIf { it.contains(':') }
            hostPort = right
        }
        if (methodPass == null || hostPort == null) {
            // فرمت قدیمی: کل method:password@host:port به‌صورت یک‌جا base64 شده
            val decoded = runCatching { decodeBase64(withoutScheme.substringBefore('?')) }.getOrNull()
            if (decoded != null && decoded.contains('@')) {
                methodPass = decoded.substringBeforeLast('@')
                hostPort = decoded.substringAfterLast('@')
            }
        }
        if (methodPass == null || hostPort == null || !methodPass.contains(':')) return null

        val method = methodPass.substringBefore(':')
        val password = methodPass.substringAfter(':')
        val (host, port) = splitHostPort(hostPort) ?: return null

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "shadowsocks")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", host)
                    put("port", port)
                    put("method", method)
                    put("password", password)
                    put("level", 8)
                }))
            })
        }
    }

    // ── کمکی‌ها ──

    private fun buildStreamSettings(
        network: String,
        headerType: String,
        security: String,
        host: String,
        path: String,
        sni: String,
        alpn: String,
        fp: String,
        pbk: String = "",
        sid: String = "",
        spx: String = ""
    ): JSONObject {
        val stream = JSONObject().put("network", network)

        when (network) {
            "ws" -> stream.put("wsSettings", JSONObject().apply {
                put("path", path.ifBlank { "/" })
                put("headers", JSONObject().apply { if (host.isNotBlank()) put("Host", host) })
            })
            "grpc" -> stream.put("grpcSettings", JSONObject().apply {
                put("serviceName", path.ifBlank { host })
                put("multiMode", false)
            })
            "h2", "http" -> stream.put("httpSettings", JSONObject().apply {
                put("host", JSONArray(listOf(host).filter { it.isNotBlank() }))
                put("path", path.ifBlank { "/" })
            })
            "tcp" -> if (headerType == "http") {
                stream.put("tcpSettings", JSONObject().apply {
                    put("header", JSONObject().apply {
                        put("type", "http")
                        put("request", JSONObject().apply {
                            put("path", JSONArray(listOf(path.ifBlank { "/" })))
                            put("headers", JSONObject().apply {
                                if (host.isNotBlank()) put("Host", JSONArray(listOf(host)))
                            })
                        })
                    })
                })
            }
            else -> Unit
        }

        when (security) {
            "tls" -> stream.put("security", "tls").put("tlsSettings", JSONObject().apply {
                put("serverName", sni)
                put("allowInsecure", false)
                if (fp.isNotBlank()) put("fingerprint", fp)
                if (alpn.isNotBlank()) put("alpn", JSONArray(alpn.split(",").map { it.trim() }))
            })
            "reality" -> stream.put("security", "reality").put("realitySettings", JSONObject().apply {
                put("serverName", sni)
                if (fp.isNotBlank()) put("fingerprint", fp)
                if (pbk.isNotBlank()) put("publicKey", pbk)
                if (sid.isNotBlank()) put("shortId", sid)
                if (spx.isNotBlank()) put("spiderX", spx)
            })
            else -> stream.put("security", "none")
        }

        return stream
    }

    private fun splitHostPort(hostPort: String): Pair<String, Int>? {
        val lastColon = hostPort.lastIndexOf(':')
        if (lastColon <= 0) return null
        val host = hostPort.substring(0, lastColon).trim('[', ']')
        val port = hostPort.substring(lastColon + 1).toIntOrNull() ?: return null
        return host to port
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&')
            .filter { it.isNotBlank() && it.contains('=') }
            .associate {
                val key = it.substringBefore('=')
                val value = URLDecoder.decode(it.substringAfter('='), "UTF-8")
                key to value
            }

    private fun decodeBase64(input: String): String {
        var normalized = input.trim().replace('-', '+').replace('_', '/')
        val padding = (4 - normalized.length % 4) % 4
        normalized += "=".repeat(padding)
        return String(Base64.decode(normalized, Base64.DEFAULT))
    }
}
