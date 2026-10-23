package com.wifiguard.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var toggleBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        fun btn(text: String, onClick: () -> Unit) = Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
            root.addView(this)
        }

        btn("1. בקש הרשאת Shizuku") {
            try {
                if (!ShizukuShell.binderAlive()) toast("Shizuku לא פועל. הפעל אותו קודם.")
                else Shizuku.requestPermission(100)
            } catch (e: Throwable) { toast("שגיאה: ${e.message}") }
        }
        btn("2. הרשאת הצגה מעל אפליקציות") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        btn("3. פטור מחיסכון בסוללה") {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Throwable) { toast("לא ניתן לפתוח: ${e.message}") }
        }
        toggleBtn = btn("") {
            if (Prefs.guardEnabled(this)) {
                askCode("הזן קוד כדי לעצור את החסימה") {
                    Prefs.setGuardEnabled(this, false)
                    GuardService.stop(this)
                    refresh()
                }
            } else {
                Prefs.setGuardEnabled(this, true)
                Prefs.setUnlocked(false)
                GuardService.start(this)
                refresh()
            }
        }
        btn("שנה קוד") {
            askCode("הזן את הקוד הנוכחי") {
                val ni = EditText(this).apply {
                    hint = "קוד חדש"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                AlertDialog.Builder(this).setTitle("קוד חדש").setView(ni)
                    .setPositiveButton("שמור") { _, _ ->
                        val v = ni.text.toString()
                        if (v.length >= 4) { Prefs.setCode(this, v); toast("הקוד עודכן") }
                        else toast("הקוד חייב להכיל לפחות 4 תווים")
                    }
                    .setNegativeButton("ביטול", null).show()
            }
        }
        btn("בדיקת כיבוי עכשיו (לאבחון)") {
            val app = applicationContext
            Thread {
                val ok = WifiControl.disable(app, 3)
                runOnUiThread { toast(if (ok) "כובה בהצלחה" else "הכיבוי נכשל, ראה יומן"); refresh() }
            }.start()
        }
        btn("רענן") { refresh() }

        logView = TextView(this).apply { textSize = 12f; gravity = Gravity.START }
        root.addView(logView)
        setContentView(ScrollView(this).apply { addView(root) })

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Prefs.guardEnabled(this)) GuardService.start(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val pm = getSystemService(PowerManager::class.java)
        val lines = listOf(
            "Shizuku פועל: ${yn(ShizukuShell.binderAlive())}",
            "הרשאת Shizuku לאפליקציה: ${yn(ShizukuShell.granted())}",
            "הצגה מעל אפליקציות: ${yn(Settings.canDrawOverlays(this))}",
            "פטור מסוללה: ${yn(pm.isIgnoringBatteryOptimizations(packageName))}",
            "החסימה מופעלת: ${yn(Prefs.guardEnabled(this))}",
            "Wi-Fi דולק כעת: ${yn(WifiControl.isOn(this))}",
            "נפתח בקוד: ${yn(Prefs.unlocked(this))}"
        )
        status.text = lines.joinToString("\n")
        toggleBtn.text = if (Prefs.guardEnabled(this)) "עצור חסימה (דורש קוד)" else "הפעל חסימה"
        logView.text = "\nיומן:\n" + AppLog.read(this)
    }

    private fun askCode(title: String, onOk: () -> Unit) {
        val input = EditText(this).apply {
            hint = "קוד"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setPositiveButton("אישור") { _, _ ->
                if (input.text.toString() == Prefs.code(this)) onOk() else toast("קוד שגוי")
            }
            .setNegativeButton("ביטול", null).show()
    }

    private fun yn(b: Boolean) = if (b) "כן" else "לא"
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
