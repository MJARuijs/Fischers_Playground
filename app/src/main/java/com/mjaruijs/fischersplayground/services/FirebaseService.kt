package com.mjaruijs.fischersplayground.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_INVITE
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_MOVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_REQUESTED

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
        println("Received Firebase message:")
        for (entry in message.data) {
            println("${entry.key} : ${entry.value}")
        }
        val topic = message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic..")
        val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")

        when (topic) {
            "new_game" -> sendMessage(FLAG_NEW_GAME, data)
            "move" -> sendMessage(FLAG_OPPONENT_MOVED, data)
            "invite" -> sendMessage(FLAG_NEW_INVITE, data)
            "request_undo" -> sendMessage(FLAG_UNDO_REQUESTED, data)
        }

//        val content = getSharedPreferences("test", MODE_PRIVATE).getString("value", "")
//        getSharedPreferences("test", MODE_PRIVATE).edit().putString("value", "$content|1").apply()

        sendNotification()
    }

    private fun sendNotification() {

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.black_queen)
            .setContentTitle("Title")
            .setContentText("Content")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {

        fun getToken(context: Context): String {
            return context.getSharedPreferences("fcm_token", MODE_PRIVATE).getString("token", "")!!
        }

    }

}