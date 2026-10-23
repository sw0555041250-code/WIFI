package com.wifiguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Prefs.guardEnabled(c)) {
            Prefs.setUnlocked(false)
            try { GuardService.start(c) } catch (_: Throwable) {}
        }
    }
}
