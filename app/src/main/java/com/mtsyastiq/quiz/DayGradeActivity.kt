package com.mtsyastiq.quiz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.opencsv.CSVReader
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayGradeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GID = "extra_gid"
        const val EXTRA_GRADE = "extra_grade"
        const val EXTRA_DAY = "extra_day"
        const val SHEET_ID = "1oONKFlTmIN4wgqQUiJNKeDf3e2RU4MV08fJw6grMIU8"
    }

    private lateinit var eventNameView: TextView
    private lateinit var gradeTextView: TextView
    private lateinit var dayDateTextView: TextView
    private lateinit var icon1: ImageView
    private lateinit var icon2: ImageView
    private lateinit var icon3: ImageView
    private lateinit var backBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_grade)

        eventNameView = findViewById(R.id.event_name)
        gradeTextView = findViewById(R.id.grade_text)
        dayDateTextView = findViewById(R.id.day_date_text)
        icon1 = findViewById(R.id.icon1)
        icon2 = findViewById(R.id.icon2)
        icon3 = findViewById(R.id.icon3)
        backBtn = findViewById(R.id.btn_back)

        val gid = intent.getStringExtra(EXTRA_GID) ?: return
        val grade = intent.getStringExtra(EXTRA_GRADE) ?: "7"
        val day = intent.getStringExtra(EXTRA_DAY) ?: "senin"

        // Set grade, day, and date
        gradeTextView.text = "Kelas $grade"
        val datePart = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())
        dayDateTextView.text = "${day.replaceFirstChar { it.uppercase() }}, $datePart"

        // Fetch event name from the main CSV (we'll reuse same CSV URL pattern without gid)
        fetchEventName()

        fetchSubjectsAndLinks(gid, grade)

        backBtn.setOnClickListener { finish() }
    }

    private fun getDownloadableDriveUrl(url: String?): String? {
        if (url.isNullOrEmpty() || !url.contains("drive.google.com")) {
            return url
        }
        // Try to find ID from "/d/..." or "/file/d/..."
        var fileId = Regex("/d/([A-Za-z0-9_-]+)").find(url)?.groups?.get(1)?.value

        // If not found, try to find ID from "?id=..."
        if (fileId.isNullOrEmpty()) {
            fileId = Regex("[?&]id=([A-Za-z0-9_-]+)").find(url)?.groups?.get(1)?.value
        }

        return if (fileId != null) {
            "https://drive.google.com/uc?export=download&id=$fileId"
        } else {
            url // Return original URL if no ID could be parsed
        }
    }

    private fun fetchEventName() {
        // Reuse the CSV used by MainActivity for event name (gid unspecified -> first sheet)
        val url = "https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread { eventNameView.text = "" }
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val body = response.body?.string() ?: ""
                val reader = CSVReader(StringReader(body))
                val rows = try { reader.readAll() } catch (t: Throwable) { emptyList() }
                if (rows.isNotEmpty()) {
                    val eventName = rows[0].getOrNull(2)?.trim() ?: ""
                    runOnUiThread { eventNameView.text = eventName }
                }
            }
        })
    }

    private fun fetchSubjectsAndLinks(gid: String, grade: String) {
        val url = "https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv&gid=$gid"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.w("DayGradeActivity", "Failed to fetch sheet gid=$gid", e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val body = try { response.body?.string() ?: "" } catch (t: Throwable) { "" }
                if (body.isBlank()) return

                val reader = CSVReader(StringReader(body))
                val rows = try { reader.readAll() } catch (t: Throwable) { emptyList() }

                // Determine start row for the grade (1-based)
                val startRow = when (grade) {
                    "7" -> 2
                    "8" -> 6
                    "9" -> 10
                    else -> 2
                }

                // helper to get cell at column index (A=0, B=1, C=2, D=3)
                fun cell(row: Int, colIndex: Int): String {
                    val idx = row - 1
                    if (idx < 0 || idx >= rows.size) return ""
                    val cols = rows[idx]
                    return cols.getOrNull(colIndex)?.trim() ?: ""
                }

                // Icons are column C -> index 2; forms are column D -> index 3; labels in B -> index 1
                val icons = mutableListOf<String>()
                val forms = mutableListOf<String>()
                val labels = mutableListOf<String>()
                for (i in 0..2) {
                    val r = startRow + i
                    icons.add(cell(r, 2))
                    forms.add(cell(r, 3))
                    labels.add(cell(r, 1))
                }

                runOnUiThread {
                    // Icon 1
                    if (icons[0].isNotEmpty()) {
                        Glide.with(this@DayGradeActivity).load(getDownloadableDriveUrl(icons[0])).placeholder(R.drawable.ic_launcher_foreground).into(icon1)
                        icon1.setOnClickListener {
                            val link = forms[0]
                            if (link.isNotEmpty()) {
                                val intent = Intent(this@DayGradeActivity, QuizActivity::class.java)
                                intent.putExtra(QuizActivity.EXTRA_URL, link)
                                startActivity(intent)
                            }
                        }
                    }
                    // label 1
                    findViewById<TextView>(R.id.label1).text = labels.getOrNull(0) ?: ""
                    // Icon 2
                    if (icons[1].isNotEmpty()) {
                        Glide.with(this@DayGradeActivity).load(getDownloadableDriveUrl(icons[1])).placeholder(R.drawable.ic_launcher_foreground).into(icon2)
                        icon2.setOnClickListener {
                            val link = forms[1]
                            if (link.isNotEmpty()) {
                                val intent = Intent(this@DayGradeActivity, QuizActivity::class.java)
                                intent.putExtra(QuizActivity.EXTRA_URL, link)
                                startActivity(intent)
                            }
                        }
                    }
                    // label 2
                    findViewById<TextView>(R.id.label2).text = labels.getOrNull(1) ?: ""
                    // Icon 3
                    if (icons[2].isNotEmpty()) {
                        Glide.with(this@DayGradeActivity).load(getDownloadableDriveUrl(icons[2])).placeholder(R.drawable.ic_launcher_foreground).into(icon3)
                        icon3.setOnClickListener {
                            val link = forms[2]
                            if (link.isNotEmpty()) {
                                val intent = Intent(this@DayGradeActivity, QuizActivity::class.java)
                                intent.putExtra(QuizActivity.EXTRA_URL, link)
                                startActivity(intent)
                            }
                        }
                    }
                    // label 3
                    findViewById<TextView>(R.id.label3).text = labels.getOrNull(2) ?: ""
                }
            }
        })
    }
}
