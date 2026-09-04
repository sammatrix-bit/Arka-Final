package com.arka.vpn.model

/**
 * پنج حالت اتصال نوار بالا.
 * assetFile → دقیقاً همون فایلی که در app/src/main/assets/ برای این بخش خونده می‌شه.
 * انتخاب/تست کانفیگ برای هر حالت در ArkaViewModel (searchFast/searchFirstHealthy/searchHard) پیاده‌سازی شده.
 */
enum class ConnectionMode(
    val key: String,
    val label: String,
    val hint: String,
    val assetFile: String,
    val connectedMessage: String
) {
    FAST(
        key = "fast",
        label = "سریع",
        hint = "تست ۱۰ تا ۲۰ کانفیگ به‌صورت موازی و اتصال به کمترین تاخیر",
        assetFile = "fast.txt",
        connectedMessage = "سریع‌ترین کانفیگ انتخاب و وصل شد"
    ),
    NORMAL(
        key = "normal",
        label = "عادی",
        hint = "اتصال به اولین کانفیگ سالم",
        assetFile = "normal.txt",
        connectedMessage = "اولین کانفیگ سالم وصل شد"
    ),
    HARD(
        key = "hard",
        label = "سخت",
        hint = "جست‌وجوی گسترده در لیست بزرگ — هدف فقط وصل شدن، با تلاش مجدد خودکار",
        assetFile = "hard.txt",
        connectedMessage = "کانفیگ سالم پیدا و وصل شد"
    ),
    STABLE(
        key = "stable",
        label = "ثابت",
        hint = "اتصال به سرورهای پایدار با آپ‌تایم بالا",
        assetFile = "stable.txt",
        connectedMessage = "به سرور پایدار وصل شدید"
    ),
    USA(
        key = "usa",
        label = "آمریکا",
        hint = "فقط کانفیگ‌های country=US",
        assetFile = "usa.txt",
        connectedMessage = "با آی‌پی آمریکا وصل شدید"
    );

    companion object {
        fun fromKey(key: String): ConnectionMode = entries.first { it.key == key }
    }
}
