package com.example.controllerapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private fun refreshList(list: EditText) {
    val logs = MQTTManager.logs.joinToString("\n")
    list.setText(logs)
}

class Logs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_logs)

        val buttonBack = findViewById<Button>(R.id.buttonBack)
        val buttonClear = findViewById<Button>(R.id.buttonClear)
        val list = findViewById<EditText>(R.id.editTextTextMultiLine)

        buttonClear.setOnClickListener {
            MQTTManager.logs.clear()
            refreshList(list)
        }

        refreshList(list)
        MQTTManager.onLogsUpdated = {
            runOnUiThread {
                refreshList(list)
            }
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        MQTTManager.onLogsUpdated = null
    }
}