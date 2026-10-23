package com.wifiguard.app

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import rikka.shizuku.Shizuku

object ShizukuShell {
    fun binderAlive(): Boolean = try { Shizuku.pingBinder() } catch (e: Throwable) { false }

    fun granted(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    /** מריץ פקודת shell דרך Shizuku. מחזיר (קוד יציאה, פלט) או (-1, הודעת שגיאה). */
    fun run(cmd: String): Pair<Int, String> {
        return try {
            if (!granted()) return Pair(-1, "אין הרשאת Shizuku")
            // newProcess פרטי ב־API החדש, לכן קוראים לו דרך reflection.
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            Pair(code, (out + err).trim())
        } catch (e: Throwable) {
            Pair(-1, "חריגה: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

object WifiControl {
    private fun wm(c: Context) =
        c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun isOn(c: Context): Boolean = wm(c).isWifiEnabled

    /** מכבה Wi-Fi ומאמת בפועל שהוא כבוי. מנסה כמה פעמים. */
    @Suppress("DEPRECATION")
    fun disable(c: Context, tries: Int = 5): Boolean {
        repeat(tries) { i ->
            val r = ShizukuShell.run("svc wifi disable")
            AppLog.add(c, "כיבוי, ניסיון ${i + 1}: קוד=${r.first} פלט='${r.second}'")
            try { wm(c).isWifiEnabled = false } catch (_: Throwable) {}
            Thread.sleep(1500)
            if (!isOn(c)) {
                AppLog.add(c, "אומת: ה־Wi-Fi כבוי")
                return true
            }
        }
        AppLog.add(c, "נכשל: ה־Wi-Fi נשאר דולק אחרי $tries ניסיונות")
        return false
    }

    @Suppress("DEPRECATION")
    fun enable(c: Context) {
        val r = ShizukuShell.run("svc wifi enable")
        AppLog.add(c, "הפעלה: קוד=${r.first} פלט='${r.second}'")
        try { wm(c).isWifiEnabled = true } catch (_: Throwable) {}
    }
}
