package com.ternaryop.batterychargelogger

import android.content.SharedPreferences

const val PREF_ACCOUNT_NAME = "accountName"
const val PREF_LAST_CAPACITY = "lastCapacity"
const val PREF_SHEET_ID = "sheetId"
const val PREF_SHEET_NAME = "sheetName"

val SharedPreferences.lastCapacity: Int
    get() {
        return getInt(PREF_LAST_CAPACITY, -1)
    }

fun SharedPreferences.updateCapacity(capacity: Int) {
    edit()
        .putInt(PREF_LAST_CAPACITY, capacity)
        .apply()
}

val SharedPreferences.sheetId: String?
    get() {
        return getString(PREF_SHEET_ID, null)
    }

val SharedPreferences.sheetName: String?
    get() {
        return getString(PREF_SHEET_NAME, null)
    }
