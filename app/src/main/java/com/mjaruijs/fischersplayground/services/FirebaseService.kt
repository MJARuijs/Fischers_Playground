package com.mjaruijs.fischersplayground.services

import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.notification.NotificationBuilder.Companion.GROUP_CHANNEL_ID

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            Toast.makeText(applicationContext, "GOTTEM", Toast.LENGTH_SHORT).show()

            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = Topic.fromString(message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic.."))
            val content = message.data["content"] ?: throw IllegalArgumentException("No data was found with name: content..")
            val messageId = message.data["id"]?.toLong() ?: throw IllegalArgumentException("No data was found with name: id..")

            val contentList = content.split('|').toTypedArray()


            val dataManager = DataManager.getInstance(applicationContext)
//            val dataManager = DataManager(applicationContext)
            if (dataManager.isMessageHandled(messageId, "Firebase")) {
                println("Got firebase message but was already handled: $topic $messageId")
                return
            }
            println("Got firebase message: $topic $messageId")

            val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("content", contentList),
                    Pair("messageId", messageId)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)
//
//            applicationContext.run {
//                workManager.getWorkInfoByIdLiveData(worker.id)
//                    .observe() {
//                        if (it != null && it.state.isFinished) {
//
//                        }
//                    }
//            }

            val notificationData = notificationBuilder.createNotificationData(applicationContext, topic, contentList) ?: return
            val notification = notificationBuilder.build(applicationContext, false, notificationData)
            notificationBuilder.notify(notification)

            val summaryNotification = notificationBuilder.build(applicationContext, true, "Title??", "Message!", GROUP_CHANNEL_ID, null)
            notificationBuilder.notify(0, summaryNotification)

        } catch (e: Exception) {
            NetworkManager.getInstance().sendCrashReport("firebase_crash.txt", e.stackTraceToString())
        }
    }

}