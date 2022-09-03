package com.mjaruijs.fischersplayground.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.util.Logger

object NotificationBuilder {

    private var notificationId = 1
    private val notificationIds = HashSet<Int>()

    fun build(context: Context, notificationData: NotificationData) {
        build(context, notificationData.title, notificationData.message, notificationData.intent)
    }

    fun build(context: Context, title: String, message: String, onClickIntent: PendingIntent) {
        try {
            val channelId = context.getString(R.string.default_notification_channel_id)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.black_queen)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(onClickIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Opponent moved", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

//            val notificationId = generateId()
//            notificationIds += notificationId
            notificationManager.notify(notificationId++, notificationBuilder.build())
        } catch (e: Exception){
            Logger.log(context, e.stackTraceToString(), "notification_builder_crash_log.txt")
        }
    }

    private fun generateId(): Int {
        for (i in 0 until Int.MAX_VALUE) {
            if (!notificationIds.contains(i)) {
                return i
            }
        }
        throw IllegalArgumentException("No more ID's available for notifications..")
    }

}