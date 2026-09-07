package com.arka.vpn.repo

import android.content.Context
import com.arka.vpn.vpncore.ArkaCoreManager
import com.arka.vpn.vpncore.ConfigConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * تست واقعیِ سطح پروتکل روی هر کانفیگ — نه فقط TCP handshake.
 *
 * قبلاً این کلاس فقط چک می‌کرد پورت سرور باز هست یا نه (TCP)، که نمی‌تونست تشخیص بده
 * پروتکل/رمز/تنظیمات کانفیگ واقعاً درسته یا نه. الان دقیقاً کاری رو می‌کنه که دکمه‌ی
 * «تست» در v2rayNG / NekoBox انجام می‌ده: خودِ Xray-core یک outbound واقعی از روی همین
 * کانفیگ می‌سازه و یک درخواست HTTP واقعی (به gstatic.com/generate_204) از توش رد می‌کنه.
 * اگه رمز غلط باشه، سرور رد کنه، یا هر مشکل واقعی دیگه‌ای باشه، این تست شکست می‌خوره —
 * برخلاف TCP ساده که فقط می‌گفت «پورت باز است».
 */
object ConfigTester {

    data class TestResult(val reachable: Boolean, val latencyMs: Long)

    suspend fun testReachability(context: Context, link: String): TestResult =
        withContext(Dispatchers.IO) {
            val testConfig = ConfigConverter.buildTestConfig(link)
                ?: return@withContext TestResult(false, -1)

            val latency = ArkaCoreManager.measureOutboundDelay(context, testConfig)
            if (latency >= 0) {
                TestResult(reachable = true, latencyMs = latency)
            } else {
                TestResult(reachable = false, latencyMs = -1)
            }
        }
}
