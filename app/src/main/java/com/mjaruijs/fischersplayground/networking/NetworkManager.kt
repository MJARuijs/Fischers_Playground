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
        private const val LOCAL_SERVER_IP = "192.168.178.17"
        private const val SERVER_PORT = 4500

        private var instance: NetworkManager? = null
//
//        fun keepAlive() {
//            if (instance == null) {
//                println("Networker: wanted to set keepAlive but instance is null")
//                return
//            }
//            instance!!.keepAlive()
//        }

        fun getInstance(listener: NetworkListener): NetworkManager {
            if (instance == null) {
                instance = NetworkManager()
            }

            instance!!.addListener(listener)

            return instance!!
        }

    }

    private val clientInitializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private lateinit var manager: Manager

    private lateinit var client: EncodedClient

    private val messageQueue = ArrayList<NetworkMessage>()

//    private var listeners = ArrayList<NetworkListener>()

    private var keepAlive = AtomicBoolean(false)

//    var numberOfClients = 0
//        private set

    fun isRunning(): Boolean {
        if (initialized.get()) {
            return true
        }
        return false
    }

    fun addListener(listener: NetworkListener) {
//        listeners += listener
        log("Adding listener: ${listener.name}")
//        numberOfClients++
    }

    fun removeListener(listener: NetworkListener) {
//        numberOfClients--
        log("Removing listener: ${listener.name}")
//        listeners.remove(listener)
    }

//    fun numberOfListeners() = listeners.size

//    fun keepAlive() {
//        keepAlive.set(true)
//        log("Setting keepAlive")
//    }
//
//    fun clearKeepAlive() {
//        log("Clearing keepalive")
//        keepAlive.set(false)
//    }

    fun log(message: String) {
        println("Networker: $message")
    }

    fun stop(): Boolean {
//        if (keepAlive.get()) {
//            log("Tried to kill networker but kept it alive")
//            return false
//        }
//        if (listeners.isNotEmpty()) {
//            print("Tried to stop networker but clients are still bound: ${listeners.size}")
//            for (client in listeners) {
//                print(", ${client.name}")
//            }
//            println()
//            return false
//        }

        log("Stopping networker")

        messageQueue.clear()
        client.close()
        initialized.set(false)
        clientInitializing.set(false)
        manager.stop()
        return true
    }

    fun run(context: Context) {
        if (initialized.get()) {
            return
        }

        manager = Manager("Client")
        manager.context = context

        Thread {
            try {
                clientInitializing.set(true)
                client = EncodedClient(LOCAL_SERVER_IP, SERVER_PORT, ::onRead)
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
//                println("Sending message: $message")
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
        if (message.topic != Topic.USER_STATUS) {
            log("Received message: $message")
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