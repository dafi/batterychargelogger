package com.ternaryop.batterychargelogger.service

import android.app.job.JobParameters
import androidx.preference.PreferenceManager
import com.ternaryop.batterychargelogger.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LoggerJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        Log.write(jobService, "runJob")
        if (jobService.batteryManager.isChargingStatus) {
            Log.write(jobService, "runJob.isChargingStatus")
            return false
        }
        val capacity = jobService.batteryManager.capacity
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        if (isCapacityHigher(jobService, capacity)) {
            try {
                log(jobService)
                prefs.updateCapacity(capacity)
            } catch (t: Throwable) {
                Log.write(jobService, t)
            }
            return true
        }
        prefs.updateCapacity(capacity)
        return false
    }

    private fun log(jobService: AbsJobService) {
        Log.write(jobService, "preparing for logging")
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val sheetId = prefs.sheetId ?: return
        val sheetName = prefs.sheetName ?: return
        val credential = getAccount(jobService, PREF_ACCOUNT_NAME) ?: return

        Log.write(jobService, "log to sheet")
        jobService.launch(Dispatchers.IO) {
            val capacity = jobService.batteryManager.capacity
            LogSheet(
                jobService.getString(R.string.app_name),
                credential,
                sheetId
            ).log(sheetName, capacity.toString())
            Log.write(jobService, "Logged with success")
        }
    }

    private fun isCapacityHigher(
        jobService: AbsJobService,
        capacity: Int
    ): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val lastCapacity = prefs.lastCapacity

        Log.write(jobService, "isCapacityHigher capacity = $capacity, lastCapacity = $lastCapacity")
        return capacity > lastCapacity
    }
}
