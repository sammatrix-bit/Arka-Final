package com.arka.vpn.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * یک ردیف = یک لینک کانفیگ.
 * hash یونیکه (SHA-256 روی متن لینک) → همین باعث می‌شه لینک تکراری در insert نادیده گرفته بشه.
 */
@Entity(
    tableName = "configs",
    indices = [Index(value = ["hash"], unique = true)]
)
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,      // fast / normal / hard / stable / usa
    val protocol: String,      // vless / vmess / trojan / ss / unknown
    val link: String,
    val hash: String,
    val remark: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
