package com.mjaruijs.fischersplayground.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.activities.MessageReceiver
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DataManagerService : Service() {

    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val opponentMovedReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)
    private val requestUndoReceiver = MessageReceiver(Topic.GAME_UPDATE, "request_undo", ::onUndoRequested)
    private val undoAcceptedReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_undo", ::onUndoAccepted)
    private val undoRejectedReceiver = MessageReceiver(Topic.GAME_UPDATE, "rejected_undo", ::onUndoRejected)
    private val opponentResignedReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_resigned", ::onOpponentResigned)
    private val opponentOfferedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_offered_draw", ::onOpponentOfferedDraw)
    private val opponentAcceptedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_draw", ::onOpponentAcceptedDraw)
    private val opponentDeclinedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "declined_draw", ::onOpponentDeclinedDraw)
    private val chatMessageReceiver = MessageReceiver(Topic.CHAT_MESSAGE, "", ::onChatMessageReceived)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()
    private val recentOpponents = Stack<Pair<String, String>>()

    private val loadingData = AtomicBoolean(false)

    private var currentClient: Messenger? = null

    private lateinit var serviceMessenger: Messenger
    private lateinit var id: String

    private var dataLoaded = false

    private var bindCount = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            loadingData.set(true)
            Thread {
                loadData()
                loadingData.set(false)
                log("Done loading data")
            }.start()

            val topic = intent.getStringExtra("topic") ?: ""
            val data = intent.getStringExtra("data") ?: ""

            while (loadingData.get()) {
                Thread.sleep(10)
            }

            when (topic) {
                "move" -> onOpponentMoved(data)
            }
        }

//        startForeground(1, buildNotification())
//        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        val caller = intent?.getStringExtra("caller") ?: ""

        bindCount++
        Toast.makeText(applicationContext, "Binding service to $caller. $bindCount", Toast.LENGTH_SHORT).show()

        serviceMessenger = Messenger(IncomingHandler(this))

        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bindCount--

        Toast.makeText(applicationContext, "Unbound service.. $bindCount", Toast.LENGTH_SHORT).show()
        saveData()

        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(applicationContext, "Service created..", Toast.LENGTH_SHORT).show()
        val preferences = applicationContext.getSharedPreferences("user_data", MODE_PRIVATE)
        id = preferences.getString(USER_ID_KEY, "")!!

        load()

        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(opponentMovedReceiver, gameUpdateFilter)
        registerReceiver(requestUndoReceiver, gameUpdateFilter)
        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
        registerReceiver(chatMessageReceiver, chatFilter)

//        startForeground(1, buildNotification())
//        stopSelf()
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Service destroyed..", Toast.LENGTH_SHORT).show()
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(opponentMovedReceiver)
        unregisterReceiver(requestUndoReceiver)
        unregisterReceiver(undoAcceptedReceiver)
        unregisterReceiver(undoRejectedReceiver)
        unregisterReceiver(opponentResignedReceiver)
        unregisterReceiver(opponentOfferedDrawReceiver)
        unregisterReceiver(opponentAcceptedDrawReceiver)
        unregisterReceiver(opponentDeclinedDrawReceiver)
        unregisterReceiver(chatMessageReceiver)

        dataLoaded = false

        super.onDestroy()
    }

    private fun sendMessage(flag: Int, data: Any?) {
        if (currentClient == null) {
            println("Tried to send message: $flag")
        }
        currentClient?.send(Message.obtain(null, flag, data))
    }

    private fun load() {
        if (!dataLoaded) {
            dataLoaded = true
            loadingData.set(true)

            Thread {
                loadData()
                loadingData.set(false)
            }.start()
        }
    }

    private fun log(message: String) {
        FileManager.append(applicationContext, "log.txt", "$message\n")
    }

    private fun buildNotification(): Notification {
        val channelId = "Fetching updates"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.black_queen)
            .setContentTitle("Title")
            .setContentText("Fetching game updates")
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Opponent moved", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        return notificationBuilder.build()
    }

    class IncomingHandler(service: DataManagerService) : Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {

            val service = serviceReference.get()!!
            if (msg.replyTo != null) {
                service.currentClient = msg.replyTo
//                service.load()
            }

            when (msg.what) {
                //TODO: Can this be removed? (probably)
                FLAG_SET_ID -> {
                    if (msg.obj is String) {
                        service.id = msg.obj as String
                    }
                }

                FLAG_GET_MULTIPLAYER_GAMES -> msg.replyTo.send(Message.obtain(null, FLAG_GET_MULTIPLAYER_GAMES, service.savedGames))
                FLAG_GET_ALL_DATA -> {
                    while (service.loadingData.get()) {
                        Thread.sleep(1)
                    }
                    msg.replyTo.send(Message.obtain(null, FLAG_GET_ALL_DATA, Triple(service.savedGames, service.savedInvites, service.recentOpponents)))
                }
                FLAG_SET_GAME_STATUS -> {
                    val data = msg.obj as? Pair<*, *> ?: return
                    if (data.first is String && data.second is GameStatus) {
                        service.savedGames[data.first]?.status = data.second as GameStatus
                    } else {
                        throw IllegalArgumentException("Got invalid arguments for FLAG_SET_GAME_STATUS")
                    }
                }
                FLAG_SAVE_GAME -> {
                    val game = msg.obj as? MultiPlayerGame ?: return
                    service.savedGames[game.gameId] = game
                }
                FLAG_GET_GAME -> {
                    val gameId = msg.obj as String
                    msg.replyTo.send(Message.obtain(null, FLAG_GET_GAME, service.savedGames[gameId]))
                }
                FLAG_DELETE_GAME -> {
                    val id = msg.obj as String
                    service.savedGames.remove(id)
                    service.savedInvites.remove(id)
                    service.saveData()
                }
                FLAG_STORE_SENT_INVITE -> {
                    val data = msg.obj as Triple<*, *, *>
                    if (data.first is String && data.second is String && data.third is InviteData) {
                        val inviteId = data.first as String
                        val opponentId = data.second as String
                        val opponentName = (data.third as InviteData).opponentName
                        service.updateRecentOpponents(Pair(opponentName, opponentId))
                        service.savedInvites[inviteId] = data.third as InviteData
                    }
                }
                FLAG_GET_RECENT_OPPONENTS -> {
                    msg.replyTo.send(Message.obtain(null, FLAG_GET_RECENT_OPPONENTS, service.recentOpponents))
                }
                FLAG_OPPONENT_MOVED -> {
                    service.onOpponentMoved(msg.obj as String)
                }
                FLAG_MOVE_MADE -> {
                    val data = msg.obj as? Pair<*, *> ?: return
                    if (data.first is String && data.second is Move) {
                        val gameId = data.first as String
                        val move = data.second as Move
                        service.onMoveMade(gameId, move)
                    }
                }
                FLAG_NEW_GAME -> service.onNewGameStarted(msg.obj as String)
                FLAG_NEW_INVITE -> service.onIncomingInvite(msg.obj as String)
            }
        }
    }

    private fun onMoveMade(gameId: String, move: Move) {
//        savedGames[gameId]?.move(move, true)
//        savedGames[gameId]!!.addMove(move)
        saveData()
    }

    private fun onUndoRequested(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        savedGames[gameId]?.addNews(NewsType.OPPONENT_REQUESTED_UNDO)

        saveData()

        sendMessage(FLAG_UNDO_REQUESTED, gameId)
    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed))
        savedGames[gameId]?.status = GameStatus.PLAYER_MOVE

        saveData()

        sendMessage(FLAG_UNDO_ACCEPTED, Pair(gameId, numberOfMovesReversed))
    }

    private fun onUndoRejected(gameId: String) {
        savedGames[gameId]?.addNews(NewsType.OPPONENT_REJECTED_UNDO)
        saveData()

        sendMessage(FLAG_UNDO_REJECTED, gameId)
    }

    private fun onOpponentResigned(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        sendMessage(FLAG_OPPONENT_RESIGNED, gameId)
    }

    private fun onOpponentOfferedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        sendMessage(FLAG_OPPONENT_OFFERED_DRAW, gameId)
    }

    private fun onOpponentAcceptedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        sendMessage(FLAG_OPPONENT_ACCEPTED_DRAW, gameId)
    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        sendMessage(FLAG_OPPONENT_REJECTED_DRAW, gameId)
    }

    private fun onChatMessageReceived(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        sendMessage(FLAG_CHAT_MESSAGE_RECEIVED, Triple(gameId, timeStamp, messageContent))
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
        updateRecentOpponents(Pair(opponentName, opponentId))

        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE
        sendMessage(FLAG_NEW_GAME, GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate))

        saveData()
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(opponentName, timeStamp, InviteType.RECEIVED)

//        Toast.makeText(applicationContext, "GOT INVITE", Toast.LENGTH_SHORT).show()

        savedInvites[inviteId] = inviteData

//        incomingInviteDialog.showInvite(opponentName, inviteId)
        saveData()

//        currentMessenger!!.send(Message.obtain(null, FLAG_GET_INVITES, savedInvites))
        sendMessage(FLAG_NEW_INVITE, Pair(inviteId, inviteData))
//        processIncomingInvite(inviteId, opponentName, timeStamp)
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        try {
//            val game = savedGames[gameId] ?: return
            val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId. DataLoaded: $dataLoaded")
            game.moveOpponent(move, false)
            savedGames[gameId] = game

            saveData()
        } catch (e: Exception) {
            FileManager.write(applicationContext, "crash_log.txt", e.stackTraceToString())
        }

        sendMessage(FLAG_OPPONENT_MOVED, MoveData(gameId, GameStatus.PLAYER_MOVE, move.timeStamp))
    }

    private fun updateRecentOpponents(newOpponent: Pair<String, String>?) {
        if (newOpponent == null) {
            return
        }
        if (newOpponent.second == id) {
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

    private fun loadData() {
        println("LOADING DATA")
        try {
            loadSavedGames()
            loadReceivedInvites()
            loadRecentOpponents()
        } catch (e: Exception) {
            FileManager.write(applicationContext, "file_loading_crash.txt", e.stackTraceToString())
        }
    }

    private fun loadSavedGames() {
        val lines = FileManager.read(this, MULTIPLAYER_GAME_FILE) ?: ArrayList()

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

    private fun loadReceivedInvites() {
        val lines = FileManager.read(this, INVITES_FILE) ?: ArrayList()

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

    private fun loadRecentOpponents() {
        val lines = FileManager.read(this, RECENT_OPPONENTS_FILE) ?: ArrayList()

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

    private fun saveData() {
        println("SAVING DATA")
        Thread {
            saveGames()
            saveReceivedInvites()
            saveRecentOpponents()
            log("Done saving data")
        }.start()
    }

    private fun saveGames() {
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

        FileManager.write(this, MULTIPLAYER_GAME_FILE, content)
    }

    private fun saveReceivedInvites() {
        var content = ""

        for ((inviteId, invite) in savedInvites) {
            content += "$inviteId|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
        }

        FileManager.write(this, INVITES_FILE, content)
    }

    private fun saveRecentOpponents() {
        var data = ""
        for (recentOpponent in recentOpponents) {
            data += "${recentOpponent.first}|${recentOpponent.second}\n"
        }
        FileManager.write(this, RECENT_OPPONENTS_FILE, data)
    }

    companion object {
        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "received_invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"

        const val FLAG_REGISTER_CLIENT = 0
        const val FLAG_SET_ID = 1
        const val FLAG_GET_MULTIPLAYER_GAMES = 2
        const val FLAG_NEW_INVITE = 3
        const val FLAG_GET_ALL_DATA = 4
        const val FLAG_NEW_GAME = 5
        const val FLAG_OPPONENT_MOVED = 6
        const val FLAG_UNDO_REQUESTED = 7
        const val FLAG_UNDO_ACCEPTED = 8
        const val FLAG_UNDO_REJECTED = 9
        const val FLAG_OPPONENT_RESIGNED = 10
        const val FLAG_OPPONENT_OFFERED_DRAW = 11
        const val FLAG_OPPONENT_ACCEPTED_DRAW = 12
        const val FLAG_OPPONENT_REJECTED_DRAW = 13
        const val FLAG_CHAT_MESSAGE_RECEIVED = 14
        const val FLAG_SET_GAME_STATUS = 15
        const val FLAG_SAVE_GAME = 16
        const val FLAG_GET_GAME = 17
        const val FLAG_DELETE_GAME = 18
        const val FLAG_STORE_SENT_INVITE = 19
        const val FLAG_GET_RECENT_OPPONENTS = 20
        const val FLAG_MOVE_MADE = 21


    }
}