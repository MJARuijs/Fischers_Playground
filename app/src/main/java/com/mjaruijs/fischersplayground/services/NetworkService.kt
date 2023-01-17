package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.os.Build.VERSION_CODES.TIRAMISU
import android.widget.Toast
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.networking.ConnectivityCallback
import com.mjaruijs.fischersplayground.networking.client.SecureClient
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.networking.nio.Manager
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.MyTimerTask
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class NetworkService : Service() {

    companion object {
        private const val TAG = "NetworkService"

//        const val PUBLIC_SERVER_IP = "94.208.124.161"
        private const val PUBLIC_SERVER_IP = "145.89.4.144"
        const val LOCAL_SERVER_IP = "192.168.178.103"
//        private const val LOCAL_SERVER_IP = "10.248.59.63"

        const val SERVER_PORT = 4502

        private var instance: NetworkService? = null

        fun sendCrashReport(fileName: String, crashLog: String, context: Context?) {
            Logger.error("ERROR_HANDLER", crashLog)
            val crashFile = FileManager.getFile(fileName)
            crashFile.writeText(crashLog)
            if (instance == null) {
                instance = NetworkService()
            }

            if (instance!!.isRunning()) {
                Logger.debug(TAG, "Trying to send crash report")
                instance!!.sendCrashReport(fileName, crashLog, context)
            } else {
                Logger.debug(TAG, "Trying to send crash report but instance is not running")
            }
        }
    }

    private var currentClient: Messenger? = null
    private val serverMessageCache = ArrayList<NetworkMessage>()
    private val systemMessageCache = ArrayList<NetworkMessage>()

    private val clientConnecting = AtomicBoolean(false)
    private val clientConnected = AtomicBoolean(false)
    private val sendingMessage = AtomicBoolean(false)
    private val clientStopping = AtomicBoolean(false)

    private lateinit var manager: Manager
    private lateinit var client: SecureClient
    private lateinit var serviceMessenger: Messenger

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var connectivityCallback: ConnectivityCallback

    private val messageLock = AtomicBoolean(false)

    private fun acquireMessageLock() {
        while (areMessagesLocked()) {
            Thread.sleep(1)
        }
        messageLock.set(true)
    }

    private fun areMessagesLocked() = messageLock.get()


    private fun addMessage(message: NetworkMessage) {
        acquireMessageLock()
        serverMessageCache += message
        messageLock.set(false)
    }

    private fun isConnected() = clientConnected.get()

    private fun isRunning() = clientConnected.get() || clientConnecting.get()

    override fun onCreate() {
        super.onCreate()
        Logger.debug(TAG, "Creating NetworkService")

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

//        if (!isConnected()) {
        connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityCallback = ConnectivityCallback(::onNetworkAvailable, ::onNetworkLost)
        connectivityManager.requestNetwork(networkRequest, connectivityCallback)
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.debug(TAG, "Service Started")
        if (intent != null) {
            val message = if (Build.VERSION.SDK_INT < TIRAMISU) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("message")
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
        Logger.warn(TAG, "Binding service")
        serviceMessenger = Messenger(IncomingHandler(this))
        return serviceMessenger.binder
    }

    private fun onNetworkAvailable() {
//        if (!leftApp) {
        Logger.warn(TAG, "Network available!")

        if (!isRunning()) {
            run()

            val userId = getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, DEFAULT_USER_ID)!!
            if (userId != DEFAULT_USER_ID) {
                sendMessageToServer(NetworkMessage(Topic.ID_LOGIN, userId))
            }
        }
    }

    private fun onNetworkLost() {
//        if (!leftApp) {
            Logger.warn(TAG, "Network Lost")
            stop()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
        try {
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
        } catch (e: Exception) {

        }
        Logger.warn(TAG, "Destroying NetworkService")
    }

    private fun sendMessageToSystem(data: Any?) {
        if (currentClient == null) {
            systemMessageCache += data as NetworkMessage
            Logger.error(TAG, "Tried to send message to client, but none are connected")
            return
        }
        currentClient!!.send(Message.obtain(null, 0, data))
    }

    private fun run() {
        if (clientConnected.get() || clientConnecting.get()) {
            return
        }

        clientStopping.set(false)

        manager = Manager("Client")
        manager.context = applicationContext
        manager.setOnClientDisconnect {
            Logger.warn(TAG, "OnClientDisconnected")
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
                Toast.makeText(applicationContext, "Failed to connect to server..", Toast.LENGTH_SHORT).show()
                clientConnected.set(false)
                sendingMessage.set(false)
            } finally {
                clientConnecting.set(false)
            }

            if (clientConnected.get()) {
                Thread(manager).start()
                manager.register(client)

                for (message in serverMessageCache) {
                    sendMessageToServer(message)
                    Thread.sleep(10)
                }
                serverMessageCache.clear()
            }
        }.start()
    }

    private fun sendMessageToServer(message: NetworkMessage) {
        Thread {
            while (clientConnecting.get() || sendingMessage.get()) {
                Thread.sleep(1)
            }

            if (clientConnected.get()) {
                try {
                    sendingMessage.set(true)
                    client.write(message.toString())

                    if (message.topic != Topic.CONFIRM_MESSAGE && message.topic != Topic.CRASH_REPORT) {
                        Logger.info(TAG, "Sending message: $message")
                    }

                    val timerTask = MyTimerTask {
                        sendingMessage.set(false)
//                        Logger.debug(TAG, "Releasing lock ${message.topic}")
                    }

                    val timer = Timer()
                    timer.schedule(timerTask, 10)
                } catch (e: Exception) {
                    Logger.error(TAG, e.stackTraceToString())
                }
            } else {
                Logger.warn(TAG, "Client not connected to server; ${message.topic} message added to queue")
                addMessage(message)
            }

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

        val dataManager = DataManager.getInstance(applicationContext)

        if (!dataManager.isMessageHandled(message.id)) {
            dataManager.setMessageHandled(message.id, applicationContext)
            dataManager.saveHandledMessages(applicationContext)

            Logger.debug(TAG, "Got message from server: ${message.topic}")

            sendMessageToSystem(message)
        }
    }

    private fun stop() {
//        Thread {
        if (clientStopping.get()) {
//            return
        }
        clientStopping.set(true)

//        while (sendingMessage.get()) {
//            Logger.warn(TAG, "Trying to stop but waiting for SendMessage")
//            Thread.sleep(1)
//        }

        serverMessageCache.clear()
//        if (clientConnected.get()) {
            client.close()
            clientConnected.set(false)
//        }
        clientConnecting.set(false)
        manager.stop()
        Logger.warn(TAG, "Network connection stopped")
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

                sendMessageToServer(NetworkMessage(Topic.CRASH_REPORT, allData))
            } catch (e: Exception) {
                throw e
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

    class IncomingHandler(val service: NetworkService): Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            service.currentClient = msg.replyTo

            if (service.systemMessageCache.isNotEmpty()) {
                for (message in service.systemMessageCache) {
                    msg.replyTo.send(Message.obtain(null, 0, message))
                }
                service.systemMessageCache.clear()
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