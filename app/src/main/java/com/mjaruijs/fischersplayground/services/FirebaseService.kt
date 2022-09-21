package com.mjaruijs.fischersplayground.services

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.notification.NotificationBuilder.Companion.GROUP_CHANNEL_ID
import com.mjaruijs.fischersplayground.util.FileManager

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = Topic.fromString(message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic.."))
            val content = message.data["content"] ?: throw IllegalArgumentException("No data was found with name: content..")
            val messageId = message.data["id"]?.toLong() ?: throw IllegalArgumentException("No data was found with name: id..")

            val contentList = content.split('|').toTypedArray()

            val dataManager = DataManager.getInstance(applicationContext)
            if (dataManager.handledMessages.contains(messageId)) {
                return
            }

            val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("content", contentList),
                    Pair("messageId", messageId)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)

            val notificationData = notificationBuilder.createNotificationData(applicationContext, topic, contentList) ?: return
            val notification = notificationBuilder.build(applicationContext, false, notificationData)
            notificationBuilder.notify(notification)

            val summaryNotification = notificationBuilder.build(applicationContext, true, "Title??", "Message!", GROUP_CHANNEL_ID, null)
            notificationBuilder.notify(0, summaryNotification)
        } catch (e: Exception) {
            FileManager.write(applicationContext, "firebase_crash_log.txt", e.stackTraceToString())
        }
    }

}