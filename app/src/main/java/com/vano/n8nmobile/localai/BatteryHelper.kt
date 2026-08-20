package com.vano.n8nmobile.localai

import android.content.Context
import android.os.BatteryManager

object BatteryHelper {
    fun getBatteryPercent(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            100
        }
    }
}
