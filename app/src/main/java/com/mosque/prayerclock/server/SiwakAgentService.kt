package com.mosque.prayerclock.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Layanan latar (foreground service) yang menjalankan sinkron dengan server, sehingga heartbeat,
 * pemantauan, dan perintah jarak jauh tetap berjalan saat SIWAK tertutup aplikasi lain (TRD §7.4).
 * Hanya berjalan bila TV terhubung ke server.
 */
@AndroidEntryPoint
class SiwakAgentService : Service() {
    companion object {
        private const val TAG = "SiwakAgent"
        private const val CHANNEL_ID = "siwak-agent"
        private const val NOTIFICATION_ID = 4018

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, SiwakAgentService::class.java))
            } catch (e: Exception) {
                // Android 12+ bisa menolak bila dipanggil dari latar belakang; dicoba lagi saat SIWAK dibuka.
                Log.w(TAG, "layanan latar belum bisa dimulai: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SiwakAgentService::class.java))
        }
    }

    @Inject lateinit var syncManager: ServerSyncManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (loop?.isActive != true) {
            loop =
                scope.launch {
                    syncManager.run()
                    // TV tidak lagi terhubung: hentikan layanan.
                    stopSelf()
                }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SIWAK terhubung ke website", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("SIWAK")
            .setContentText("Terhubung ke website SIWAK")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}
