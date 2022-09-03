package com.mjaruijs.fischersplayground.services

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.notification.NotificationData
import com.mjaruijs.fischersplayground.util.FileManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DataManager(private val context: Context) {

    val savedGames = HashMap<String, MultiPlayerGame>()
    val savedInvites = HashMap<String, InviteData>()
    val recentOpponents = Stack<Pair<String, String>>()
    private val userId: String
    private val loadingData = AtomicBoolean(false)
    private val dataLoaded = AtomicBoolean(false)

    init {
        val preferences = context.getSharedPreferences("user_data", MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, "")!!

        loadData()
    }

    fun createNotificationData(topic: String, data: Array<String>): NotificationData {

        return when (topic) {
            "new_game" -> {
                val opponentName = data[1]
                val isPlayingWhite = data[2].toBoolean()
                val color = if (isPlayingWhite) "white" else "black"
                NotificationData("New game started!", "You're playing $color against $opponentName!", createIntent(topic, data))
            }
            "move" -> {
                val gameId = data[0]
                val moveNotation = data[1]
                val move = moveNotation.substring(moveNotation.indexOf(':') + 1)
                val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with gameId: $gameId")
                NotificationData("Your move!", "${game.opponentName} played $move", createIntent(topic, data))
            }
            "invite" -> {
                val opponentName = data[0]
                NotificationData("New invite!", "$opponentName has invited you for a game of chess!", createIntent("invite", data))
            }
            else -> throw IllegalArgumentException("Could not create NotificationData for topic: $topic")
        }
    }

    private fun createIntent(topic: String, data: Array<String>): PendingIntent {
        return when (topic) {
            "move" -> createMultiplayerActivityIntent(data)
            "new_game" -> createMultiplayerActivityIntent(data)
            "invite" -> createMainActivityIntent(data)
            else -> throw IllegalArgumentException("Could not create notification for topic: $topic")
        }
    }

    private fun createMainActivityIntent(data: Array<String>): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("opponent_name", data[0])
        intent.putExtra("invite_id", data[1])
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createMultiplayerActivityIntent(data: Array<String>): PendingIntent {
        val intent = Intent(context, MultiplayerGameActivity::class.java)
        intent.putExtra("game_id", data[0])

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MultiplayerGameActivity::class.java)
        stackBuilder.addNextIntent(intent)
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun isLoadingData() = loadingData.get()

    fun isDataLoaded() = dataLoaded.get()

    fun loadData() {
        if (loadingData.get()) {
//            println("Data is already loading.. returning..")
            return
        }

//        println("LOADING DATA")

        loadingData.set(true)
        Thread {
            try {
                loadSavedGames()
                loadInvites()
                loadRecentOpponents()
                dataLoaded.set(true)
            } catch (e: Exception) {
                FileManager.write(context, "file_loading_crash.txt", e.stackTraceToString())
            }

            loadingData.set(false)
        }.start()

    }

    private fun loadSavedGames() {
        val lines = FileManager.read(context, MULTIPLAYER_GAME_FILE) ?: ArrayList()

        for (gameData in lines) {
            if (gameData.isBlank()) {
                continue
            }

            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
            val gameId = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val isPlayerWhite = data[3].toBoolean()
            val gameStatus = GameStatus.fromString(data[4])
            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
            val newsData = data[7].removePrefix("[").removeSuffix("]").split("\\")

//            val winner = data[7]

            val moves = ArrayList<Move>()

            for (move in moveList) {
                if (move.isNotBlank()) {
                    moves += Move.fromChessNotation(move)
                }
            }

            val messages = ArrayList<ChatMessage>()
            for (message in chatMessages) {
                if (message.isNotBlank()) {
                    val messageData = message.split(',')
                    val timeStamp = messageData[0]
                    val messageContent = messageData[1]
                    val type = MessageType.fromString(messageData[2])

                    messages += ChatMessage(timeStamp, messageContent, type)
                }
            }

            val newsUpdates = ArrayList<News>()
            for (news in newsData) {
                if (news.isBlank()) {
                    continue
                }

                newsUpdates += News.fromString(news)
            }

            val newGame = MultiPlayerGame(gameId, userId, opponentName, isPlayerWhite, moves, messages, newsUpdates)
            newGame.status = gameStatus

            savedGames[gameId] = newGame
        }
    }

    fun loadInvites() {
        val lines = FileManager.read(context, INVITES_FILE) ?: ArrayList()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val data = line.split('|')
            val inviteId = data[0]
            val opponentName = data[1]
            val timeStamp = data[2].toLong()
            val type = InviteType.fromString(data[3])

            savedInvites[inviteId] = InviteData(opponentName, timeStamp, type)
        }
    }

    fun loadRecentOpponents() {
        val lines = FileManager.read(context, RECENT_OPPONENTS_FILE) ?: ArrayList()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val data = line.split('|')
            val opponentName = data[0]
            val opponentId = data[1]
            updateRecentOpponents(Pair(opponentName, opponentId))
        }
    }

    fun updateRecentOpponents(newOpponent: Pair<String, String>?) {
        if (newOpponent == null) {
            return
        }
        if (newOpponent.second == userId) {
            return
        }

        val temp = Stack<Pair<String, String>>()

        while (temp.size < 2 && recentOpponents.isNotEmpty()) {
            val opponent = recentOpponents.pop()

            if (opponent == newOpponent) {
                continue
            }

            temp.push(opponent)
        }

        while (recentOpponents.isNotEmpty()) {
            recentOpponents.pop()
        }

        for (i in 0 until temp.size) {
            recentOpponents.push(temp.pop())
        }

        recentOpponents.push(newOpponent)
        saveRecentOpponents()
    }

    fun saveData() {
//        println("SAVING DATA")
        Thread {
            saveGames()
            saveInvites()
            saveRecentOpponents()
//            log("Done saving data")
        }.start()
    }

    fun saveGames() {
        var content = ""

        for ((gameId, game) in savedGames) {
            var moveData = "["

            for ((i, move) in game.moves.withIndex()) {
                moveData += move.toChessNotation()
                if (i != game.moves.size - 1) {
                    moveData += "\\"
                }
            }
            moveData += "]"

            var chatData = "["

            for ((i, message) in game.chatMessages.withIndex()) {
                chatData += message.toString()
                if (i != game.chatMessages.size - 1) {
                    chatData += "\\"
                }
            }
            chatData += "]"

            var newsContent = "["

            for ((i, news) in game.newsUpdates.withIndex()) {
                newsContent += news.toString()
                if (i != game.newsUpdates.size - 1) {
                    newsContent += "\\"
                }
            }
            newsContent += "]"

            content += "$gameId|${game.lastUpdated}|${game.opponentName}|${game.isPlayingWhite}|${game.status}|$moveData|$chatData|$newsContent\n"
        }

        FileManager.write(context, MULTIPLAYER_GAME_FILE, content)
    }

    fun saveInvites() {
        var content = ""

        for ((inviteId, invite) in savedInvites) {
            content += "$inviteId|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
        }

        FileManager.write(context, INVITES_FILE, content)
    }

    fun saveRecentOpponents() {
        var data = ""
        for (recentOpponent in recentOpponents) {
            data += "${recentOpponent.first}|${recentOpponent.second}\n"
        }
        FileManager.write(context, RECENT_OPPONENTS_FILE, data)
    }

    companion object {

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "received_invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"

        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            if (instance == null) {
                instance = DataManager(context)
            } else {
                // TODO: Is this useful?
//                instance!!.loadData()
            }
            instance!!.loadData()

            return instance!!
        }

    }

}