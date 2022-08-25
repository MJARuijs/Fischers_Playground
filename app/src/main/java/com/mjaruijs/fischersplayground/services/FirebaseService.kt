package com.mjaruijs.fischersplayground.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_MOVED

class FirebaseService : FirebaseMessagingService() {

    private var serviceMessenger: Messenger? = null
    var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            serviceMessenger = Messenger(service)
            serviceBound = true

//            val registrationMessage = Message.obtain(null, FLAG_REGISTER_CLIENT, activityName)
//            registrationMessage.replyTo = clientMessenger
//            serviceMessenger!!.send(registrationMessage)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            serviceBound = false
        }
    }

    fun sendMessage(flag: Int, data: Any? = null) {
        val message = Message.obtain(null, flag, data)
        serviceMessenger!!.send(message)
    }

    override fun onCreate() {
        super.onCreate()
        bindService(Intent(this, DataManagerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New token was generated: $token")
//        Toast.makeText(this, "Got new token! $token", Toast.LENGTH_SHORT).show()
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
//        super.onMessageReceived(message)
//        Looper.prepare()
//        Toast.makeText(this, "${message.data["topic"]}", Toast.LENGTH_SHORT).show()
        println("Received message:")
        for (entry in message.data) {
            println("${entry.key} : ${entry.value}")
        }
        val topic = message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic..")
        val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")

        if (topic == "move") {
            sendMessage(FLAG_OPPONENT_MOVED, data)
        }
    }

    companion object {

        fun getToken(context: Context): String {
            return context.getSharedPreferences("fcm_token", MODE_PRIVATE).getString("token", "")!!
        }

    }

}