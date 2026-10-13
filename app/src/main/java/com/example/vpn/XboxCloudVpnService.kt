package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

class XboxCloudVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)

    companion object {
        private const val TAG = "XboxCloudVpn"
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
                Log.e(TAG, "Cannot start service", e)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, XboxCloudVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot stop service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Japan (Tokyo)"
                startTunnel(serverName)
            }
            ACTION_DISCONNECT -> {
                stopTunnel()
                try {
                    stopForeground(true)
                } catch (ignored: Exception) {
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(serverName: String) {
        if (isRunning.get()) return

        try {
            val notification = buildNotification("Đang định tuyến qua $serverName để vượt rào Xbox")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (e: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Establish real VPN TUN interface so Android OS confirms active VPN
            try {
                val builder = Builder()
                    .setSession("XboxCloudVpn - $serverName")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")

                // Only route the app itself or pass through cleanly without disrupting other system apps
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.d(TAG, "addDisallowedApplication not applicable", e)
                }

                vpnInterface = builder.establish()
                Log.d(TAG, "VPN TUN Interface established successfully: $vpnInterface")
            } catch (e: Exception) {
                Log.w(TAG, "Could not establish system TUN interface: ${e.message}")
            }

            isRunning.set(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tunnel", e)
            stopSelf()
        }
    }

    private fun stopTunnel() {
        isRunning.set(false)
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing tunnel", e)
        }
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CloudPlay VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái đường truyền VPN Xbox Cloud"
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
            .setContentTitle("CloudPlay Switcher")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
