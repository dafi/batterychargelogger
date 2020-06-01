package com.ternaryop.batterychargelogger.service

import android.app.job.JobParameters
import androidx.preference.PreferenceManager
import com.ternaryop.batterychargelogger.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LoggerJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        if (isCharging(jobService)) {
            return false
        }
        if (isCapacityHigher(jobService)) {
            log(jobService)
            return true
        }
        return false
    }

    private fun log(jobService: AbsJobService) {
        val credential = getAccount(jobService, PREF_ACCOUNT_NAME) ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val sheetId = prefs.sheetId ?: return

        jobService.launch(Dispatchers.IO) {
            try {
                val now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                SheetUpdater(
                    jobService.getString(R.string.app_name),
                    credential,
                    sheetId
                )
                    .fill(
                        "From job $now",
                        jobService.batteryManager.capacity.toString()
                    )
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun isCapacityHigher(jobService: AbsJobService): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val lastCapacity = prefs.lastCapacity
        val capacity = jobService.batteryManager.capacity

        prefs.updateCapacity(capacity)

//        return capacity > lastCapacity
        return true
    }

    private fun isCharging(jobService: AbsJobService): Boolean {
        val isCharging = jobService.batteryManager.isChargingStatus

        return isCharging
    }
}
