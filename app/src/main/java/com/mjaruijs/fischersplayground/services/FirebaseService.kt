package com.mjaruijs.fischersplayground.services

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.notification.NotificationBuilder.Companion.MOVE_CHANNEL_ID
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger

class FirebaseService : FirebaseMessagingService() {

    init {
//        Logger.log(applicationContext, "LOL", "lol.txt")
//        throw IllegalArgumentException("LOL")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New token was generated: $token")
//        Toast.makeText(this, "Got new token! $token", Toast.LENGTH_SHORT).show()
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

//    override fun onDestroy() {
//        Toast.makeText(applicationContext, "Firebase destroyed", Toast.LENGTH_SHORT).show()
//        super.onDestroy()
//    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic..")
            val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")
            val dataList = data.split('|').toTypedArray()

//            val extraData = HashMap<String, String>()
//            extraData["news_topic"] = topic

            Logger.log(applicationContext,"Got firebaseMessage: $topic $data")

            val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic),
                    Pair("data", dataList)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)

            val notificationData = notificationBuilder.createNotificationData(applicationContext, topic, dataList)
            val notification = notificationBuilder.build(applicationContext, false, notificationData)
            notificationBuilder.notify(notification)

            val summaryNotification = notificationBuilder.build(applicationContext, true, "Title??", "Message!", MOVE_CHANNEL_ID, null)
            notificationBuilder.notify(0, summaryNotification)

            Logger.log(applicationContext, "got to end of FireBase")
        } catch (e: Exception) {
            FileManager.write(applicationContext, "firebase_crash_log.txt", e.stackTraceToString())
        }
    }
}