package com.ternaryop.batterychargelogger

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by dave on 23/06/16.
 * helper class to log errors to file
 */
object Log {
    private val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun write(context: Context, vararg msg: String) {
        write(context, null, *msg)
    }

    fun write(context: Context, t: Throwable?, vararg msg: String) {
        val date = DATE_TIME_FORMAT.format(Date())
        val destFile = logFile(context);

        // Android Q uses sandbox so we must ensure directories exist
        destFile.parentFile?.mkdirs()
        try {
            FileOutputStream(destFile, true).use { fos ->
                val ps = PrintStream(fos)
                for (m in msg) {
                    ps.println("$date - $m")
                }
                t?.also {
                    ps.print("$date - ")
                    it.printStackTrace(ps)
                }
                ps.flush()
                ps.close()
            }
        } catch (fosEx: Exception) {
            fosEx.printStackTrace()
        }
    }

    fun readContent(context: Context): String {
        return if (logFile(context).exists()) {
            logFile(context).readText()
        } else {
            ""
        }
    }

    fun delete(context: Context) {
        logFile(context).delete()
    }

    private fun logFile(context: Context) = File(context.cacheDir, "log")
}
