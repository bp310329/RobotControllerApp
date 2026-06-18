package com.example.controllerapp

import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended

object MQTTManager {
    private var client: MqttAsyncClient? = null
    val logs = mutableListOf<String>()
    var onLogsUpdated: (() -> Unit)? = null

    fun setClient(mqttClient: MqttAsyncClient) {
        client = mqttClient
    }

    fun getClient(): MqttAsyncClient? {
        return client
    }

    fun publish(topic: String, message: String) {
        try {
            if (client?.isConnected == true) {
                client?.publish(topic, message.toByteArray(), 1, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isConnected(): Boolean {
        return client?.isConnected == true
    }

    fun addLog(message: String) {
        logs.add(message)
        onLogsUpdated?.invoke()
    }

    fun subscribe(topic: String, onMessageReceived: (String) -> Unit) {
        try {
            if (client?.isConnected == true) {
                client?.subscribe(topic, 1) { _, msg ->
                    val text = String(msg.payload)
                    onMessageReceived(text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}