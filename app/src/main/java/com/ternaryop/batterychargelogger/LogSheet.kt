package com.ternaryop.batterychargelogger

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.model.ValueRange
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class LogSheet(
    appName: String,
    credential: GoogleAccountCredential,
    private val sheetId: String
) {
    private var sheetsService: com.google.api.services.sheets.v4.Sheets

    init {
        val transport = NetHttpTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        sheetsService = com.google.api.services.sheets.v4.Sheets.Builder(
            transport, jsonFactory, credential
        )
            .setApplicationName(appName)
            .build()
    }

    fun log(sheetName: String?, message: String) {
        val valueInputOption = "USER_ENTERED"

        val now = LocalDateTime.now()
        val date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val time = now.format(DateTimeFormatter.ofPattern("hh:mm:ss"))

        val results = listOf(date, time, message)
        val list = listOf(results)
        val requestBody = ValueRange()
        requestBody.setValues(list)
        val sheetPrefix = sheetName?.let { "${it}!" } ?: ""

        val result = sheetsService
            .spreadsheets()
            .values()
            .append(sheetId, "${sheetPrefix}A1:A", requestBody)
            .setValueInputOption(valueInputOption)
            .execute()

        println("SheetUpdater.fill() - table range ${result.tableRange} updated ${result.updates.updatedRange}")
    }
}