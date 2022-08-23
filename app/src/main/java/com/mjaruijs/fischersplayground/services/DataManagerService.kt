package com.mjaruijs.fischersplayground.services

import android.app.*
import android.content.Context
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
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DataManagerService : Service() {

    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val gameUpdateReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
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
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private lateinit var serviceMessenger: Messenger
    private lateinit var id: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startService()
        return START_STICKY
    }

    private fun startService() {
        if (isServiceStarted) return

        Toast.makeText(this, "Service starting task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FischersPlayground::lock").apply {
                acquire()
            }
        }

//        GlobalScope.launch(Dispatchers.IO) {
//
//        }
    }

    private fun stopService() {
        Toast.makeText(this, "Service stopping task", Toast.LENGTH_SHORT).show()

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
        } catch (e: Exception) {
            println("Stopped without being started: ${e.message}")
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        Toast.makeText(applicationContext, "Binding service..", Toast.LENGTH_SHORT).show()

        serviceMessenger = Messenger(IncomingHandler(this))

        Thread {
            loadingData.set(true)
            loadData()
            loadingData.set(false)
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

        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
        registerReceiver(requestUndoReceiver, gameUpdateFilter)
        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
        registerReceiver(chatMessageReceiver, chatFilter)

        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        Toast.makeText(applicationContext, "Service destroyed..", Toast.LENGTH_SHORT).show()
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(inviteReceiver)
//        unregisterReceiver(newsReceiver)
        unregisterReceiver(gameUpdateReceiver)
        unregisterReceiver(requestUndoReceiver)
        unregisterReceiver(undoAcceptedReceiver)
        unregisterReceiver(undoRejectedReceiver)
        unregisterReceiver(opponentResignedReceiver)
        unregisterReceiver(opponentOfferedDrawReceiver)
        unregisterReceiver(opponentAcceptedDrawReceiver)
        unregisterReceiver(opponentDeclinedDrawReceiver)
        unregisterReceiver(chatMessageReceiver)

        saveData()

        super.onDestroy()
    }

    private fun sendMessage(flag: Int, data: Any?) {
        currentClient!!.send(Message.obtain(null, flag, data))
    }

    private fun createNotification(): Notification {
        val channelId = "Fischers_server_connection"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Fischers Notification Channel", NotificationManager.IMPORTANCE_HIGH).let {
            it.description = "Fischers server connection"
            it.enableVibration(true)
            it
        }

        notificationManager.createNotificationChannel(channel)

        val intent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = Notification.Builder(this, channelId)

        return builder
            .setContentTitle("Fischers Playground")
            .setContentText("It's your move!")
            .setContentIntent(intent)
            .setTicker("Tickie!")
            .build()
    }

    class IncomingHandler(service: DataManagerService) : Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {

            val service = serviceReference.get()!!

            service.currentClient = msg.replyTo

            when (msg.what) {
//                //TODO: Can this be removed? (probably)
//                FLAG_SET_ID -> {
//                    if (msg.obj is String) {
//                        service.id = msg.obj as String
//                    }
//                }

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
            }
        }
    }

    private fun onUndoRequested(content: String) {
        val data = content.split('|')
        val gameId = data[0]

        savedGames[gameId]?.addNews(NewsType.OPPONENT_REQUESTED_UNDO)

        sendMessage(FLAG_UNDO_REQUESTED, gameId)
    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed))
        savedGames[gameId]?.status = GameStatus.PLAYER_MOVE

        sendMessage(FLAG_UNDO_ACCEPTED, Pair(gameId, numberOfMovesReversed))
    }

    private fun onUndoRejected(gameId: String) {
        savedGames[gameId]?.addNews(NewsType.OPPONENT_REJECTED_UNDO)

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

        Thread {
            saveData()
        }.start()
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(opponentName, timeStamp, InviteType.RECEIVED)

        println("Got invite in service")

        Toast.makeText(applicationContext, "GOT INVITE", Toast.LENGTH_SHORT).show()

        savedInvites[inviteId] = inviteData

//        incomingInviteDialog.showInvite(opponentName, inviteId)

//        currentMessenger!!.send(Message.obtain(null, FLAG_GET_INVITES, savedInvites))
        sendMessage(FLAG_NEW_INVITE, Pair(inviteId, inviteData))
//        processIncomingInvite(inviteId, opponentName, timeStamp)
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.moveOpponent(move, false)

        savedGames[gameId] = game

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
        loadSavedGames()
        loadReceivedInvites()
        loadRecentOpponents()
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
        Thread {
            saveGames()
            saveReceivedInvites()
            saveRecentOpponents()
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


    }
}