package com.mjaruijs.fischersplayground.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.notification.NotificationBuilder

class FirebaseService : FirebaseMessagingService() {

    private var currentNotificationId = 0

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New token was generated: $token")
//        Toast.makeText(this, "Got new token! $token", Toast.LENGTH_SHORT).show()
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val dataManager = DataManager.getInstance(applicationContext)

//            NetworkManager.keepAlive()

            val topic = message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic..")
            val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")
            val dataList = data.split('|').toTypedArray()

//            val extraData = HashMap<String, String>()
//            extraData["news_topic"] = topic

            val notificationData = dataManager.createNotificationData(topic, dataList)

            val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic),
                    Pair("data", dataList)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)

            NotificationBuilder.build(applicationContext, notificationData)
        } catch (e: Exception) {
            FileManager.write(applicationContext, "firebase_crash_log.txt", e.stackTraceToString())
        }
    }

//    private fun createIntent(topic: String, data: List<String>): PendingIntent {
//        return when (topic) {
//            "move" -> createMultiplayerActivityIntent(data)
//            "new_game" -> createMultiplayerActivityIntent(data)
//            "invite" -> createMainActivityIntent(data)
//            else -> throw IllegalArgumentException("Could not create notification for topic: $topic")
//        }
//    }

//    private fun createMainActivityIntent(data: List<String>): PendingIntent {
//        val intent = Intent(applicationContext, MainActivity::class.java)
//        intent.putExtra("opponent_name", data[0])
//        intent.putExtra("invite_id", data[1])
//        return PendingIntent.getActivity(applicationContext, 0, intent, FLAG_UPDATE_CURRENT)
//    }
//
//    private fun createMultiplayerActivityIntent(data: List<String>): PendingIntent {
//        val intent = Intent(applicationContext, MultiplayerGameActivity::class.java)
//        intent.putExtra("game_id", data[0])
//
//        val stackBuilder = TaskStackBuilder.create(applicationContext)
//        stackBuilder.addParentStack(MultiplayerGameActivity::class.java)
//        stackBuilder.addNextIntent(intent)
//        return stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)
//    }

    private fun createIntent(topic: String, extraData: HashMap<String, String>): PendingIntent? {
        return when (topic) {
            "move" -> {
                val intent = Intent(applicationContext, MultiplayerGameActivity::class.java)

                for (data in extraData) {
                    intent.putExtra(data.key, data.value)
                }

                val stackBuilder = TaskStackBuilder.create(applicationContext)
                stackBuilder.addParentStack(MultiplayerGameActivity::class.java)
                stackBuilder.addNextIntent(intent)
                stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)
            }
            "invite" -> {
                val intent = Intent(applicationContext, MainActivity::class.java)

                for (data in extraData) {
                    intent.putExtra(data.key, data.value)
                }

                PendingIntent.getActivity(applicationContext, 0, intent, FLAG_UPDATE_CURRENT)
            }
            else -> null
        }
    }

    private fun sendNotification(message: String, onClickIntent: PendingIntent) {
        try {
            val channelId = getString(R.string.default_notification_channel_id)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.black_queen)
                .setContentTitle("Title")
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(onClickIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Opponent moved", IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(currentNotificationId++, notificationBuilder.build())
        } catch (e: Exception){
            Logger.log(applicationContext, e.stackTraceToString(), "fire_base_crash_log.txt")
        }

    }

}