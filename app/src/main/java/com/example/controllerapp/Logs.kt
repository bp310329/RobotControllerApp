package com.example.controllerapp

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private fun refreshList(logText: TextView, logScroll: ScrollView) {
    logText.text = MQTTManager.logs.joinToString("\n")

    logScroll.post {
        logScroll.fullScroll(View.FOCUS_DOWN)
    }
}

class Logs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        val buttonBack = findViewById<Button>(R.id.buttonBack)
        val buttonClear = findViewById<Button>(R.id.buttonClear)
        val logText = findViewById<TextView>(R.id.logText)
        val logScroll = findViewById<ScrollView>(R.id.logScroll)
        val promptInput = findViewById<EditText>(R.id.promptInput)
        val sendPrompt = findViewById<Button>(R.id.sendPrompt)
        val inputLayout = findViewById<View>(R.id.inputLayout)

        ViewCompat.setOnApplyWindowInsetsListener(inputLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = maxOf(systemBars.bottom, ime.bottom)

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottomPadding
            )

            insets
        }

        buttonClear.setOnClickListener {
            MQTTManager.logs.clear()
            refreshList(logText, logScroll)
        }

        refreshList(logText, logScroll)

        MQTTManager.onLogsUpdated = {
            runOnUiThread {
                refreshList(logText, logScroll)
            }
        }

        buttonBack.setOnClickListener {
            finish()
        }

        sendPrompt.setOnClickListener {

            val prompt = promptInput.text.toString()

            if (prompt.isNotBlank()) {
                MQTTManager.publish("controller/prompt", prompt)
                MQTTManager.addLog("[APP] $prompt")

                promptInput.text.clear()
                promptInput.clearFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(promptInput.windowToken, 0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MQTTManager.onLogsUpdated = null
    }
}