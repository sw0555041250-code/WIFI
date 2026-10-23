package com.wifiguard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class GuardService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(1, createNotification())
            startGuardLoop()
        }
        return START_STICKY
    }

    private fun startGuardLoop() {
        serviceScope.launch {
            while (isActive && isRunning) {
                if (Prefs.isGuardEnabled(applicationContext)) {
                    WifiControl.disableWifi()
                }
                delay(3000)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "GUARD_CHANNEL")
            .setContentTitle("Wi-Fi Guard פעיל")
            .setContentText("מערכת הגנת ה-Wi-Fi פועלת ברקע באופן רציף")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "GUARD_CHANNEL",
                "Wi-Fi Guard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }
}
