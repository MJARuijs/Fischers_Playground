package com.mjaruijs.fischersplayground.services

import android.os.Looper
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.FIRE_BASE_PREFERENCE_FILE
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_PREFERENCE_FILE
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.notification.NotificationBuilder.Companion.GROUP_CHANNEL_ID
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(FIRE_BASE_PREFERENCE_FILE, MODE_PRIVATE).edit().putString("token", token).apply()

//        Logger.debug("MyTag", "New FCM Token: $token")
        val userId = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, "")!!

        if (userId.isNotBlank()) {
//            val networkManager = NetworkManager.getInstance()
//            networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$userId|$token"))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
//            Looper.prepare()
//            Toast.makeText(applicationContext, "Got message!", Toast.LENGTH_SHORT).show()

            FileManager.init(applicationContext)

            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = Topic.fromString(message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic.."))
            val content = message.data["content"] ?: throw IllegalArgumentException("No data was found with name: content..")
            val messageId = message.data["id"]?.toLong() ?: throw IllegalArgumentException("No data was found with name: id..")

            val contentList = content.split('|').toTypedArray()

//            val dataManager = DataManager.getInstance(applicationContext)
//            if (dataManager.isMessageHandled(messageId)) {
//                return
//            }

            Logger.debug(TAG, "Got message about $topic")

//            FileManager.createIfAbsent("notifications.txt")
//            FileManager.append(applicationContext, "notifications.txt", "$topic;$content;$messageId")

            val worker = OneTimeWorkRequestBuilder<ProcessIncomingDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("content", contentList),
                    Pair("messageId", messageId)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)

//            Looper.prepare()
//            Toast.makeText(applicationContext, "Processing move!", Toast.LENGTH_SHORT).show()

            val notificationData = notificationBuilder.createNotificationData(applicationContext, topic, contentList) ?: return
            val notification = notificationBuilder.build(applicationContext, false, notificationData)
            notificationBuilder.notify(notification)

//            val summaryNotification = notificationBuilder.build(applicationContext, true, "Title??", "Message!", GROUP_CHANNEL_ID, null)
//            notificationBuilder.notify(0, summaryNotification)
        } catch (e: Exception) {
            FileManager.write(applicationContext, "crash.txt", e.stackTraceToString())
            NetworkService.sendCrashReport("crash_firebase.txt", e.stackTraceToString(), applicationContext)
        }
    }

    companion object {
        private const val TAG = "FirebaseService"
    }

}