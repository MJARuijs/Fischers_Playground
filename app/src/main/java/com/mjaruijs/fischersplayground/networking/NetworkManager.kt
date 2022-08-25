package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import java.util.concurrent.atomic.AtomicBoolean

object NetworkManager {

    private const val PUBLIC_SERVER_IP = "217.101.191.23"
    private const val LOCAL_SERVER_IP = "192.168.178.18"
    private const val SERVER_PORT = 4500

    private val clientInitializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    private val manager = Manager("Client")

    private lateinit var client: EncodedClient

    fun isRunning(): Boolean {
        if (initialized.get()) {
            return true
        }
        return false
    }

    fun run(context: Context) {
        if (initialized.get()) {
            return
        }

        manager.context = context

        Thread {
            try {
                clientInitializing.set(true)
                client = EncodedClient(LOCAL_SERVER_IP, SERVER_PORT, NetworkManager::onRead)
                initialized.set(true)
            } catch (e: Exception) {
                println("Failed to connect to server..")
                initialized.set(false)
            } finally {
                clientInitializing.set(false)
            }

            if (initialized.get()) {
                Thread(manager).start()
                manager.register(client)
            }
        }.start()
    }

    fun sendMessage(message: NetworkMessage) {

        Thread {
            while (clientInitializing.get()) {}

            if (initialized.get()) {
                println("Sending message: $message")
                client.write(message.toString())
            }

        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {
        if (message.topic != Topic.USER_STATUS) {
            println("Received message: $message")
        }

        if (message.topic == Topic.INFO) {
            val intent = Intent("mjaruijs.fischers_playground.INFO").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        } else if (message.topic == Topic.GAME_UPDATE) {
            val intent = Intent("mjaruijs.fischers_playground.GAME_UPDATE").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        } else if (message.topic == Topic.CHAT_MESSAGE) {
            val intent = Intent("mjaruijs.fischers_playground.CHAT_MESSAGE").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        } else if (message.topic == Topic.USER_STATUS) {
            val intent = Intent("mjaruijs.fischers_playground.USER_STATUS").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        }
    }

}