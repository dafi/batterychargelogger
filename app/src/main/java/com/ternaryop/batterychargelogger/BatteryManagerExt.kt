package com.ternaryop.batterychargelogger

import android.content.Context
import android.os.BatteryManager
import android.os.BatteryManager.*

val Context.batteryManager: BatteryManager
    get() {
        return getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

val BatteryManager.capacity: Int
    get() {
        return getIntProperty(BATTERY_PROPERTY_CAPACITY)
    }

val BatteryManager.isChargingStatus: Boolean
    get() {
        return getIntProperty(BATTERY_PROPERTY_STATUS) == BATTERY_STATUS_CHARGING
    }