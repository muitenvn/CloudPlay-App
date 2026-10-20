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

        fun startVpn(context: Context, server: ServerLocation) {
            val intent = Intent(context, XboxCloudVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_NAME, server.name)
                putExtra(EXTRA_SERVER_IP, server.ip)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot start foreground service", e)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, XboxCloudVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                try {
                    context.stopService(Intent(context, XboxCloudVpnService::class.java))
                } catch (e2: Exception) {
                    Log.e(TAG, "Cannot stop service", e2)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Japan (Tokyo)"
                startOptimizer(serverName)
            }
            ACTION_DISCONNECT -> {
                stopOptimizer()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } catch (ignored: Exception) {
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startOptimizer(serverName: String) {
        if (isRunning.get()) return

        try {
            val notification = buildNotification("Đang tối ưu kết nối xCloud qua máy chủ $serverName")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (e: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning.set(true)
            Log.d(TAG, "Foreground Stream Optimizer service started for $serverName")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            stopSelf()
        }
    }

    private fun stopOptimizer() {
        isRunning.set(false)
        Log.d(TAG, "Foreground Stream Optimizer service stopped")
    }

    override fun onDestroy() {
        stopOptimizer()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CloudPlay Stream Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái duy trì luồng truyền Cloud Gaming"
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
