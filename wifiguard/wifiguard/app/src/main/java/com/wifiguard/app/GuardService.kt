package com.wifiguard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.Executors

/**
 * מחליף את שני המקרואים ("חסימה" + "נעילה"):
 *  - Wi-Fi נדלק ולא נפתח בקוד -> מכבים, מאמתים, ומציגים בקשת קוד.
 *  - Wi-Fi כבוי -> החסימה חוזרת להיות פעילה.
 * בנוסף יש בדיקה כל 5 שניות, למקרה שהודעת מערכת הוחמצה.
 */
class GuardService : Service() {

    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var busy = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = check("broadcast")
    }

    private val poll = object : Runnable {
        override fun run() {
            check("poll")
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        handler.post(poll)
        AppLog.add(this, "השירות התחיל")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        try { unregisterReceiver(receiver) } catch (_: Throwable) {}
        AppLog.add(this, "השירות נעצר")
        super.onDestroy()
    }

    private fun check(src: String) {
        val on = WifiControl.isOn(this)
        val unlocked = Prefs.unlocked(this)
        if (on && !unlocked) {
            block(src)
        } else if (!on && unlocked &&
            System.currentTimeMillis() - Prefs.unlockTime(this) > 15_000
        ) {
            Prefs.setUnlocked(false)
            AppLog.add(this, "ה־Wi-Fi כבוי, החסימה הופעלה מחדש")
        }
    }

    private fun block(src: String) {
        if (busy || UnlockActivity.showing) return
        busy = true
        worker.execute {
            try {
                AppLog.add(this, "זוהה Wi-Fi דולק ($src), מתחיל חסימה")
                Thread.sleep(2000)
                if (!WifiControl.isOn(this) || Prefs.unlocked(this)) return@execute
                if (WifiControl.disable(this)) showPrompt()
            } finally {
                busy = false
            }
        }
    }

    private fun showPrompt() {
        val i = Intent(this, UnlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        handler.post {
            try { startActivity(i) } catch (e: Throwable) {
                AppLog.add(this, "פתיחה ישירה של חלון הקוד נחסמה: ${e.message}")
            }
        }
        // גיבוי: התראה במסך מלא, למקרה שאין הרשאת "הצגה מעל אפליקציות"
        val pi = PendingIntent.getActivity(
            this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Wi-Fi חסום")
            .setContentText("לחץ להזנת קוד")
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(2, n)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "חסינת וויפי", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("חסינת וויפי פעילה")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL = "wg"

        fun start(c: Context) {
            c.startForegroundService(Intent(c, GuardService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, GuardService::class.java))
        }
    }
}
