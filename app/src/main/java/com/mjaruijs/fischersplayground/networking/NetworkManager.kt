package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.nio.Manager
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {

    companion object {

        private const val PUBLIC_SERVER_IP = "94.208.124.161"
//        private const val PUBLIC_SERVER_IP = "217.101.191.23"
        private const val LOCAL_SERVER_IP = "192.168.178.103"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"
        private const val SERVER_PORT = 4502

        private var instance: NetworkManager? = null

        fun getInstance(): NetworkManager {
            if (instance == null) {
                instance = NetworkManager()
            }

            return instance!!
        }

    }

    private val clientConnecting = AtomicBoolean(false)
    private val clientConnected = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: EncodedClient

    private val messageQueue = ArrayList<NetworkMessage>()

    private fun log(message: String) {
        println("Networker: $message")
    }

    fun stop(): Boolean {
        messageQueue.clear()
        if (clientConnected.get()) {
//            println("Client was connected; closing channel")
            client.close()
            clientConnected.set(false)
        } else {
//            println("No client was connected; no need to close channel")
        }
        clientConnecting.set(false)
        manager.stop()
        return true
    }

    fun isConnected(): Boolean {
        return clientConnected.get()
    }

    fun run(context: Context, address: String = PUBLIC_SERVER_IP, port: Int = SERVER_PORT) {
        if (clientConnected.get()) {
            return
        }

        log("Starting networker")

        manager = Manager("Client")
        manager.context = context
        manager.setOnClientDisconnect {
            stop()
        }

        Thread {
            try {
                clientConnecting.set(true)
                client = EncodedClient(PUBLIC_SERVER_IP, SERVER_PORT, ::onRead)
                clientConnected.set(true)
            } catch (e: Exception) {
                log("Failed to connect to server..")
                clientConnected.set(false)
            } finally {
                clientConnecting.set(false)
            }

            if (clientConnected.get()) {
                Thread(manager).start()
                manager.register(client)

                for (message in messageQueue) {
                    sendMessage(message)
                }
                messageQueue.clear()
            }
        }.start()
    }

    fun sendMessage(message: NetworkMessage) {
        Thread {
            while (clientConnecting.get()) {
                Thread.sleep(1)
            }

            if (clientConnected.get()) {
                try {
                    log("Sending message: ${message.content}")
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {

                }
            } else {
                messageQueue += message
            }

        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {
        log("Received message: $message")

        val messageData = message.content.split('|').toTypedArray()

        val intent = Intent("mjaruijs.fischers_playground")
            .putExtra("topic", message.topic.toString())
            .putExtra("content", messageData)

        context.sendBroadcast(intent)
    }

}