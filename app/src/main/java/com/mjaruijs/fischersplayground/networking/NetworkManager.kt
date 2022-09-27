package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.util.FileManager
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {

    companion object {

        private const val TAG = "NetworkManager"

        private const val PUBLIC_SERVER_IP = "94.208.124.161"
//        private const val PUBLIC_SERVER_IP = "217.101.191.23"
        private const val LOCAL_SERVER_IP = "192.168.178.18"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"

        private const val SERVER_PORT = 4500

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
            Log.i(TAG, "Stopping server connection!")
            clientConnected.set(false)
        }
        clientConnecting.set(false)
        manager.stop()
    }

    fun isConnected(): Boolean {
        return clientConnected.get()
    }

    fun run(context: Context) {
        if (clientConnected.get()) {
            return
        }

        Log.i(TAG, "Starting networker")

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
                Log.w("Networker", "Failed to connect to server..")
                Looper.prepare()
                Toast.makeText(context, "Failed to connect to server..", Toast.LENGTH_SHORT).show()
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
        sendingMessage.set(true)

        Thread {
            while (clientConnecting.get()) {
                Thread.sleep(1)
            }

            if (clientConnected.get()) {
                try {
                    Log.i(TAG, "Sending message: $message")
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {
                    sendCrashReport("network_send_crash.txt", e.stackTraceToString())
                }
            } else {
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
            Log.i(TAG, "Received message: $message")
        }

        val dataManager = DataManager.getInstance(context)

        if (!dataManager.isMessageHandled(message.id)) {
            dataManager.handledMessage(message.id)
            dataManager.lockAndSaveHandledMessages(context)

            val intent = Intent("mjaruijs.fischers_playground")
                .putExtra("topic", message.topic.toString())
                .putExtra("content", messageData)
                .putExtra("messageId", message.id)

            context.sendBroadcast(intent)
        } else {
//            println("Message already handled: ${message.id} ${message.topic}")
        }
    }

    fun sendCrashReport(fileName: String, crashLog: String) {
        Thread {
            try {
                val gameFiles = FileManager.listFilesInDirectory()
                var allData = ""
                val crashContent = trimCrashReport(crashLog)

                allData += "$fileName|$crashContent\\\n"

                for (gameFile in gameFiles) {
                    val file = FileManager.getFile(gameFile)
                    if (file.exists()) {
                        val fileContent = file.readText()
                        allData += if (gameFile.endsWith("_crash.txt")) {
                            val compressedContent = trimCrashReport(fileContent)
                            "$gameFile|$compressedContent\\\n"
                        } else {
                            "$gameFile|$fileContent\\\n"
                        }
                    }
                }

                sendMessage(NetworkMessage(Topic.CRASH_REPORT, allData))
            } catch (e: Exception) {

            } finally {
                val crashFile = FileManager.getFile(fileName)
                crashFile.writeText(crashLog)
            }
        }.start()
    }

    private fun trimCrashReport(crashLog: String): String {
        var crashContent = ""

        for ((i, line) in crashLog.split('\n').withIndex()) {
            if (!line.trim().startsWith("at com.mjaruijs") && i != 0) {
                break
            } else {
                crashContent += "$line\n"
            }
        }
        return crashContent
    }


}