package com.arka.vpn.vpncore

import android.content.Context
import android.provider.Settings
import go.Seq
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * پوشش نازک روی libv2ray.aar — همون Xray-core واقعی که v2rayNG استفاده می‌کنه
 * (پروژه‌ی رسمی 2dust/AndroidLibXrayLite). این تنها لایه‌ای هست که مستقیم با هسته‌ی native حرف می‌زنه.
 *
 * API این کلاس (initCoreEnv / newCoreController / CoreController.startLoop(config, tunFd) / stopLoop /
 * measureDelay / queryAllOutboundTrafficStats) دقیقاً منطبق با نحوه‌ی استفاده‌ی واقعی v2rayNG از
 * همین کتابخونه‌ست — نه یک API فرضی.
 */
object ArkaCoreManager {

    private val initialized = AtomicBoolean(false)
    private var controller: CoreController? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastStatusMessage = MutableStateFlow<String?>(null)
    val lastStatusMessage: StateFlow<String?> = _lastStatusMessage.asStateFlow()

    // اگه startLoop شکست بخوره (کانفیگ نامعتبر، مشکل شبکه، ...)، اینجا خبر می‌ده —
    // چون فقط ست کردن isRunning=false کافی نیست (اگه از قبل هم false بوده، StateFlow چیزی emit نمی‌کنه
    // و UI برای همیشه توی حالت «در حال اتصال» گیر می‌کنه؛ این باگی بود که رفع شد).
    private val _connectionError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val connectionError: SharedFlow<String> = _connectionError.asSharedFlow()

    @Synchronized
    fun ensureInit(context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        Seq.setContext(appContext)

        val deviceId = try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "arka-device"
        } catch (e: Exception) {
            "arka-device"
        }

        // envPath فقط برای فایل‌های سفارشی روی دیسکه؛ geoip.dat/geosite.dat همراه libv2ray.aar
        // به assets خود اپ merge می‌شن و هسته خودکار از همونجا می‌خونتشون.
        Libv2ray.initCoreEnv(appContext.filesDir.absolutePath, deviceId)

        controller = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long {
                _isRunning.value = true
                return 0
            }

            override fun shutdown(): Long {
                _isRunning.value = false
                return 0
            }

            override fun onEmitStatus(l: Long, s: String?): Long {
                _lastStatusMessage.value = s
                return 0
            }
        })
    }

    /** واقعاً هسته را با فایل‌دسکریپتور TUN اجرا می‌کند — بدون تونل شبیه‌سازی‌شده. */
    fun startLoop(context: Context, configJson: String, tunFd: Int) {
        try {
            ensureInit(context)
            controller?.startLoop(configJson, tunFd)
        } catch (e: Exception) {
            _isRunning.value = false
            _connectionError.tryEmit(e.message ?: e.javaClass.simpleName)
        }
    }

    fun stopLoop() {
        try {
            controller?.stopLoop()
        } catch (e: Exception) {
            // اگه هسته اصلاً بالا نیومده بود، stopLoop خودش ممکنه خطا بده — بی‌خطره، فقط پاکسازی می‌کنیم
        } finally {
            _isRunning.value = false
        }
    }

    fun isCoreRunning(): Boolean = controller?.isRunning ?: false

    /** پینگ واقعی — این درخواست واقعاً از توی همون تونل پروکسی رد می‌شه، نه TCP خام. */
    fun measureDelay(url: String = "https://www.gstatic.com/generate_204"): Long =
        try {
            controller?.measureDelay(url) ?: -1L
        } catch (e: Exception) {
            -1L
        }

    /**
     * تست واقعیِ سطح پروتکل روی یک کانفیگ منفرد — بدون نیاز به وصل شدن واقعی یا اجازه‌ی VPN.
     * خودِ Xray-core این outbound رو واقعاً می‌سازه و یک درخواست HTTP از توش رد می‌کنه.
     * دقیقاً همون کاری که دکمه‌ی «تست» در v2rayNG / NekoBox انجام می‌ده — نه صرفاً TCP handshake.
     * برمی‌گردونه: تاخیر واقعی به میلی‌ثانیه، یا -1 اگه واقعاً کار نکرد (رمز غلط، سرور رد کرد، ...).
     */
    fun measureOutboundDelay(
        context: Context,
        testConfigJson: String,
        url: String = "https://www.gstatic.com/generate_204"
    ): Long {
        ensureInit(context)
        return try {
            Libv2ray.measureOutboundDelay(testConfigJson, url)
        } catch (e: Exception) {
            -1L
        }
    }

    /** آمار واقعی آپلود/دانلود از هسته — قالب Go: "tag,direction,value;tag,direction,value;..." */
    fun queryTrafficStats(): List<Triple<String, String, Long>> {
        val raw = try {
            controller?.queryAllOutboundTrafficStats().orEmpty()
        } catch (e: Exception) {
            ""
        }
        val result = mutableListOf<Triple<String, String, Long>>()
        raw.split(';').forEach { entry ->
            if (entry.isBlank()) return@forEach
            val parts = entry.split(',', limit = 3)
            if (parts.size == 3) {
                val value = parts[2].toLongOrNull() ?: return@forEach
                result.add(Triple(parts[0], parts[1], value))
            }
        }
        return result
    }

    fun reportError(message: String) {
        _connectionError.tryEmit(message)
    }
}
