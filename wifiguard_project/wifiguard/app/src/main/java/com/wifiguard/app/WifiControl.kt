package com.wifiguard.app

import java.io.DataOutputStream

object WifiControl {
    fun disableWifi() {
        try {
            val process = Runtime.getRuntime().exec("su -c svc wifi disable")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("svc wifi disable\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                Runtime.getRuntime().exec("svc wifi disable")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
