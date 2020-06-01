package com.ternaryop.batterychargelogger.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

class BatteryLoggerJobService : AbsJobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        return LoggerJob.runJob(this, params)
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    companion object {
        const val BATTERY_LOGGER_JOB_ID = 1

        private val PERIODIC_BATTERY_LOGGER_MILLIS = TimeUnit.MINUTES.toMillis(15)

        fun scheduleJob(context: Context) {
            val jobInfo = JobInfo.Builder(BATTERY_LOGGER_JOB_ID, ComponentName(context, BatteryLoggerJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(PERIODIC_BATTERY_LOGGER_MILLIS)
                .build()
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)
        }

    }
}
