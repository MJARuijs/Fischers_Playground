package com.mjaruijs.fischersplayground.services

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_PREFERENCE_FILE
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.notification.NotificationBuilder.Companion.GROUP_CHANNEL_ID
import com.mjaruijs.fischersplayground.util.FileManager

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New token was generated: $token")
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = Topic.fromString(message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic.."))
            val data = message.data["data"] ?: throw IllegalArgumentException("No data was found with name: data..")

            if (topic == Topic.RECONNECT_TO_SERVER) {
                reconnectToServer(data)
                return
            }

            val dataList = data.split('|').toTypedArray()

//            Logger.log(applicationContext,"Got firebaseMessage: $topic $data")

            val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("data", dataList)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)

            val notificationData = notificationBuilder.createNotificationData(applicationContext, topic, dataList)
            val notification = notificationBuilder.build(applicationContext, false, notificationData)
            notificationBuilder.notify(notification)

            val summaryNotification = notificationBuilder.build(applicationContext, true, "Title??", "Message!", GROUP_CHANNEL_ID, null)
            notificationBuilder.notify(0, summaryNotification)

//            Logger.log(applicationContext, "got to end of FireBase")
        } catch (e: Exception) {
            FileManager.write(applicationContext, "firebase_crash_log.txt", e.stackTraceToString())
        }
    }

    private fun reconnectToServer(data: String) {
        val serverData = data.split('|')
        val address = serverData[0]
        val port = serverData[1].toInt()

        val networkManager = NetworkManager.getInstance()
        networkManager.run(applicationContext, address, port)

        val userId = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, DEFAULT_USER_ID)!!
        if (userId != DEFAULT_USER_ID) {
            networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
        }
    }
}