package com.mjaruijs.fischersplayground.notification

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.util.Logger

class NotificationBuilder(context: Context) {

    private val dataManager = DataManager.getInstance(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val moveChannel = NotificationChannel(MOVE_CHANNEL_ID, MOVE_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val inviteChannel = NotificationChannel(INVITE_CHANNEL_ID, INVITE_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val newGameChannel = NotificationChannel(NEW_GAME_CHANNEL_ID, NEW_GAME_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        val miscellaneousChannel = NotificationChannel(MISCELLANEOUS_ID, MISCELLANEOUS_ID, NotificationManager.IMPORTANCE_DEFAULT)
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

    fun createNotificationData(context: Context, topic: Topic, data: Array<String>): NotificationData? {
        return when (topic) {
            Topic.INVITE -> {
                val opponentName = data[0]
                NotificationData("New invite!", "$opponentName has invited you for a game of chess!", INVITE_CHANNEL_ID, createMainActivityIntent(context, data))
            }
            Topic.NEW_GAME -> {
                val opponentName = data[1]
                val isPlayingWhite = data[2].toBoolean()
                val color = if (isPlayingWhite) "white" else "black"
                NotificationData("New game started!", "You're playing $color against $opponentName!", NEW_GAME_CHANNEL_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.MOVE -> {
                val gameId = data[0]
                val moveNotation = data[1]
                val move = moveNotation.substring(moveNotation.indexOf(':') + 1)
                val game = dataManager[gameId]
                NotificationData("Your move!", "${game.opponentName} played $move", MOVE_CHANNEL_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.UNDO_REQUESTED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("Undo requested!", "$opponentName has requested to undo their move!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.UNDO_ACCEPTED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("Move reversed!", "$opponentName has accepted your request to undo your move!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.UNDO_REJECTED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("Rejection!", "$opponentName has rejected your request to undo your move!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.RESIGN -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("Game over!", "$opponentName has resigned. You won!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.DRAW_OFFERED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("Draw offer!", "$opponentName has offered a draw!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.DRAW_ACCEPTED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("It's a draw!", "$opponentName has accepted your draw offer!", MISCELLANEOUS_ID, createMainActivityIntent(context, data))
            }
            Topic.DRAW_REJECTED -> {
                val opponentName = dataManager[data[0]].opponentName
                NotificationData("The show must go on!", "$opponentName has rejected your draw offer!", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.CHAT_MESSAGE -> {
                val opponentName = dataManager[data[0]].opponentName
                val message = data[2]
                NotificationData("New message!", "$opponentName: $message", MISCELLANEOUS_ID, createMultiplayerActivityIntent(context, data))
            }
            Topic.USER_STATUS_CHANGED -> null
            else -> null
        }
    }

    private fun createMainActivityIntent(context: Context, data: Array<String>): PendingIntent {
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

        const val GROUP_CHANNEL_ID = "Group channel"
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