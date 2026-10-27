package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground Service for CloudPlay Stream Optimizer.
 * Keeps the gaming process alive in foreground with high network priority,
 * without creating an unused pseudo-VPN tunnel interface.
 */
class XboxCloudVpnService : Service() {

    private val isRunning = AtomicBoolean(false)

    companion object {
        private const val TAG = "XboxStreamOptimizer"
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_SERVER_IP = "server_ip"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "cloudplay_vpn_channel"
        private val isServiceActive = AtomicBoolean(false)

        fun startVpn(context: Context, server: ServerLocation) {
            val intent = Intent(context, XboxCloudVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_NAME, server.name)
                putExtra(EXTRA_SERVER_IP, server.ip)
            }
            try {
                isServiceActive.set(true)
                try {
                    context.startService(intent)
                } catch (_: IllegalStateException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    }
                }
            } catch (e: Exception) {
                isServiceActive.set(false)
                Log.d(TAG, "Foreground service start skipped: ${e.message}")
            }
        }

        fun stopVpn(context: Context) {
            if (!isServiceActive.getAndSet(false)) return
            try {
                context.stopService(Intent(context, XboxCloudVpnService::class.java))
            } catch (e: Exception) {
                Log.d(TAG, "Service stop skipped: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "CloudPlay"
                startOptimizer(serverName)
            }
            ACTION_DISCONNECT, null -> {
                if (!isRunning.get()) {
                    startOptimizer("CloudPlay")
                }
                stopOptimizer()
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startOptimizer(serverName: String) {
        try {
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            val notifText = if (isEn) {
                "Keeping connection stable while gaming"
            } else {
                "Đang giữ kết nối ổn định khi chơi game"
            }
            val notification = buildNotification(notifText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning.set(true)
            isServiceActive.set(true)
            Log.d(TAG, "Foreground service active ($serverName)")
        } catch (e: Exception) {
            Log.d(TAG, "Foreground promotion skipped: ${e.message}")
            stopSelf()
        }
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopOptimizer() {
        isRunning.set(false)
        isServiceActive.set(false)
        Log.d(TAG, "Foreground Stream Optimizer service stopped")
    }

    override fun onDestroy() {
        stopOptimizer()
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CloudPlay Stream Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = if (isEn) "Keeps connection stable while gaming" else "Đang giữ kết nối ổn định khi chơi game"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CloudPlay Stream Optimizer")
            .setContentText(text)
            .setSmallIcon(com.example.R.drawable.cloudplay_launcher_icon_1791392788421)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
