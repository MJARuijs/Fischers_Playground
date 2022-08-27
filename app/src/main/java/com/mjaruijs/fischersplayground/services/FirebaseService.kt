package com.mjaruijs.fischersplayground.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.util.FileManager

class FirebaseService : FirebaseMessagingService() {

//    private var serviceMessenger: Messenger? = null
//    var serviceBound = false

    private var currentNotificationId = 0

//    private val connection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
//            serviceMessenger = Messenger(service)
//            serviceBound = true
//
////            val registrationMessage = Message.obtain(null, FLAG_REGISTER_CLIENT, activityName)
////            registrationMessage.replyTo = clientMessenger
////            serviceMessenger!!.send(registrationMessage)
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            serviceMessenger = null
//            serviceBound = false
//        }
//    }

//    fun sendMessage(flag: Int, data: Any? = null) {
//        val message = Message.obtain(null, flag, data)
//        serviceMessenger!!.send(message)
//    }

//    override fun onCreate() {
//        super.onCreate()
//
//        Toast.makeText(applicationContext, "Created firebase!", Toast.LENGTH_SHORT).show()
////        val intent = Intent(this, DataManagerService::class.java)
////        intent.putExtra("caller", "Firebase")
////        bindService(intent, connection, Context.BIND_AUTO_CREATE)
//    }

//    override fun onDestroy() {
//        Toast.makeText(applicationContext, "Destroyed firebase!", Toast.LENGTH_SHORT).show()
//        super.onDestroy()
//    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New token was generated: $token")
//        Toast.makeText(this, "Got new token! $token", Toast.LENGTH_SHORT).show()
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val topic = message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic..")
            val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")
            val dataList = data.split('|')

            val extraData = HashMap<String, String>()

            val notificationMessage = when (topic) {
                "new_game" -> {
                    "New game!"
                }
                "move" -> {
                    extraData["game_id"] = dataList[0]
                    extraData["move_notation"] = dataList[1]
                    "Your move!"
                }
                "invite" -> {
                    "New invite"
                }
                "request_undo" -> {
                    "Opponent is requesting to undo their move!"
                }
                else -> { "" }
            }

            val dataServiceIntent = Intent(this, DataManagerService::class.java)
            dataServiceIntent.putExtra("topic", topic)
            dataServiceIntent.putExtra("data", data)
            startForegroundService(dataServiceIntent)

            sendNotification(notificationMessage, extraData)
//            FileManager.write(applicationContext, "firebase_data.txt", "$data\n")
        } catch (e: Exception) {
            FileManager.write(applicationContext, "firebase_crash_log.txt", e.stackTraceToString())
        }
    }

    private fun sendNotification(message: String, extraData: HashMap<String, String>) {
        val intent = Intent(applicationContext, MainActivity::class.java)

        for (data in extraData) {
            intent.putExtra(data.key, data.value)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.black_queen)
            .setContentTitle("Title")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Opponent moved", IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(currentNotificationId++, notificationBuilder.build())
    }

    companion object {

        fun getToken(context: Context): String {
            return context.getSharedPreferences("fcm_token", MODE_PRIVATE).getString("token", "")!!
        }

    }

}