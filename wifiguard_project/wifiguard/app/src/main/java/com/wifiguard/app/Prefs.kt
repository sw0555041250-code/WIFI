package com.wifiguard.app

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "wifi_guard_prefs"
    private const val KEY_GUARD_ENABLED = "guard_enabled"

    fun isGuardEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GUARD_ENABLED, true)
    }

    fun setGuardState(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GUARD_ENABLED, enabled).apply()
    }
}
