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
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import java.lang.ref.WeakReference

class DataManagerService : Service() {

    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val gameUpdateReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    private lateinit var id: String

    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()

    lateinit var mainActivityClient: Messenger
    var currentClient: Messenger? = null

//    private val incomingInviteDialog = IncomingInviteDialog()


    private lateinit var serviceMessenger: Messenger

    override fun onBind(intent: Intent?): IBinder {
        Toast.makeText(applicationContext, "Binding service.. ${intent?.getStringExtra("id")}", Toast.LENGTH_SHORT).show()

        serviceMessenger = Messenger(IncomingHandler(this))

        Thread {
            loadSavedGames()
        }.start()

        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Toast.makeText(applicationContext, "Unbound service..", Toast.LENGTH_SHORT).show()

        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(applicationContext, "Service created..", Toast.LENGTH_SHORT).show()
        val preferences = applicationContext.getSharedPreferences("user_data", MODE_PRIVATE)
        id = preferences.getString("ID", "")!!

//        incomingInviteDialog.create(applicationContext)


        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(inviteReceiver, infoFilter)
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Service destroyed..", Toast.LENGTH_SHORT).show()
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(inviteReceiver)
        super.onDestroy()
    }

    class IncomingHandler(service: DataManagerService) : Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {

            val service = serviceReference.get()!!

            println("${msg.what} Setting currentMessenger to: ${msg.replyTo}")
            service.currentClient = msg.replyTo

            when (msg.what) {
//                FLAG_REGISTER_CLIENT -> {
//                    val activityName = msg.obj as String
//                    if (activityName == "main_activity") {
//                        mainActivityClient = msg.replyTo
//                        mainActivityClient.send(Message.obtain(null, FLAG_GET_MULTIPLAYER_GAMES, savedGames))
//                    } else {
//                    currentMessenger = msg.replyTo
//                    }
//                }

                FLAG_SET_ID -> {
                    if (msg.obj is String) {
                        service.id = msg.obj as String
                    }
                }

                FLAG_GET_MULTIPLAYER_GAMES -> msg.replyTo.send(Message.obtain(null, FLAG_GET_MULTIPLAYER_GAMES, service.savedGames))
                FLAG_GET_GAMES_AND_INVITES -> msg.replyTo.send(Message.obtain(null, FLAG_GET_GAMES_AND_INVITES, Pair(service.savedGames, service.savedInvites)))
            }
        }
    }

    private fun sendMessage(flag: Int, data: Any?) {
        currentClient!!.send(Message.obtain(null, flag, data))
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
        sendMessage(FLAG_NEW_GAME, Pair(opponentId, GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate)))
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        savedInvites[inviteId] = InviteData(opponentName, timeStamp, InviteType.RECEIVED)

//        incomingInviteDialog.showInvite(opponentName, inviteId)

//        currentMessenger!!.send(Message.obtain(null, FLAG_GET_INVITES, savedInvites))
        sendMessage(FLAG_GET_INVITES, savedInvites)
//        processIncomingInvite(inviteId, opponentName, timeStamp)
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        processOpponentMove(gameId, moveNotation)
    }

    private fun processOpponentMove(gameId: String, moveNotation: String) {
        val move = Move.fromChessNotation(moveNotation)

        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.moveOpponent(move, false)

        savedGames[gameId] = game

        sendMessage(FLAG_OPPONENT_MOVED, MoveData(gameId, GameStatus.PLAYER_MOVE, move.timeStamp))
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE, move.timeStamp)
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

//        mainActivityClient.send(Message.obtain(null, FLAG_GET_MULTIPLAYER_GAMES, savedGames))
    }

    companion object {

        const val FLAG_REGISTER_CLIENT = 0
        const val FLAG_SET_ID = 1
        const val FLAG_GET_MULTIPLAYER_GAMES = 2
        const val FLAG_GET_INVITES = 3
        const val FLAG_GET_GAMES_AND_INVITES = 4
        const val FLAG_NEW_GAME = 5
        const val FLAG_OPPONENT_MOVED = 6

    }
}