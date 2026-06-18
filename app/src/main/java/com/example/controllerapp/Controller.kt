package com.example.controllerapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class Controller : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentDirection = ""

    private lateinit var mqttStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var speedStatus: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var buttonMode: Button
    private var isManual = true

    private val repeatAction = object : Runnable {
        override fun run() {
            if (isManual) {
                MQTTManager.publish("controller/move", currentDirection)
            }
            handler.postDelayed(this, 200)
        }
    }

    private fun startRepeating(direction: String) {
        currentDirection = direction
        handler.removeCallbacks(repeatAction)
        handler.post(repeatAction)
    }

    private fun stopRepeating() {
        handler.removeCallbacks(repeatAction)
    }

    private fun updateConnectionStatusUI() {
        val isConnected = MQTTManager.isConnected()
        runOnUiThread {
            if (isConnected) {
                mqttStatus.text = "CONNECTED"
                mqttStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
            } else {
                mqttStatus.text = "DISCONNECTED"
                mqttStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
            }
        }
    }

    private fun updateModeStatusUI() {
        runOnUiThread {
            if (isManual) {
                buttonMode.text = "MANUAL"
                buttonMode.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            } else {
                buttonMode.text = "AI"
                buttonMode.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            }
        }
    }

    private fun subscribeToStatus() {
        MQTTManager.subscribe("controller/status") { message ->
            val data = message.split(",")
            if (data.size >= 4) {
                val battery = data[0]
                val speed = data[1]
                val connection = data[2]
                isManual = data[3].toBoolean()

                runOnUiThread {
                    batteryStatus.text = "Battery: $battery%"
                    speedStatus.text = "Speed: $speed"
                    connectionStatus.text = "Connection: $connection"
                    updateModeStatusUI()
                }
            }
        }
    }

    private fun subscribeToLogs() {
        MQTTManager.subscribe("controller/logs/input") { message ->
            MQTTManager.addLog("[IN] $message")
        }

        MQTTManager.subscribe("controller/logs/output") { message ->
            MQTTManager.addLog("[OUT] $message")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_controller)

        val buttonUp = findViewById<ImageButton>(R.id.buttonUp)
        val buttonLeft = findViewById<ImageButton>(R.id.buttonLeft)
        val buttonDown = findViewById<ImageButton>(R.id.buttonDown)
        val buttonRight = findViewById<ImageButton>(R.id.buttonRight)
        buttonMode = findViewById(R.id.buttonMode)
        val buttonLog = findViewById<Button>(R.id.buttonLog)
        val buttonBack = findViewById<Button>(R.id.buttonBack)

        mqttStatus = findViewById(R.id.statusMQTT)
        batteryStatus = findViewById(R.id.statusBattery)
        speedStatus = findViewById(R.id.statusSpeed)
        connectionStatus = findViewById(R.id.statusConnection)

        // Initial setup
        updateConnectionStatusUI()
        subscribeToStatus()
        subscribeToLogs()
        updateModeStatusUI()

        MQTTManager.getClient()?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                updateConnectionStatusUI()
                subscribeToStatus()
                subscribeToLogs()
            }

            override fun connectionLost(cause: Throwable?) {
                updateConnectionStatusUI()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {}

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        // Reusable touch listener
        val touchListener = View.OnTouchListener { view, event ->
            val direction = when (view.id) {
                R.id.buttonUp -> "forward"
                R.id.buttonLeft -> "left"
                R.id.buttonRight -> "right"
                R.id.buttonDown -> "reverse"
                else -> ""
            }

            if (direction.isEmpty()) return@OnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRepeating(direction)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRepeating()
                    true
                }
                else -> false
            }
        }

        buttonUp.setOnTouchListener(touchListener)
        buttonLeft.setOnTouchListener(touchListener)
        buttonRight.setOnTouchListener(touchListener)
        buttonDown.setOnTouchListener(touchListener)

        buttonMode.setOnClickListener {
            if (isManual) {
                MQTTManager.publish("controller/mode", "false")
            } else {
                MQTTManager.publish("controller/mode", "true")
            }
        }

        buttonLog.setOnClickListener {
            startActivity(Intent(this, Logs::class.java))
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }
}
