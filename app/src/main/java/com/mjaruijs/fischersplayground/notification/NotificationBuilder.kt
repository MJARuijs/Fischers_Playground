package com.mjaruijs.fischersplayground.notification

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.util.Logger

class NotificationBuilder(context: Context) {

    private var notificationId = 1
    private val notificationIds = HashSet<Int>()

    private val dataManager = DataManager.getInstance(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val moveChannel = NotificationChannel(NotificationBuilder.MOVE_CHANNEL_ID, NotificationBuilder.MOVE_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val inviteChannel = NotificationChannel(NotificationBuilder.INVITE_CHANNEL_ID, NotificationBuilder.INVITE_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val newGameChannel = NotificationChannel(NotificationBuilder.NEW_GAME_CHANNEL_ID, NotificationBuilder.NEW_GAME_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val miscellaneousChannel = NotificationChannel(NotificationBuilder.MISCELLANEOUS_ID, NotificationBuilder.MISCELLANEOUS_ID, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(moveChannel)
        notificationManager.createNotificationChannel(inviteChannel)
        notificationManager.createNotificationChannel(newGameChannel)
        notificationManager.createNotificationChannel(miscellaneousChannel)
        notificationManager.createNotificationChannelGroup(NotificationChannelGroup("GROUP_TEST", "Group Test"))
    }

    fun build(context: Context, isGroup: Boolean, notificationData: NotificationData): Notification {
        return build(context, isGroup, notificationData.title, notificationData.message, notificationData.channelId, notificationData.intent)
    }

    fun build(context: Context, isGroupSummary: Boolean, title: String, message: String, channelId: String, onClickIntent: PendingIntent?): Notification {
        try {
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.black_queen)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(onClickIntent)
                .setGroup("GROUP_TEST")

            if (isGroupSummary) {
                notificationBuilder.setGroupSummary(true)
            }

//            val notificationId = generateId()
//            notificationIds += notificationId
//            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            return notificationBuilder.build()
        } catch (e: Exception){
            Logger.log(context, e.stackTraceToString(), "notification_builder_crash_log.txt")
            throw e
        }
    }

    fun notify(notification: Notification) = notify(System.currentTimeMillis().toInt(), notification)

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    private fun generateId(): Int {
        for (i in 0 until Int.MAX_VALUE) {
            if (!notificationIds.contains(i)) {
                return i
            }
        }
        throw IllegalArgumentException("No more ID's available for notifications..")
    }

    fun createNotificationData(context: Context, topic: String, data: Array<String>): NotificationData {

        return when (topic) {
            "new_game" -> {
                val opponentName = data[1]
                val isPlayingWhite = data[2].toBoolean()
                val color = if (isPlayingWhite) "white" else "black"
                NotificationData("New game started!", "You're playing $color against $opponentName!", NEW_GAME_CHANNEL_ID, createIntent(context, topic, data))
            }
            "move" -> {
                val gameId = data[0]
                val moveNotation = data[1]
                val move = moveNotation.substring(moveNotation.indexOf(':') + 1)
                val game = dataManager[gameId]
                NotificationData("Your move!", "${game.opponentName} played $move", MOVE_CHANNEL_ID, createIntent(context, topic, data))
            }
            "invite" -> {
                val opponentName = data[0]
                NotificationData("New invite!", "$opponentName has invited you for a game of chess!", INVITE_CHANNEL_ID, createIntent(context, "invite", data))
            }
            else -> throw IllegalArgumentException("Could not create NotificationData for topic: $topic")
        }
    }

    private fun createIntent(context: Context, topic: String, data: Array<String>): PendingIntent {
        return when (topic) {
            "move" -> createMultiplayerActivityIntent(context, data)
            "new_game" -> createMultiplayerActivityIntent(context, data)
            "invite" -> createMainActivityIntent(context, data)
            else -> throw IllegalArgumentException("Could not create notification for topic: $topic")
        }
    }

    fun createMainActivityIntent(context: Context, data: Array<String>): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("opponent_name", data[0])
        intent.putExtra("invite_id", data[1])
        return PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createMultiplayerActivityIntent(context: Context, data: Array<String>): PendingIntent {
        val intent = Intent(context, MultiplayerGameActivity::class.java)
        intent.putExtra("game_id", data[0])

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MultiplayerGameActivity::class.java)
        stackBuilder.addNextIntent(intent)
        return stackBuilder.getPendingIntent(System.currentTimeMillis().toInt(), PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {

        const val MOVE_CHANNEL_ID = "Move updates"
        const val INVITE_CHANNEL_ID = "New invites"
        const val NEW_GAME_CHANNEL_ID = "New game started"
        const val MISCELLANEOUS_ID = "Miscellaneous notifications"

        private var instance: NotificationBuilder? = null

        fun getInstance(context: Context): NotificationBuilder {
            if (instance == null) {
                instance = NotificationBuilder(context)
            }

            return instance!!
        }

    }

}