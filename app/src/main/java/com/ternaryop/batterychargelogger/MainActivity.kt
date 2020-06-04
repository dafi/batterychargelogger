package com.ternaryop.batterychargelogger

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.BatteryManager
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.BatteryManager.BATTERY_STATUS_CHARGING
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.sheets.v4.SheetsScopes
import com.ternaryop.batterychargelogger.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import kotlin.coroutines.CoroutineContext

class MainActivity
    : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks, CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private lateinit var credential: GoogleAccountCredential

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        job = Job()
        setupUI()
        update()

        credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, listOf(*SCOPES)
        )
            .setBackOff(ExponentialBackOff())

    }

    private fun setupUI() {
        binding.update.setOnClickListener { updateSheet() }
        binding.save.setOnClickListener { save() }
        binding.refreshLog.setOnClickListener { refreshLog() }
        binding.clearLog.setOnClickListener { clearLog() }
        binding.deleteLog.setOnClickListener { deleteLog() }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        binding.sheetId.setText(prefs.sheetId)
        binding.sheetName.setText(prefs.sheetName)
    }

    private fun deleteLog() {
        Log.delete(this)
        clearLog()
    }

    private fun clearLog() {
        binding.logContent.setText("")
    }

    private fun refreshLog() {
        binding.logContent.setText(Log.readContent(this))
    }

    private fun save() {
        val sheetId = binding.sheetId.text.toString()
        val sheetName = binding.sheetName.text.toString()
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()

        if (sheetId.isNotBlank()) {
            editor.putString(PREF_SHEET_ID, sheetId)
        }

        if (sheetName.isNotBlank()) {
            editor.putString(PREF_SHEET_NAME, sheetName)
        }

        editor.apply()
    }

    private fun updateSheet() {
        Log.write(this, "UpdateSheet")
        if (!isGooglePlayServicesAvailable()) {
            Log.write(this, "acquireGooglePlayServices")
            acquireGooglePlayServices()
        } else if (credential.selectedAccountName == null) {
            Log.write(this, "chooseAccount")
            chooseAccount()
        } else {
            update()
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val sheetId = prefs.sheetId ?: return
            launch(Dispatchers.IO) {
                try {
                    LogSheet(
                        getString(R.string.app_name),
                        credential,
                        sheetId
                    ).log(
                        prefs.sheetName,
                        "From UI " +
                                batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY).toString()
                    )
                } catch (t: Throwable) {
                    Log.write(applicationContext, t, "UpdateSheet")
                    runOnUiThread {
                        handleUpdateError(t)
                    }
                }
            }
        }
    }

    private fun handleUpdateError(t: Throwable) {
        when (t) {
            is GooglePlayServicesAvailabilityIOException -> showGooglePlayServicesAvailabilityErrorDialog(
                t.connectionStatusCode
            )
            is UserRecoverableAuthIOException -> startActivityForResult(
                t.intent,
                REQUEST_AUTHORIZATION
            )
            else -> Toast.makeText(
                this,
                "The following error occurred:\n" + t.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private fun chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS
            )
        ) {
            val accountName =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(PREF_ACCOUNT_NAME, null)
            if (accountName != null) {
                credential.selectedAccountName = accountName
                updateSheet()
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                this,
                "This app needs to access your Google account (via Contacts).",
                REQUEST_PERMISSION_GET_ACCOUNTS,
                Manifest.permission.GET_ACCOUNTS
            )
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    this,
                    getString(R.string.google_play_missing),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                updateSheet()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                data.extras != null
            ) {
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    val settings = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = settings.edit()
                    editor.putString(PREF_ACCOUNT_NAME, accountName)
                    editor.apply()
                    credential.selectedAccountName = accountName
                    updateSheet()
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                updateSheet()
            }
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     * requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode, permissions, grantResults, this
        )
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     * permission
     * @param list The requested permission list. Never null.
     */
    override fun onPermissionsGranted(requestCode: Int, list: List<String>) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     * permission
     * @param list The requested permission list. Never null.
     */
    override fun onPermissionsDenied(requestCode: Int, list: List<String>) {
        // Do nothing.
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     * Google Play Services on this device.
     */
    private fun showGooglePlayServicesAvailabilityErrorDialog(
        connectionStatusCode: Int
    ) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this@MainActivity,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog.show()
    }

    private fun update() {
        val lastCapacity = PreferenceManager.getDefaultSharedPreferences(this).lastCapacity
        val status = if (isCharging()) {
            "CHARGING"
        } else {
            "NONE"
        }
        binding.status.text = "$status ${batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)} ($lastCapacity)"
    }

    fun isCharging(): Boolean {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BATTERY_STATUS_CHARGING
    }

    companion object {
        const val REQUEST_ACCOUNT_PICKER = 1000
        const val REQUEST_AUTHORIZATION = 1001
        const val REQUEST_GOOGLE_PLAY_SERVICES = 1002
        const val REQUEST_PERMISSION_GET_ACCOUNTS = 1003

        private val SCOPES = arrayOf(SheetsScopes.SPREADSHEETS)
    }
}