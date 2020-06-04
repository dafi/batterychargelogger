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
        if (isCapacityHigher(jobService)) {
            log(jobService)
            return true
        }
        return false
    }

    private fun log(jobService: AbsJobService) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val sheetId = prefs.sheetId ?: return
        val sheetName = prefs.sheetName ?: return
        val credential = getAccount(jobService, PREF_ACCOUNT_NAME) ?: return

        jobService.launch(Dispatchers.IO) {
            try {
                LogSheet(
                    jobService.getString(R.string.app_name),
                    credential,
                    sheetId
                ).log(sheetName, jobService.batteryManager.capacity.toString())
            } catch (t: Throwable) {
                Log.write(jobService, t)
            }
        }
    }

    private fun isCapacityHigher(jobService: AbsJobService): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        val lastCapacity = prefs.lastCapacity
        val capacity = jobService.batteryManager.capacity

        prefs.updateCapacity(capacity)

        Log.write(jobService, "isCapacityHigher capacity = $capacity, lastCapacity = $lastCapacity")
        return capacity > lastCapacity
    }
}
