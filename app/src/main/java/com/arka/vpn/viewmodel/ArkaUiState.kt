package com.arka.vpn.viewmodel

import com.arka.vpn.data.ConfigEntity
import com.arka.vpn.model.ConnectionMode
import com.arka.vpn.model.ConnectionState

data class ArkaUiState(
    val mode: ConnectionMode = ConnectionMode.NORMAL,
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val source: String = "public",              // "public" | "private"
    val privateUnlocked: Boolean = false,
    val progress: Float = 0f,                    // 0..1 — واقعاً از پیشرفتِ تستِ کانفیگ‌ها میاد
    val activeConfig: ConfigEntity? = null,       // کانفیگی که واقعاً تست شده و وصل شده
    val pingMs: Int? = null,                      // تاخیر واقعی TCP handshake همون کانفیگ فعال
    val elapsedSeconds: Int = 0,
    val dataUsageMb: Double = 0.0,                // تخمینی — بدون تونل واقعی قابل اندازه‌گیری دقیق نیست
    val currentSpeedMbps: Double = 0.0,           // تخمینی
    val sparkline: List<Float> = List(44) { 5f },
    val configCounts: Map<ConnectionMode, Int> = emptyMap(),
    val toastMessage: String? = null
)
