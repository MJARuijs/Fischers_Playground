package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.services.DataManager
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {

    companion object {

        private const val PUBLIC_SERVER_IP = "94.208.124.161"
//        private const val PUBLIC_SERVER_IP = "217.101.191.23"
        private const val LOCAL_SERVER_IP = "192.168.178.103"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"
        private const val SERVER_PORT = 4501

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
    private val sendingMessage = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: EncodedClient

    private val messageQueue = ArrayList<NetworkMessage>()

    fun stop() {
        while (sendingMessage.get()) {
            Thread.sleep(1)
        }
        messageQueue.clear()
        if (clientConnected.get()) {
            client.close()
            log("Stopping server connection!")
            clientConnected.set(false)
        }
        clientConnecting.set(false)
        manager.stop()
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

    private fun log(message: String) {
        println("Networker: $message")
    }

    fun sendMessage(message: NetworkMessage) {
        println("Trying to send message: ${message.topic}")
        sendingMessage.set(true)

        Thread {

            while (clientConnecting.get()) {
                Thread.sleep(1)
            }

            if (clientConnected.get()) {
                try {
                    log("Sending message: $message")
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {

                }
            } else {
                log("Added message to queue ${message.topic}")
                messageQueue += message
            }
            sendingMessage.set(false)
        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {

        val messageData = message.content.split('|').toTypedArray()

        if (message.topic == Topic.HEART_BEAT) {
            sendMessage(NetworkMessage(Topic.HEART_BEAT, ""))
            return
        }

        sendMessage(NetworkMessage(Topic.CONFIRM_MESSAGE, "", message.id))

        if (message.topic != Topic.CONFIRM_MESSAGE) {
            log("Received message: $message")
        }

//        val dataManager = DataManager(context)
        val dataManager = DataManager.getInstance(context)

        if (!dataManager.isMessageHandled(message.id)) {
            dataManager.handledMessage(message.id)
            dataManager.saveHandledMessages(context)

            val intent = Intent("mjaruijs.fischers_playground")
                .putExtra("topic", message.topic.toString())
                .putExtra("content", messageData)
                .putExtra("messageId", message.id)

            context.sendBroadcast(intent)
        } else {
            println("Message already handled: ${message.id} ${message.topic}")
        }
    }

}