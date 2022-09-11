package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {

    companion object {

        private const val PUBLIC_SERVER_IP = "217.101.191.23"
        private const val LOCAL_SERVER_IP = "192.168.178.103"
        private const val SERVER_PORT = 4500

        private var instance: NetworkManager? = null

        fun getInstance(): NetworkManager {
            if (instance == null) {
                instance = NetworkManager()
            }

            return instance!!
        }

    }

    private val clientInitializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: EncodedClient

    private val messageQueue = ArrayList<NetworkMessage>()

    fun isRunning(): Boolean {
        if (initialized.get()) {
            return true
        }
        return false
    }

    private fun log(message: String) {
        println("Networker: $message")
    }

    fun stop(): Boolean {
        messageQueue.clear()
        if (initialized.get()) {
            client.close()
            initialized.set(false)
        }
        clientInitializing.set(false)
        manager.stop()
        return true
    }

    fun run(context: Context, address: String = LOCAL_SERVER_IP, port: Int = SERVER_PORT) {
        if (initialized.get()) {
            println("")
            return
        }

        manager = Manager("Client")
        manager.context = context
        manager.setOnClientDisconnect {
            stop()
        }

        Thread {
            try {
                clientInitializing.set(true)
                client = EncodedClient(address, port, ::onRead)
                initialized.set(true)
            } catch (e: Exception) {
                log("Failed to connect to server..")
                initialized.set(false)
            } finally {
                clientInitializing.set(false)
            }

            if (initialized.get()) {
                Thread(manager).start()
                manager.register(client)

//                println("processing queued messages")

                for (message in messageQueue) {
//                    println("Sending queued message: $message")
                    sendMessage(message)
                }
                messageQueue.clear()
            }
        }.start()
    }

    fun sendMessage(message: NetworkMessage) {

        Thread {
            while (clientInitializing.get()) {
                Thread.sleep(1)
            }

            if (initialized.get()) {
                log("Sending message: $message")
                try {
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {

                }
            } else {
                messageQueue += message
//                println("Queueing message: $message")
            }

        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {
//        if (message.topic != Topic.USER_STATUS_CHANGED) {
            log("Received message: $message")
//        }

        val messageData = message.content.split('|').toTypedArray()

        val intent = Intent("mjaruijs.fischers_playground")
            .putExtra("topic", message.topic.toString())
            .putExtra("content", messageData)

        context.sendBroadcast(intent)
    }

}