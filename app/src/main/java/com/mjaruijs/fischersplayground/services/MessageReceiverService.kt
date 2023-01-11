package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import com.mjaruijs.fischersplayground.activities.MessageReceiver
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import java.lang.ref.WeakReference

class MessageReceiverService : Service() {

    companion object {
        private const val TAG = "MessageReceiverService"
    }

    private val networkReceiver = MessageReceiver(::onMessageReceived)
    private val intentFilter = IntentFilter("mjaruijs.fischers_playground")

    private var currentClient: Messenger? = null

    private lateinit var serviceMessenger: Messenger

    private val messageCache = ArrayList<NetworkMessage>()

    override fun onCreate() {
        super.onCreate()
        registerReceiver(networkReceiver, intentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        serviceMessenger = Messenger(IncomingHandler(this))
        return serviceMessenger.binder
    }

    private fun onMessageReceived(topic: Topic, content: String, messageId: Long) {
        sendMessage(NetworkMessage(topic, content, messageId))
    }

    private fun sendMessage(data: Any?) {
        if (currentClient == null) {
            messageCache += data as NetworkMessage
            return
        }
        currentClient!!.send(Message.obtain(null, 0, data))
    }

    class IncomingHandler(service: MessageReceiverService): Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = serviceReference.get()!!
            service.currentClient = msg.replyTo

            if (service.messageCache.isNotEmpty()) {
                for (message in service.messageCache) {
//                    Logger.warn(TAG, "Sending backed up message: $message")
                    msg.replyTo.send(Message.obtain(null, 0, message))
                }
                service.messageCache.clear()
            }

        }

    }
}