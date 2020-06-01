package com.ternaryop.batterychargelogger

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.sheets.v4.SheetsScopes

private val SCOPES = arrayOf(SheetsScopes.SPREADSHEETS)

fun getAccount(context: Context, prefAccountName: String): GoogleAccountCredential? {
    return try {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val accountName = prefs.getString(prefAccountName, null) ?: return null

        GoogleAccountCredential
            .usingOAuth2(context, listOf(*SCOPES))
            .setBackOff(ExponentialBackOff())
            .apply {
                selectedAccountName = accountName
            }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

