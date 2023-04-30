package com.mjaruijs.fischersplayground.services

import android.os.Bundle
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
            FileManager.init(applicationContext)

            val notificationBuilder = NotificationBuilder.getInstance(this)

            val topic = Topic.fromString(message.data["topic"] ?: throw IllegalArgumentException("No data was found with name: topic.."))
            val content = message.data["content"] ?: throw IllegalArgumentException("No data was found with name: content..")
            val messageId = message.data["id"]?.toLong() ?: throw IllegalArgumentException("No data was found with name: id..")

            val contentList = content.split('|').toTypedArray()

            val dataManager = DataManager.getInstance(applicationContext)
            if (dataManager.isMessageHandled(messageId)) {
                Logger.debug(TAG, "Got message about $topic but was already handled!")
                return
            }
            dataManager.setMessageHandled(messageId, applicationContext)

            Logger.debug(TAG, "Got message about $topic")

            val workerData = Bundle()
            workerData.putString("topic", topic.toString())
            workerData.putStringArray("content", contentList)
            workerData.putLong("messageId", messageId)

            DataWorker(applicationContext, workerData).start()

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