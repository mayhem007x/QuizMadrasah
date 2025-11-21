package com.mtsyastiq.quiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.opencsv.CSVReader
import okhttp3.*
import java.io.IOException
import java.io.StringReader

abstract class BaseDayChooseActivity : AppCompatActivity() {

    private lateinit var eventNameView: TextView
    private lateinit var gradeTextView: TextView

    // Abstract properties that subclasses must implement
    abstract val gradeNumber: String  // "7", "8", or "9"
    private val passwordsUrl = "https://docs.google.com/spreadsheets/d/1oONKFlTmIN4wgqQUiJNKeDf3e2RU4MV08fJw6grMIU8/export?format=csv&gid=1988574439"
    abstract val csvUrl: String      // URL to fetch event name from

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_day)

        eventNameView = findViewById(R.id.event_name)
        gradeTextView = findViewById(R.id.grade_text)

        // Set grade text
        gradeTextView.text = "Kelas $gradeNumber"

        // Fetch and display event name from CSV
        fetchEventName()

        // Wire up day buttons
        setupDayButton(R.id.btn_senin, "senin")
        setupDayButton(R.id.btn_selasa, "selasa")
        setupDayButton(R.id.btn_rabu, "rabu")
        setupDayButton(R.id.btn_kamis, "kamis")
        setupDayButton(R.id.btn_jumat, "jumat")
        setupDayButton(R.id.btn_sabtu, "sabtu")

        // Back button
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun fetchEventName() {
        val client = OkHttpClient()
        val request = Request.Builder().url(csvUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    eventNameView.text = "Error loading event name"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val reader = CSVReader(StringReader(body))
                    val rows = try { reader.readAll() } catch (t: Throwable) { emptyList() }
                    if (rows.isNotEmpty()) {
                        val eventName = rows[0].getOrNull(2)?.trim() ?: ""
                        runOnUiThread {
                            eventNameView.text = eventName
                        }
                    }
                }
            }
        })
    }

    private fun setupDayButton(buttonId: Int, day: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            showPasswordDialog(day)
        }
    }

    private fun showPasswordDialog(day: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.password_input)

        AlertDialog.Builder(this)
            .setTitle("Masukkan Password")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val password = passwordInput.text.toString()
                checkPassword(day, password)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkPassword(day: String, enteredPassword: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(passwordsUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@BaseDayChooseActivity,
                        "Gagal memeriksa password", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val reader = CSVReader(StringReader(body))
                    val rows = try { reader.readAll() } catch (t: Throwable) { emptyList() }

                    val correctPassword = when (day.lowercase()) {
                        "senin" -> rows.getOrNull(0)?.getOrNull(1)
                        "selasa" -> rows.getOrNull(1)?.getOrNull(1)
                        "rabu" -> rows.getOrNull(2)?.getOrNull(1)
                        "kamis" -> rows.getOrNull(3)?.getOrNull(1)
                        "jumat" -> rows.getOrNull(4)?.getOrNull(1)
                        "sabtu" -> rows.getOrNull(5)?.getOrNull(1)
                        else -> null
                    }?.trim()

                    runOnUiThread {
                        if (correctPassword != null && enteredPassword == correctPassword) {
                            // Start the corresponding day activity
                            startDayActivity(day)
                        } else {
                            Toast.makeText(this@BaseDayChooseActivity,
                                "Password anda salah, coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun startDayActivity(day: String) {
        val gid = when (day.lowercase()) {
            "senin" -> "1488244068"
            "selasa" -> "1058704566"
            "rabu" -> "1863002525"
            "kamis" -> "402384371"
            "jumat" -> "1956927793"
            "sabtu" -> "386671976"
            else -> "1488244068"
        }

        val intent = Intent(this, DayGradeActivity::class.java)
        intent.putExtra(DayGradeActivity.EXTRA_GID, gid)
        intent.putExtra(DayGradeActivity.EXTRA_GRADE, gradeNumber)
        intent.putExtra(DayGradeActivity.EXTRA_DAY, day)
        startActivity(intent)
    }
}
