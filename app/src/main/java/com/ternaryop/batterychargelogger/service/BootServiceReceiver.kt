package com.ternaryop.batterychargelogger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ternaryop.batterychargelogger.Log

// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.ternaryop.batterychargelogger.debug

class BootServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.write(context, "Boot completed")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BatteryLoggerJobService.scheduleJob(context)
        }
    }
}
