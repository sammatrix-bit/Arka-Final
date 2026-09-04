package com.arka.vpn

import android.app.Application

class ArkaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // هسته‌ی VPN واقعی (Xray-core از libv2ray.aar) موقع اولین اتصال، توسط
        // ArkaCoreManager.ensureInit() مقداردهی اولیه می‌شه — نیازی به init سراسری اینجا نیست.
    }
}
