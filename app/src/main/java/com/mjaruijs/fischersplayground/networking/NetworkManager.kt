package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.util.Logger
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

object NetworkManager {

    private const val SERVER_IP = "192.168.178.71"
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
                client = EncodedClient(SERVER_IP, SERVER_PORT, NetworkManager::onRead)
                initialized.set(true)
            } catch (e: ConnectException) {
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

    fun sendMessage(message: Message) {
        Thread {
            while (clientInitializing.get()) {}

            if (initialized.get()) {
                client.write(message.toString())
            }

        }.start()
    }

    private fun onRead(message: Message, context: Context) {
        Logger.debug("Received message from: ${message.senderAddress}: $message")

        if (message.topic == Topic.INFO) {
            val intent = Intent("mjaruijs.fischers_playground.INFO").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        } else if (message.topic == Topic.GAME_UPDATE) {
            val intent = Intent("mjaruijs.fischers_playground.GAME_UPDATE").putExtra(message.category, message.content)
            context.sendBroadcast(intent)
        }
    }

}