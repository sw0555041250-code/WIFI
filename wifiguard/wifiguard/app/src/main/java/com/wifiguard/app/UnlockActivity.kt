package com.wifiguard.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

/** חלון הקוד (מקביל ל"בקשת קוד" במקרו). קוד נכון -> מפעיל Wi-Fi. קוד שגוי או ביטול -> נשאר כבוי. */
class UnlockActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showing = true
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val input = EditText(this).apply {
            hint = "קוד"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("קוד")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("אישור") { _, _ ->
                if (input.text.toString() == Prefs.code(this)) {
                    Prefs.setUnlocked(true)
                    AppLog.add(this, "הוזן קוד נכון, מפעיל Wi-Fi")
                    Toast.makeText(this, "קוד נכון", Toast.LENGTH_SHORT).show()
                    val app = applicationContext
                    Thread { WifiControl.enable(app) }.start()
                } else {
                    AppLog.add(this, "הוזן קוד שגוי")
                    Toast.makeText(this, "קוד שגוי", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton("ביטול") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        showing = false
        super.onDestroy()
    }

    companion object {
        @Volatile var showing = false
    }
}
