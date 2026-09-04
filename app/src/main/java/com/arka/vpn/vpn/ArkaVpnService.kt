package com.arka.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.arka.vpn.MainActivity
import com.arka.vpn.vpncore.ArkaCoreManager

/**
 * سرویس واقعی VPN. یک رابط TUN واقعی با VpnService.Builder می‌سازه و فایل‌دسکریپتورش رو
 * مستقیم به هسته‌ی Xray-core واقعی (ArkaCoreManager → libv2ray.aar) می‌ده.
 * دیگه هیچ تایمر شبیه‌سازی‌شده‌ای اینجا نیست.
 */
class ArkaVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_CONNECT = "com.arka.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.arka.vpn.action.DISCONNECT"
        const val EXTRA_CONFIG_JSON = "extra_config_json"
        private const val NOTIF_CHANNEL_ID = "arka_vpn_channel"
        private const val NOTIF_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopTunnel()
                return START_NOT_STICKY
            }
            else -> {
                val configJson = intent?.getStringExtra(EXTRA_CONFIG_JSON)
                if (configJson.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIF_ID, buildNotification())
                startTunnel(configJson)
            }
        }
        return START_STICKY
    }

    private fun startTunnel(configJson: String) {
        try {
            val builder = Builder()
                .setSession("Arka")
                .setMtu(1500)
                .addAddress("10.10.14.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                // جلوگیری از حلقه‌ی بی‌نهایت: ترافیک خودِ اپ نباید وارد تونل خودش بشه
                .addDisallowedApplication(packageName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface?.close()
            vpnInterface = builder.establish()

            val fd = vpnInterface?.fd
            if (fd == null) {
                stopTunnel()
                return
            }

            ArkaCoreManager.startLoop(applicationContext, configJson, fd)
        } catch (e: Exception) {
            stopTunnel()
        }
    }

    private fun stopTunnel() {
        try {
            ArkaCoreManager.stopLoop()
        } finally {
            try {
                vpnInterface?.close()
            } catch (_: Exception) {
            }
            vpnInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "اتصال آرکا",
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("آرکا متصل است")
            .setContentText("ترافیک شما از تونل امن رد می‌شود")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }
}
