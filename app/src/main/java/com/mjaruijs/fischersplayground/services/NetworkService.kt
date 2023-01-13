package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.os.Build.VERSION_CODES.TIRAMISU
import android.util.Log
import android.widget.Toast
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.networking.client.SecureClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class NetworkService : Service() {

    companion object {
        private const val TAG = "NetworkService"

        const val PUBLIC_SERVER_IP = "94.208.124.161"
        //        private const val PUBLIC_SERVER_IP = "10.248.59.222"
        const val LOCAL_SERVER_IP = "192.168.178.18"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"

        const val SERVER_PORT = 4500

//        private var instance: NetworkService? = null
//
//        fun sendMessage(message: NetworkMessage) {
//            if (instance == null) {
//                instance = NetworkService()
//            }
//
//            if (instance!!.isRunning()) {
//                instance!!.sendMessageToServer(message)
//            }
//        }
//
        fun sendCrashReport(fileName: String, crashLog: String, context: Context?) {
            Logger.error("ERROR_HANDLER", crashLog)
//            if (instance == null) {
//                instance = NetworkService()
//            }
//
//            if (instance!!.isRunning()) {
//                Logger.debug(TAG, "Trying to send crash report")
//                instance!!.sendCrashReport(fileName, crashLog, context)
//            } else {
//                Logger.debug(TAG, "Trying to send crash report but instance is not running")
//            }
        }
    }

//    private lateinit var networkManager: NetworkManager

    private var currentClient: Messenger? = null
    private val messageQueue = ArrayList<NetworkMessage>()
    private val messageCache = ArrayList<NetworkMessage>()

    private val clientConnecting = AtomicBoolean(false)
    private val clientConnected = AtomicBoolean(false)
    private val sendingMessage = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: SecureClient
    private lateinit var serviceMessenger: Messenger

    override fun onCreate() {
        super.onCreate()
        Logger.debug(TAG, "Creating NetworkService")
        run(applicationContext)

//        instance = this

        val userId = getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, ClientActivity.DEFAULT_USER_ID)!!
        if (userId != DEFAULT_USER_ID) {
            sendMessageToServer(NetworkMessage(Topic.ID_LOGIN, userId))
        }
//        networkManager = NetworkManager.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val message = if (Build.VERSION.SDK_INT < TIRAMISU) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<NetworkMessage>("message")
            } else {
                intent.getParcelableExtra("message", NetworkMessage::class.java)
            }

            if (message != null) {
                sendMessageToServer(message)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        serviceMessenger = Messenger(IncomingHandler(this))
        return serviceMessenger.binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
        Logger.warn(TAG, "Destroying NetworkService")
    }

    fun isRunning(): Boolean {
        return clientConnected.get() || clientConnecting.get()
    }

    private fun sendMessageToSystem(data: Any?) {
        if (currentClient == null) {
            messageCache += data as NetworkMessage
            Logger.error(TAG, "Tried to send Message to client, but was null")
            return
        }
        currentClient!!.send(Message.obtain(null, 0, data))
    }

    private fun run(context: Context) {
        if (clientConnected.get() || clientConnecting.get()) {
            return
        }

        manager = Manager("Client")
        manager.context = applicationContext
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
                Toast.makeText(applicationContext, "Failed to connect to server..", Toast.LENGTH_SHORT).show()
                clientConnected.set(false)
                sendingMessage.set(false)
            } finally {
                clientConnecting.set(false)
            }

            if (clientConnected.get()) {
                Thread(manager).start()
                manager.register(client)

                for (message in messageQueue) {
                    sendMessageToServer(message)
                }
                messageQueue.clear()
            }
        }.start()
    }

    private fun sendMessageToServer(message: NetworkMessage) {
        Thread {
            while (clientConnecting.get()) {
                Thread.sleep(1)
            }
            sendingMessage.set(true)

//            if (clientConnected.get()) {
                try {
                    if (message.topic != Topic.CONFIRM_MESSAGE && message.topic != Topic.CRASH_REPORT) {
                        Logger.info(TAG, "Sending message: $message")
                    }
                    client.write(message.toString())
                    messageQueue.remove(message)
                } catch (e: Exception) {
                    Logger.error(TAG, e.stackTraceToString())
                    Logger.warn(TAG, "Client not connected; ${message.topic} message added to queue")
                    messageQueue += message
//                    sendCrashReport("crash_network_send.txt", e.stackTraceToString(), null)
                } finally {
                    sendingMessage.set(false)
                }
//            } else {

//            }

            sendingMessage.set(false)
        }.start()
    }

    private fun onRead(message: NetworkMessage, context: Context) {
        if (message.topic == Topic.HEART_BEAT) {
            sendMessageToServer(NetworkMessage(Topic.HEART_BEAT, ""))
            return
        }

        sendMessageToServer(NetworkMessage(Topic.CONFIRM_MESSAGE, "", message.id))

        if (message.topic != Topic.CONFIRM_MESSAGE) {
            Logger.info(TAG, "Received message: $message")
        }

//        val dataManager = DataManager.getInstance(applicationContext)

//        val intent = Intent(context, DataManagerService::class.java)

//        if (!dataManager.isMessageHandled(message.id)) {
//            dataManager.handledMessage(message.id)
//            dataManager.lockAndSaveHandledMessages(applicationContext)

            sendMessageToSystem(message)
//            val intent = Intent("mjaruijs.fischers_playground")
//                .putExtra("topic", message.topic.toString())
//                .putExtra("content", message.content)
//                .putExtra("messageId", message.id)
//
//            context.sendBroadcast(intent)
//        }
    }

    private fun stop() {
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

//    fun sendCrashReport(fileName: String, crashLog: String, context: Context?) {
//        if (context != null) {
//            try {
//                Looper.prepare()
//            } catch (e: Exception) {
//                Logger.warn(TAG, e.stackTraceToString())
//            }
//            Toast.makeText(context, "A crash occurred!", Toast.LENGTH_SHORT).show()
//        }
//
////        Thread {
//            try {
//                val gameFiles = FileManager.listFilesInDirectory()
//                var allData = ""
//                val crashContent = trimCrashReport(crashLog)
//
//                allData += "$fileName|$crashContent\\\n"
//
//                for (gameFile in gameFiles) {
//                    val file = FileManager.getFile(gameFile)
//                    if (file.exists()) {
//                        val fileContent = file.readText()
//                        allData += if (gameFile.startsWith("crash_")) {
//                            val compressedContent = trimCrashReport(fileContent)
//                            "$gameFile|$compressedContent\\\n"
//                        } else {
//                            "$gameFile|$fileContent\\\n"
//                        }
//                    }
//                }
//
//                sendMessage(NetworkMessage(Topic.CRASH_REPORT, allData))
//            } catch (e: Exception) {
//                throw e
//            } finally {
//                val crashFile = FileManager.getFile(fileName)
//                crashFile.writeText(crashLog)
//            }
////        }.start()
//    }

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

    class IncomingHandler(service: NetworkService): Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = serviceReference.get()!!
            service.currentClient = msg.replyTo

            if (service.messageCache.isNotEmpty()) {
                for (message in service.messageCache) {
                    msg.replyTo.send(Message.obtain(null, 0, message))
                }
                service.messageCache.clear()
            }

            if (msg.what == 1) {
                val networkMessage = msg.obj as NetworkMessage
                service.sendMessageToServer(networkMessage)
            } else if (msg.what == 2) {
                service.stop()
            }
        }

    }
}