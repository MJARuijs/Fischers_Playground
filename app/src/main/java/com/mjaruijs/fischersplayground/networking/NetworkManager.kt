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
import com.mjaruijs.fischersplayground.util.MyTimerTask
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class NetworkManager {

    companion object {

        private const val TAG = "NetworkManager"

        const val PUBLIC_SERVER_IP = "80.114.20.54"
//        private const val PUBLIC_SERVER_IP = "10.248.59.222"
        const val LOCAL_SERVER_IP = "192.168.178.101"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"

        const val SERVER_PORT = 4500

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
//    private val stopping = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: SecureClient

    private val messageQueue = ArrayList<NetworkMessage>()
    private val messageLock = AtomicBoolean(false)

    private fun acquireMessageLock() {
        while (areMessagesLocked()) {
            Thread.sleep(1)
        }
        messageLock.set(true)
    }

    private fun areMessagesLocked() = messageLock.get()

    private fun getMessages(): Iterator<NetworkMessage> {
        acquireMessageLock()
        val messages = messageQueue.iterator()
        messageLock.set(false)
        return messages
    }

    private fun addMessage(message: NetworkMessage) {
        acquireMessageLock()
        messageQueue += message
        messageLock.set(false)
    }

    fun stop() {
//        Thread {
//            if (stopping.get()) {
//                return@Thread
//            }
//            stopping.set(true)

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
            Logger.debug(TAG, "Stopped networker")
//        }.start()
    }

    fun isRunning(): Boolean {
//        return (clientConnected.get() || clientConnecting.get()) && !stopping.get()
        return (clientConnected.get() || clientConnecting.get())
    }

    fun isConnected(): Boolean {
        return clientConnected.get()
    }

    fun run(context: Context) {
        if (isRunning()) {
            Logger.debug(TAG, "run() was called, but returned because: clientConnected=${clientConnected.get()}, clientConnecting=${clientConnecting.get()}")
            return
        }

//        stopping.set(false)

        manager = Manager("Client")
        manager.context = context
        manager.setOnClientDisconnect {
            stop()
        }

        Thread {
            Logger.warn(TAG, "Trying to connect to server..")

            try {
                clientConnecting.set(true)
                client = SecureClient(PUBLIC_SERVER_IP, SERVER_PORT, ::onRead)
                clientConnected.set(true)
                Logger.warn(TAG, "Connected to server..")
            } catch (e: Exception) {
                Logger.warn(TAG, "Failed to connect to server.. ${e.stackTraceToString()}")
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

//                Logger.debug(TAG, "Number of cached messages: ${messageQueue.size}")

                for (message in messageQueue) {
                    sendMessage(message)
                    Thread.sleep(10)
                }
                messageQueue.clear()
            }
        }.start()
    }

    fun sendMessage(message: NetworkMessage) {
        Thread {
//            Logger.debug(TAG, "Checking locks ${message.topic} ${Thread.currentThread().id} ${sendingMessage.get()}")
            while (clientConnecting.get() || sendingMessage.get()) {
//                Logger.debug(TAG, "Trying to send message about ${message.topic} but shit is locked ${clientConnecting.get()} ${sendingMessage.get()}")
                Thread.sleep(1)
            }

            if (clientConnected.get()) {
                try {
//                    Logger.debug(TAG, "Setting message lock ${message.topic}")
                    sendingMessage.set(true)

                    client.write(message)
                    if (message.topic != Topic.CONFIRM_MESSAGE && message.topic != Topic.CRASH_REPORT) {
                        Logger.info(TAG, "Sending message: $message")
                    }

                    val timerTask = MyTimerTask {
                        sendingMessage.set(false)
                        Logger.debug(TAG, "Releasing lock ${message.topic}")
                    }

                    val timer = Timer()
                    timer.schedule(timerTask, 150)
                } catch (e: Exception) {
                    sendCrashReport("crash_network_send.txt", e.stackTraceToString(), null)
                }
            } else {
                Logger.warn(TAG, "Client not connected; ${message.topic} message added to queue")
                addMessage(message)
            }

        }.start()

    }

    private fun onRead(message: NetworkMessage, context: Context) {
        if (message.topic == Topic.HEART_BEAT) {
            sendMessage(NetworkMessage(Topic.HEART_BEAT, ""))
            return
        }

        if (message.topic != Topic.CONFIRM_MESSAGE) {
            Logger.info(TAG, "Received message: $message")
        }

        sendMessage(NetworkMessage(Topic.CONFIRM_MESSAGE, "", message.id))

        val dataManager = DataManager.getInstance(context)

        if (!dataManager.isMessageHandled(message.id)) {
            dataManager.setMessageHandled(message.id, context)

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