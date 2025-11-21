package com.mtsyastiq.quiz

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class QuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    private lateinit var webView: WebView
    private lateinit var backButton: Button
    private var isDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Prevent the screen from sleeping during the quiz.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = findViewById(R.id.quiz_webview)
        backButton = findViewById(R.id.btn_back_to_home)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val url = intent.getStringExtra(EXTRA_URL)
        if (url != null) {
            webView.loadUrl(url)
        }

        backButton.setOnClickListener {
            showConfirmationDialog()
        }

        // Set immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isDialogShowing && !isFinishing) {
            // This is the "fast response" logic.
            // It immediately brings the activity back to the front.
            startActivity(Intent(this, QuizActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))

            // And uses a handler to reliably show the dialog just after.
            Handler(Looper.getMainLooper()).post {
                if (!isFinishing) {
                    showInterruptionDialog()
                }
            }
        }
    }

    private fun showInterruptionDialog() {
        if (isDialogShowing) return
        isDialogShowing = true

        AlertDialog.Builder(this)
            .setMessage("Terdeteksi tidak fokus pada quiz, quiz anda akan direset")
            .setPositiveButton("OK") { _, _ ->
                webView.clearCache(true)
                webView.clearHistory()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finishAffinity() // Finish all activities in the stack
            }
            .setCancelable(false)
            .setOnDismissListener { isDialogShowing = false }
            .show()
    }

    private fun showConfirmationDialog() {
        if (isDialogShowing) return
        isDialogShowing = true

        AlertDialog.Builder(this)
            .setMessage("Anda yakin ingin kembali ke halaman utama?")
            .setPositiveButton("Ya") { _, _ ->
                // Screen pinning is NOT stopped here. It will persist when returning to MainActivity.
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Kembali") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener { isDialogShowing = false }
            .show()
    }

    override fun onDestroy() {
        webView.clearCache(true)
        webView.clearHistory()

        // Allow the screen to sleep again.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onDestroy()
    }
}
