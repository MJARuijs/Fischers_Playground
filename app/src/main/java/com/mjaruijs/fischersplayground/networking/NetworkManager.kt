package com.mjaruijs.fischersplayground.networking

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.widget.Toast
import com.mjaruijs.fischersplayground.networking.client.SecureClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.util.concurrent.atomic.AtomicBoolean

class NetworkManager {

    companion object {

        private const val TAG = "NetworkManager"

        private const val PUBLIC_SERVER_IP = "94.208.124.161"
//        private const val PUBLIC_SERVER_IP = "10.248.59.222"
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
    private lateinit var client: SecureClient

    private val messageQueue = ArrayList<NetworkMessage>()

    fun stop() {
        Thread {
            while (sendingMessage.get()) {
                Logger.warn(TAG, "Trying to stop but waiting for SendMessage")
                Thread.sleep(1)
            }

            messageQueue.clear()
            if (clientConnected.get()) {
                client.close()
                clientConnected.set(false)
            }
            clientConnecting.set(false)
            manager.stop()
        }.start()
    }

    fun isRunning(): Boolean {
        return clientConnected.get() || clientConnecting.get()
    }

    fun isConnected(): Boolean {
        return clientConnected.get()
    }

    fun run(context: Context) {
        if (clientConnected.get() || clientConnecting.get()) {
            return
        }

        manager = Manager("Client")
        manager.context = context
        manager.setOnClientDisconnect {
            stop()
        }

        Thread {
            Logger.warn(TAG, "Trying to connect to server..")

            try {
                clientConnecting.set(true)
                client = SecureClient(LOCAL_SERVER_IP, SERVER_PORT, ::onRead)
                clientConnected.set(true)
                Logger.warn(TAG, "Connected to server..")
            } catch (e: Exception) {
                Logger.warn(TAG, "Failed to connect to server..")
                Looper.prepare()
                Toast.makeText(context, "Failed to connect to server..", Toast.LENGTH_SHORT).show()
                clientConnected.set(false)
                sendingMessage.set(false)
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
            sendingMessage.set(true)

            if (clientConnected.get()) {
                try {
                    if (message.topic != Topic.CONFIRM_MESSAGE && message.topic != Topic.CRASH_REPORT) {
                        Logger.info(TAG, "Sending message: $message")
                    }
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {
                    sendCrashReport("crash_network_send.txt", e.stackTraceToString(), null)
                } finally {
                    sendingMessage.set(false)
                }
            } else {
                Logger.warn(TAG, "Client not connected; ${message.topic} message added to queue")
                messageQueue += message
            }

            sendingMessage.set(false)
        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {
        if (message.topic == Topic.HEART_BEAT) {
            sendMessage(NetworkMessage(Topic.HEART_BEAT, ""))
            return
        }

        sendMessage(NetworkMessage(Topic.CONFIRM_MESSAGE, "", message.id))

        if (message.topic != Topic.CONFIRM_MESSAGE) {
            Logger.info(TAG, "Received message: $message")
        }

        val dataManager = DataManager.getInstance(context)

        if (!dataManager.isMessageHandled(message.id)) {
            dataManager.handledMessage(message.id)
            dataManager.lockAndSaveHandledMessages(context)

            val intent = Intent("mjaruijs.fischers_playground")
                .putExtra("topic", message.topic.toString())
                .putExtra("content", message.content)
                .putExtra("messageId", message.id)

            context.sendBroadcast(intent)
        } else {
//            Logger.debug(TAG, "Message already handled: ${message.id} ${message.topic}")
        }
    }

    fun sendCrashReport(fileName: String, crashLog: String, context: Context?) {
        if (context != null) {
            try {
                Looper.prepare()
            } catch (e: Exception) {
                Logger.warn(TAG, e.stackTraceToString())
            }
            Toast.makeText(context, "A crash occurred!", Toast.LENGTH_SHORT).show()
        }
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
                        allData += if (gameFile.startsWith("crash_")) {
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