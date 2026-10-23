package com.wifiguard.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val passwordInput = EditText(this).apply {
            hint = "הכנס סיסמה לפתיחת ה-Wi-Fi"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val btnUnlock = Button(this).apply {
            text = "פתח חסימה"
        }

        val btnLock = Button(this).apply {
            text = "נעל Wi-Fi מחדש"
        }

        layout.addView(passwordInput)
        layout.addView(btnUnlock)
        layout.addView(btnLock)
        setContentView(layout)

        val serviceIntent = Intent(this, GuardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        btnUnlock.setOnClickListener {
            if (passwordInput.text.toString() == "218178002") {
                Prefs.setGuardState(this, false)
                Toast.makeText(this, "החסימה הופסקה זמנית", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "סיסמה שגויה!", Toast.LENGTH_SHORT).show()
            }
        }

        btnLock.setOnClickListener {
            Prefs.setGuardState(this, true)
            Toast.makeText(this, "ה-Wi-Fi ננעל מחדש", Toast.LENGTH_SHORT).show()
        }
    }
}
