package com.arka.vpn.data

import android.content.Context

/** وضعیت فعال‌سازی منبع «شخصی» رو بین اجراهای اپ نگه می‌داره (خودِ لینک/کد نمایش داده نمی‌شه). */
class PrivateAccessStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("arka_private_prefs", Context.MODE_PRIVATE)

    fun isActivated(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    fun activate(codeOrLink: String) {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_CODE, codeOrLink)
            .apply()
    }

    companion object {
        private const val KEY_ACTIVE = "private_active"
        private const val KEY_CODE = "private_code"
    }
}
