package com.wifiguard.app

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("wg", Context.MODE_PRIVATE)

    // אותו קוד שהיה במקרו. אפשר לשנות מתוך האפליקציה.
    fun code(c: Context): String = sp(c).getString("code", "123456")!!
    fun setCode(c: Context, v: String) = sp(c).edit().putString("code", v).apply()

    fun unlocked(c: Context): Boolean = sp(c).getBoolean("unlocked", false)
    fun unlockTime(c: Context): Long = sp(c).getLong("unlockTime", 0L)
    fun setUnlocked(c: Context, v: Boolean) {
        sp(c).edit().putBoolean("unlocked", v)
            .putLong("unlockTime", if (v) System.currentTimeMillis() else 0L).apply()
    }

    fun guardEnabled(c: Context): Boolean = sp(c).getBoolean("enabled", true)
    fun setGuardEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()
}

object AppLog {
    private fun file(c: Context) = File(c.filesDir, "log.txt")

    @Synchronized
    fun add(c: Context, msg: String) {
        val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        val f = file(c)
        f.appendText("$t  $msg\n")
        if (f.length() > 60_000) {
            f.writeText(f.readLines().takeLast(200).joinToString("\n") + "\n")
        }
    }

    @Synchronized
    fun read(c: Context): String {
        val f = file(c)
        return if (f.exists()) f.readLines().takeLast(60).joinToString("\n") else "(אין רישומים)"
    }
}
