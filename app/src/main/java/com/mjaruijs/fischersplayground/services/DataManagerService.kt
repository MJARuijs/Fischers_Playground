package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import com.mjaruijs.fischersplayground.activities.MainActivity
import com.mjaruijs.fischersplayground.activities.MessageReceiver
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time

class DataManagerService : Service() {

    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    private lateinit var messenger: Messenger

    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()

    private lateinit var id: String

    override fun onBind(intent: Intent?): IBinder {
        Toast.makeText(applicationContext, "Binding service.. ${intent?.getStringExtra("id")}", Toast.LENGTH_SHORT).show()
        id = intent?.getStringExtra("id") ?: ""
        messenger = Messenger(IncomingHandler())
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Toast.makeText(applicationContext, "Unbound service..", Toast.LENGTH_SHORT).show()

        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(applicationContext, "Service created..", Toast.LENGTH_SHORT).show()

        registerReceiver(newGameReceiver, infoFilter)
        loadSavedGames()
    }

    override fun onDestroy() {
//        Toast.makeText(applicationContext, "Service destroyed..", Toast.LENGTH_SHORT).show()
        unregisterReceiver(newGameReceiver)
        super.onDestroy()
    }

    private fun onNewGameStarted(content: String) {
        val data = content.split('|')

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()

        val underscoreIndex = inviteId.indexOf('_')
        val opponentId = inviteId.substring(0, underscoreIndex)

        val timeStamp = Time.getFullTimeStamp()
        val newGameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        val newGame = MultiPlayerGame(inviteId, id, opponentName, playingWhite)
        savedGames[inviteId] = newGame
        savedInvites.remove(inviteId)

        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE
//        val doesCardExist = gameAdapter.updateGameCard(inviteId, newGameStatus, playingWhite, hasUpdate)

//        if (!doesCardExist) {
//            gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate)
//        }

//        updateRecentOpponents(Pair(opponentName, opponentId))
    }

    internal class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            println("Received in Service: ${msg.obj}")
            msg.replyTo.send(Message.obtain(null, 0, "Hello to you too!"))
//            when (msg.what) {
//                0 -> {
//                    println("HELLO")
////                    msg.
//                }
//                else -> super.handleMessage(msg)
//            }
        }
    }

    private fun loadSavedGames() {
        val lines = FileManager.read(this, MainActivity.MULTIPLAYER_GAME_FILE) ?: ArrayList()

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

            val newGame = MultiPlayerGame(gameId, id, opponentName, isPlayerWhite, moves, messages, newsUpdates)
            newGame.status = gameStatus

            savedGames[gameId] = newGame
        }
    }

    companion object {

        const val REGISTER_CLIENT_VALUE = 0
        const val UNREGISTER_CLIENT_VALUE = 1

    }

//    inner class LocalBinder : Binder() {
//        fun getService() = this@DataManagerService
//    }
}