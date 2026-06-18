package com.example.controllerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : AppCompatActivity() {

    private lateinit var ipInput: EditText
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView

    //private var mqttClient: MqttAsyncClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ipInput)
        connectButton = findViewById(R.id.connectButton)
        statusText = findViewById(R.id.statusText)

        connectButton.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            if (ip.isNotEmpty()) {
                connectToBroker(ip)
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.prompt_ip), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToBroker(ip: String) {
        val serverUri = "tcp://$ip:1883"
        val clientId = "AndroidClient_${System.currentTimeMillis()}"

        val topic = "test/topic"
        val payload = "hello".toByteArray()

        Log.d("MQTT_APP", "Attempting connection to: $serverUri")
        statusText.text = "Connecting to $ip..."
        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.black))
        statusText.setTextColor(ContextCompat.getColorStateList(this, R.color.black))

        try {
            MQTTManager.getClient()?.let {
                if (it.isConnected) {
                    try { it.disconnect() } catch (e: Exception) { Log.e("MQTT_APP", "Old client disconnect failed", e) }
                }
            }

            //mqttClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
            val mqttClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
            MQTTManager.setClient(mqttClient)

            MQTTManager.getClient()?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("MQTT_APP", "Callback: connectComplete ($serverURI)")
                    runOnUiThread {
                        statusText.text = "Status: Connected"
                        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.green))
                        Toast.makeText(this@MainActivity, R.string.connected_to_broker, Toast.LENGTH_SHORT).show()

                        try {
                            MQTTManager.getClient()?.publish(topic, payload, 1, false, null, object: IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    val controllerIntent = Intent(this@MainActivity, Controller::class.java)
                                    startActivity(controllerIntent)
                                }

                                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                    runOnUiThread {
                                        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.red))
                                        Toast.makeText(this@MainActivity, "Message publish failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT_APP", "Callback: connectionLost", cause)
                    runOnUiThread {
                        statusText.text = "Status: Connection Lost\n${cause?.message}"
                        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.red))
                        Toast.makeText(this@MainActivity, R.string.connection_lost, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun messageArrived(topic: String?, message: org.eclipse.paho.client.mqttv3.MqttMessage?) {
                    Log.d("MQTT_APP", "Message arrived on $topic")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val options = MqttConnectOptions().apply {
                isCleanSession = false
                isAutomaticReconnect = true
                connectionTimeout = 15
                keepAliveInterval = 60
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
            }

            Log.d("MQTT_APP", "Calling connect()...")
            MQTTManager.getClient()?.connect(options, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT_APP", "Listener: onSuccess")
                    runOnUiThread {
                        statusText.text = "Status: Connection Successful"
                        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.green))
                        Toast.makeText(this@MainActivity, R.string.connection_success, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    val errorMessage = if (exception is MqttException) {
                        "Code: ${exception.reasonCode}\n${exception.message}"
                    } else {
                        exception?.message ?: "Unknown error"
                    }
                    Log.e("MQTT_APP", "Listener: onFailure - $errorMessage", exception)
                    runOnUiThread {
                        statusText.text = "Status: Failed\n$errorMessage"
                        statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.red))
                        Toast.makeText(this@MainActivity, getString(R.string.connection_error, errorMessage), Toast.LENGTH_LONG).show()
                    }
                }
            })
            Log.d("MQTT_APP", "connect() initiated asynchronously")

        } catch (e: Throwable) {
            Log.e("MQTT_APP", "Fatal error in connectToBroker", e)
            statusText.text = "Fatal Error: ${e.message}"
            statusText.setTextColor(ContextCompat.getColorStateList(this@MainActivity, R.color.red))
            Toast.makeText(this@MainActivity, "Fatal error occurred", Toast.LENGTH_LONG).show()
        }
    }
}