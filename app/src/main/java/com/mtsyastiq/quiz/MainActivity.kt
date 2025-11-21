package com.mtsyastiq.quiz

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.opencsv.CSVReader
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.StringReader

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CSV_URL = "https://docs.google.com/spreadsheets/d/1oONKFlTmIN4wgqQUiJNKeDf3e2RU4MV08fJw6grMIU8/export?format=csv"
        private const val PREFS_NAME = "QuizAppPrefs"
        private const val KEY_PINNING_CONSENT = "pinningConsent"
    }

    private lateinit var logoView: ImageView
    private lateinit var schoolNameView: TextView
    private lateinit var eventNameView: TextView
    private lateinit var prefs: SharedPreferences

    private var dataHasBeenFetched = false
    private var permissionDialog: AlertDialog? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // When returning from settings, simply re-run the check.
        // onResume will handle this, so this callback can be empty.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        logoView = findViewById(R.id.school_logo)
        schoolNameView = findViewById(R.id.school_name)
        eventNameView = findViewById(R.id.event_name)

        findViewById<Button>(R.id.btn_kelas7).setOnClickListener { startChooseDay("7") }
        findViewById<Button>(R.id.btn_kelas8).setOnClickListener { startChooseDay("8") }
        findViewById<Button>(R.id.btn_kelas9).setOnClickListener { startChooseDay("9") }
        findViewById<Button>(R.id.btn_exit).setOnClickListener { showExitDialog() }
    }

    override fun onResume() {
        super.onResume()
        // onResume is the single source of truth. Always check permissions here.
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // If data is already loaded, we assume permissions are granted and we're good.
        if (dataHasBeenFetched) return

        if (hasAllPermissions()) {
            permissionDialog?.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    startLockTask()
                }
            }
            fetchCsvAndPopulate()
        } else {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        if (permissionDialog?.isShowing == true) {
            return // Don't create a new dialog if one is already showing.
        }

        permissionDialog?.dismiss() // Dismiss any previous instance.

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permissions, null)
        val overlaySwitch = dialogView.findViewById<SwitchCompat>(R.id.switch_overlay)
        val dndSwitch = dialogView.findViewById<SwitchCompat>(R.id.switch_dnd)
        val pinningSwitch = dialogView.findViewById<SwitchCompat>(R.id.switch_pinning)
        val okButton = dialogView.findViewById<Button>(R.id.btn_ok)

        fun updateOkButtonState() {
            val hasOverlay = hasOverlayPermission()
            val hasDnd = hasDndPermission()
            val hasPinningConsent = prefs.getBoolean(KEY_PINNING_CONSENT, false)
            okButton.isEnabled = hasOverlay && hasDnd && hasPinningConsent
        }

        overlaySwitch.isChecked = hasOverlayPermission()
        dndSwitch.isChecked = hasDndPermission()
        pinningSwitch.isChecked = prefs.getBoolean(KEY_PINNING_CONSENT, false)
        updateOkButtonState()

        overlaySwitch.setOnClickListener {
            if (!hasOverlayPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                permissionLauncher.launch(intent)
            }
        }

        dndSwitch.setOnClickListener {
            if (!hasDndPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                permissionLauncher.launch(intent)
            }
        }

        pinningSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_PINNING_CONSENT, isChecked).apply()
            updateOkButtonState()
        }

        okButton.setOnClickListener {
            permissionDialog?.dismiss()
            checkAndRequestPermissions()
        }

        permissionDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        permissionDialog?.show()
    }

    private fun hasAllPermissions(): Boolean {
        return hasOverlayPermission() && hasDndPermission() && prefs.getBoolean(KEY_PINNING_CONSENT, false)
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }

    private fun hasDndPermission(): Boolean {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) notificationManager.isNotificationPolicyAccessGranted else true
    }

    private fun startChooseDay(grade: String) {
        val activityClass = when (grade) {
            "7" -> ChooseDay7Activity::class.java
            "8" -> ChooseDay8Activity::class.java
            "9" -> ChooseDay9Activity::class.java
            else -> return
        }
        startActivity(Intent(this, activityClass))
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.exit_dialog_message))
            .setPositiveButton(getString(R.string.exit_dialog_positive)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    stopLockTask()
                }
                finishAffinity()
            }
            .setNegativeButton(getString(R.string.exit_dialog_negative), null)
            .show()
    }

    private fun getDownloadableDriveUrl(url: String?): String? {
        if (url.isNullOrEmpty() || !url.contains("drive.google.com")) {
            return url
        }
        var fileId = Regex("/d/([A-Za-z0-9_-]+)").find(url)?.groups?.get(1)?.value

        if (fileId.isNullOrEmpty()) {
            fileId = Regex("[?&]id=([A-Za-z0-9_-]+)").find(url)?.groups?.get(1)?.value
        }

        return if (fileId != null) {
            "https://drive.google.com/uc?export=download&id=$fileId"
        } else {
            url
        }
    }

    private fun fetchCsvAndPopulate() {
        if (dataHasBeenFetched) return

        val client = OkHttpClient()
        val request = Request.Builder().url(CSV_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    schoolNameView.text = getString(R.string.error_no_connection)
                    eventNameView.text = getString(R.string.error_check_connection)
                    Toast.makeText(this@MainActivity, getString(R.string.error_config_failed), Toast.LENGTH_SHORT).show()
                    logoView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = try { response.body?.string() ?: "" } catch (_: Throwable) { "" }
                if (body.isBlank()) {
                    runOnUiThread { logoView.setImageResource(R.drawable.ic_launcher_foreground) }
                    return
                }

                val reader = CSVReader(StringReader(body))
                val rows = try { reader.readAll() } catch (_: Throwable) { emptyList() }
                if (rows.isNotEmpty()) {
                    val first = rows[0]
                    val schoolText = first.getOrNull(1)?.trim() ?: ""
                    val eventText = first.getOrNull(2)?.trim() ?: ""
                    val logoUrl = first.getOrNull(0)?.trim()

                    val finalLogoUrl = getDownloadableDriveUrl(logoUrl)

                    runOnUiThread {
                        schoolNameView.text = schoolText
                        eventNameView.text = eventText
                        if (!finalLogoUrl.isNullOrEmpty()) {
                            Glide.with(this@MainActivity)
                                .load(finalLogoUrl)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(logoView)
                        } else {
                            logoView.setImageResource(R.drawable.ic_launcher_foreground)
                        }

                        dataHasBeenFetched = true
                    }
                }
            }
        })
    }
}
