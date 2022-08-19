package com.mjaruijs.fischersplayground.activities

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.activities.game.PractiseGameActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.*
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.CreateGameDialog
import com.mjaruijs.fischersplayground.dialogs.CreateUsernameDialog
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAMES_AND_INVITES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_MULTIPLAYER_GAMES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_SET_ID
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : ClientActivity() {

    override var activityName = "main_activity"

    override var clientMessenger = Messenger(IncomingHandler(this))

    private var id: String? = null
    private var userName: String? = null

    private val idReceiver = MessageReceiver(Topic.INFO, "id", ::onIdReceived)
    private val playersReceiver = MessageReceiver(Topic.INFO, "search_players_result", ::onPlayersReceived)
//    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)

//    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
//    private val newsReceiver = MessageReceiver(Topic.INFO, "news", ::onNewsReceived)
//    private val gameUpdateReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
//    private val requestUndoReceiver = MessageReceiver(Topic.GAME_UPDATE, "request_undo", ::onUndoRequested)
//    private val undoAcceptedReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_undo", ::onUndoAccepted)
//    private val undoRejectedReceiver = MessageReceiver(Topic.GAME_UPDATE, "rejected_undo", ::onUndoRejected)
//    private val opponentResignedReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_resigned", ::onOpponentResigned)
//    private val opponentOfferedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_offered_draw", ::onOpponentOfferedDraw)
//    private val opponentAcceptedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_draw", ::onOpponentAcceptedDraw)
//    private val opponentDeclinedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "declined_draw", ::onOpponentDeclinedDraw)
//    private val chatMessageReceiver = MessageReceiver(Topic.CHAT_MESSAGE, "", ::onChatMessageReceived)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
//    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
//    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    private val createUsernameDialog = CreateUsernameDialog()
    private val createGameDialog = CreateGameDialog(::onInvite)
//    private val incomingInviteDialog = IncomingInviteDialog()

    private lateinit var gameAdapter: GameAdapter

    private val recentOpponents = Stack<Pair<String, String>>()

    private var stayingInApp = false

    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()

    private var maxTextSize = Float.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Figure out why this invokes onStop(), and find a solution
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        println("MAIN_ACTIVITY: onCreate")
        setContentView(R.layout.activity_main)
        hideActivityDecorations()

        registerReceivers()

        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)
        id = preferences.getString("ID", "")
        userName = preferences.getString("USER_NAME", "")

        Thread {
            while (!serviceBound) {
                Thread.sleep(10)
            }
            sendMessage(FLAG_SET_ID, id)
//            serviceMessenger!!.send(Message.obtain(null, FLAG_SET_ID, id))
        }.start()

        if (!isInitialized()) {
            preloadModels()

            NetworkManager.run(this)

            if (id != null && id!!.isNotBlank()) {
                NetworkManager.sendMessage(NetworkMessage(Topic.INFO, "id", "$id"))
            }
        }

        createUsernameDialog.create(this)
        createUsernameDialog.setLayout()

        if (userName == null || userName!!.isBlank()) {
            createUsernameDialog.show(::setUserName)
        } else {
            findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        }

        initUIComponents()
    }

    private fun isInitialized() = NetworkManager.isRunning()

    private fun isUserRegisteredAtServer(): Boolean {
        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)
        return preferences.contains("ID")
    }

    private fun updateRecentOpponents(newOpponent: Pair<String, String>) {
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

        createGameDialog.setRecentOpponents(recentOpponents)
    }

    private fun onInvite(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        savedInvites[inviteId] = InviteData(opponentName, timeStamp, InviteType.PENDING)
        updateRecentOpponents(Pair(opponentName, opponentId))
    }

    private fun onGameClicked(gameCard: GameCardItem) {
        stayingInApp = true
        gameAdapter.clearUpdate(gameCard, id!!)

        if (gameCard.gameStatus == GameStatus.INVITE_RECEIVED) {
//            incomingInviteDialog.showInvite(gameCard.opponentName, gameCard.id)
        } else if (gameCard.gameStatus != GameStatus.INVITE_PENDING) {
//            saveData()
            val intent = Intent(this, MultiplayerGameActivity::class.java)
                .putExtra("id", id)
                .putExtra("user_name", userName)
                .putExtra("is_playing_white", gameCard.isPlayingWhite)
                .putExtra("game_id", gameCard.id)
                .putExtra("opponent_name", gameCard.opponentName)
//                .putExtra("saved_games", gamesToString())
            startActivity(intent)
        }
    }

    private fun onGameDeleted(gameId: String) {
        savedGames.remove(gameId)
        savedInvites.remove(gameId)
        saveGames()
    }

//    private fun onNewGameStarted(content: String) {
//        val data = content.split('|')
//
//        val inviteId = data[0]
//        val opponentName = data[1]
//        val playingWhite = data[2].toBoolean()
//
//        val underscoreIndex = inviteId.indexOf('_')
//        val opponentId = inviteId.substring(0, underscoreIndex)
//
//        val timeStamp = Time.getFullTimeStamp()
//        val newGameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
//
//        val newGame = MultiPlayerGame(inviteId, id!!, opponentName, playingWhite)
//        savedGames[inviteId] = newGame
//        savedInvites.remove(inviteId)
//
//        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE
//        val doesCardExist = gameAdapter.updateGameCard(inviteId, newGameStatus, playingWhite, hasUpdate)
//
//        if (!doesCardExist) {
//            gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate)
//        }
//
//        updateRecentOpponents(Pair(opponentName, opponentId))
//    }
//
//    private fun onNewsReceived(content: String) {
//        val data = content.split('|')
//
//        for (news in data) {
//            if (news.isBlank()) {
//                continue
//            }
//
//            val newsData = news.split(',')
//            val id = newsData[0]
//            val type = newsData[1]
//
//            val extraData = if (newsData.size == 3) newsData[2] else ""
//
////            processNews(id, type, extraData)
//        }
////        val startIndex = content.indexOf('{') + 1
////        val endIndex = content.indexOf('}')
////        val activeGamesData = content.substring(startIndex, endIndex)
////        parseActiveGameData(activeGamesData)
////
////        val receivedInvitesStartIndex = content.indexOf('{', endIndex) + 1
////        val receivedInvitesEndIndex = content.indexOf('}', receivedInvitesStartIndex)
////        val receivedInvitesData = content.substring(receivedInvitesStartIndex, receivedInvitesEndIndex)
////        parseReceivedInvitesData(receivedInvitesData)
////
////        val pendingInvitesStartIndex = content.indexOf('{', receivedInvitesEndIndex) + 1
////        val pendingInvitesEndIndex = content.indexOf('}', pendingInvitesStartIndex)
////        val pendingInvitesData = content.substring(pendingInvitesStartIndex, pendingInvitesEndIndex)
////        parsePendingInvitesData(pendingInvitesData)
////
////        val recentOpponentsStartIndex = content.indexOf('{', pendingInvitesEndIndex) + 1
////        val recentOpponentsEndIndex = content.indexOf('}', recentOpponentsStartIndex)
////        val recentOpponentsData = content.substring(recentOpponentsStartIndex, recentOpponentsEndIndex)
////        parseRecentOpponents(recentOpponentsData)
////
////        val updatedGameStartIndex = content.indexOf('{', recentOpponentsEndIndex) + 1
////        val updatedGameEndIndex = content.indexOf('}', updatedGameStartIndex)
////        val updatedGamesData = content.substring(updatedGameStartIndex, updatedGameEndIndex)
////        processUpdatedGames(updatedGamesData)
//    }

//    private fun processNews(id: String, type: String, extraData: String) {
//        when (type) {
//            "OPPONENT_INVITED" -> {
//                val data = extraData.split(':')
//                val opponentName = data[0]
//                val timeStamp = data[1].toLong()
//
//                processIncomingInvite(id, opponentName, timeStamp)
//            }
//            "OPPONENT_MOVED" -> processOpponentMove(id, extraData)
//            "OPPONENT_RESIGNED" -> processOpponentResigned(id)
//            "OPPONENT_OFFERED_DRAW" -> processOpponentOfferingDraw(id)
//            "OPPONENT_ACCEPTED_DRAW" -> processOpponentAcceptedDraw(id)
//            "OPPONENT_REJECTED_DRAW" -> processOpponentRejectedDraw(id)
//            "OPPONENT_REQUESTED_UNDO" -> processOpponentRequestingUndo(id)
//            "UNDO_REQUEST_ACCEPTED" -> processUndoRequestAccepted(id, extraData.toInt())
//            "OPPONENT_REJECTED_UNDO_REQUEST" -> processOpponentRejectedUndoRequest(id)
//            "CHAT_MESSAGE" -> {
//                val data = extraData.split(':')
//                val messageContent = data[0]
//                val timeStamp = data[1]
//                processChatMessage(id, messageContent, timeStamp)
//            }
//        }
//    }

//    private fun onIncomingInvite(content: String) {
//        val data = content.split('|')
//
//        val opponentName = data[0]
//        val inviteId = data[1]
//        val timeStamp = data[2].toLong()
//
//        processIncomingInvite(inviteId, opponentName, timeStamp)
//    }

//    private fun processIncomingInvite(inviteId: String, opponentName: String, timeStamp: Long) {
//        savedInvites[inviteId] = InviteData(opponentName, timeStamp, InviteType.RECEIVED)
//
////        incomingInviteDialog.showInvite(opponentName, inviteId)
//        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)
//    }
//
//    private fun processOpponentMove(gameId: String, moveNotation: String) {
//        val move = Move.fromChessNotation(moveNotation)
//
//        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//        game.moveOpponent(move, false)
//
//        savedGames[gameId] = game
//
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE, move.timeStamp)
//    }

    override fun onOpponentMoved(data: Triple<String, GameStatus, Long>?) {
        if (data == null) {
            return
        }

        gameAdapter.updateCardStatus(data.first, data.second, data.third)
    }

//    private fun processOpponentResigned(gameId: String) {
//        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//        game.addNews(News(NewsType.OPPONENT_RESIGNED))
//        savedGames[gameId] = game
//        gameAdapter.updateCardStatus(gameId, GameStatus.GAME_WON)
//    }
//
//    private fun processOpponentOfferingDraw(gameId: String) {
//        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//        game.addNews(News(NewsType.OPPONENT_OFFERED_DRAW))
//        savedGames[gameId] = game
//        gameAdapter.hasUpdate(gameId)
//    }
//
//    private fun processOpponentAcceptedDraw(gameId: String) {
//        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//        game.addNews(News(NewsType.OPPONENT_ACCEPTED_DRAW))
//
//        savedGames[gameId] = game
//        gameAdapter.hasUpdate(gameId)
//    }
//
//    private fun processOpponentRejectedDraw(gameId: String) {
//        val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//        game.addNews(News(NewsType.OPPONENT_DECLINED_DRAW))
//        savedGames[gameId] = game
//
//        gameAdapter.hasUpdate(gameId)
//    }

    override fun onUndoRequested(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onUndoRequestAccepted(data: Pair<String, Int>?) {
        if (data == null) {
            return
        }
        gameAdapter.hasUpdate(data.first)
    }

    override fun onUndoRequestRejected(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

//    private fun processOpponentRequestingUndo(gameId: String) {
//        gameAdapter.hasUpdate(gameId)
////        savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REQUESTED_UNDO))
//    }
//
//    private fun processUndoRequestAccepted(gameId: String, numberOfMovesReversed: Int) {
////        savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed))
////        savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//        gameAdapter.hasUpdate(gameId)
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
//    }
//
//    private fun processOpponentRejectedUndoRequest(gameId: String) {
//        gameAdapter.hasUpdate(gameId)
////        savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REJECTED_UNDO))
//    }
//
//    private fun processChatMessage(gameId: String, messageContent: String, timeStamp: String) {
//        val message = ChatMessage(timeStamp, messageContent, MessageType.RECEIVED)
//        savedGames[gameId]?.chatMessages?.add(message)
//    }

    private fun setUserName(userName: String) {
        this.userName = userName

        savePreference("USER_NAME", userName)

        findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        NetworkManager.sendMessage(NetworkMessage(Topic.INFO, "user_name", userName))
    }

    private fun onPlayersReceived(content: String) {
        val playersData = content.split(')')

        createGameDialog.clearPlayers()
        for (playerData in playersData) {
            if (playerData.isBlank()) {
                continue
            }

            val data = playerData.removePrefix("(").removeSuffix(")").split('|')
            val name = data[0]
            val id = data[1]
            createGameDialog.addPlayers(name, id)
        }
    }

    private fun onIdReceived(id: String) {
        this.id = id

        savePreference("ID", id)

        createGameDialog.updateId(id)
    }

//    private fun onOpponentMoved(content: String) {
//        val data = content.split('|')
//
//        val gameId = data[0]
//        val moveNotation = data[1]
//        processOpponentMove(gameId, moveNotation)
//    }

    override fun onOpponentResigned(gameId: String) {
        gameAdapter.updateCardStatus(gameId, GameStatus.GAME_WON)
    }

    override fun onOpponentOfferedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onOpponentAcceptedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onOpponentRejectedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

//    private fun onUndoRequested(content: String) {
//        val data = content.split('|')
//        val gameId = data[0]
//
//        processOpponentRequestingUndo(gameId)
//    }

//    private fun onUndoAccepted(content: String) {
//        val data = content.split('|')
//        val gameId = data[0]
//        val numberOfMovesReversed = data[1].toInt()
//
//        processUndoRequestAccepted(gameId, numberOfMovesReversed)
//    }

//    private fun onUndoRejected(gameId: String) {
//        processOpponentRejectedUndoRequest(gameId)
//    }
//
//    private fun onChatMessageReceived(content: String) {
//        val data = content.split('|')
//        val gameId = data[0]
//        val timeStamp = data[1]
//        val messageContent = data[2]
//
//        processChatMessage(gameId, messageContent, timeStamp)
//    }
//
//    private fun parseActiveGameData(activeGamesData: String) {
//        val gamesData = activeGamesData.split(',')
//
//        var content = ""
//
//        for (gameData in gamesData) {
//            if (gameData.isBlank()) {
//                continue
//            }
//
//            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
//            val gameId = data[0]
//            val lastUpdated = data[1].toLong()
//            val opponentName = data[2]
//            val isPlayerWhite = data[3].toBoolean()
//            val currentPlayerToMove = data[4]
//            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
//            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
//            val winner = data[7]
//
//            val moves = ArrayList<Move>()
//
//            for (move in moveList) {
//                if (move.isNotBlank()) {
//                    moves += Move.fromChessNotation(move)
//                }
//            }
//
//            val messages = ArrayList<ChatMessage>()
//            for (message in chatMessages) {
//                if (message.isNotBlank()) {
//                    val messageData = message.split('~')
//                    val senderId = messageData[0]
//                    val timeStamp = messageData[1]
//                    val messageContent = messageData[2]
//
//                    val type = if (senderId == id) MessageType.SENT else MessageType.RECEIVED
//
//                    messages += ChatMessage(timeStamp, messageContent, type)
//                }
//            }
//
//            val gameStatus = if (winner.isBlank()) {
//                if (currentPlayerToMove == id) {
//                    GameStatus.PLAYER_MOVE
//                } else {
//                    GameStatus.OPPONENT_MOVE
//                }
//            } else if (winner == id) {
//                GameStatus.GAME_WON
//            } else if (winner == "draw") {
//                GameStatus.GAME_DRAW
//            } else {
//                GameStatus.GAME_LOST
//            }
//
//            content += "$gameId|$lastUpdated|$opponentName|$gameStatus|$isPlayerWhite|true\n"
//
//            val newGame = MultiPlayerGame(gameId, id!!, opponentName, isPlayerWhite, moves, messages)
//            newGame.status = gameStatus
//
//            savedGames[gameId] = newGame
//            gameAdapter += GameCardItem(gameId, lastUpdated, opponentName, gameStatus, isPlayerWhite, hasUpdate = false)
//        }
//
//        FileManager.write(this, "game.txt", content)
//    }
//
//    private fun parseReceivedInvitesData(receivedInviteData: String) {
//        val invitesData = receivedInviteData.split(',')
//
//        for (inviteData in invitesData) {
//            if (inviteData.isBlank()) {
//                continue
//            }
//
//            val data = inviteData.split('|')
//            val gameId = data[0]
//            val timeStamp = data[1].toLong()
//            val invitingUsername = data[2]
//
//            gameAdapter += GameCardItem(gameId, timeStamp, invitingUsername, GameStatus.INVITE_RECEIVED, hasUpdate = false)
//        }
//    }
//
//    private fun parsePendingInvitesData(pendingInvitesData: String) {
//        val invitesData = pendingInvitesData.split(',')
//
//        for (inviteData in invitesData) {
//            if (inviteData.isBlank()) {
//                continue
//            }
//
//            val data = inviteData.split('|')
//            val gameId = data[0]
//            val timeStamp = data[1].toLong()
//            val invitingUsername = data[2]
//
//            gameAdapter += GameCardItem(gameId, timeStamp, invitingUsername, GameStatus.INVITE_PENDING, hasUpdate = false)
//        }
//    }
//
//    private fun parseRecentOpponents(recentOpponentsData: String) {
//        val opponentsData = recentOpponentsData.split(',')
//
//        for (opponentData in opponentsData) {
//            if (opponentData.isBlank()) {
//                continue
//            }
//
//            val data = opponentData.split('|')
//            val opponentName = data[0]
//            val opponentId = data[1]
//
//            updateRecentOpponents(Pair(opponentName, opponentId))
//        }
//    }
//
//    private fun processUpdatedGames(updatedGameData: String) {
//        val updatedGameIds = updatedGameData.split(' ')
//
//        for (gameId in updatedGameIds) {
//            if (gameId.isBlank()) {
//                continue
//            }
//
//            gameAdapter.hasUpdate(gameId)
//        }
//    }

    private fun registerReceivers() {
        registerReceiver(idReceiver, infoFilter)
        registerReceiver(playersReceiver, infoFilter)

//        registerReceiver(inviteReceiver, infoFilter)
//        registerReceiver(newGameReceiver, infoFilter)
//        registerReceiver(newsReceiver, infoFilter)
//        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
//        registerReceiver(requestUndoReceiver, gameUpdateFilter)
//        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
//        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
//        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
//        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
//        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
//        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
//        registerReceiver(chatMessageReceiver, chatFilter)
    }

    override fun onResume() {
        super.onResume()

        if (serviceBound) {
//            serviceMessenger!!.send(Message.obtain(null, FLAG_GET_GAMES_AND_INVITES))
            sendMessage(FLAG_GET_GAMES_AND_INVITES)
        }

        stayingInApp = false

        if (isUserRegisteredAtServer()) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|online"))
        }

//        loadData()

        registerReceivers()
    }

    override fun onStop() {
        unregisterReceiver(idReceiver)
        unregisterReceiver(playersReceiver)

//        unregisterReceiver(inviteReceiver)
//        unregisterReceiver(newGameReceiver)
//        unregisterReceiver(newsReceiver)
//        unregisterReceiver(gameUpdateReceiver)
//        unregisterReceiver(requestUndoReceiver)
//        unregisterReceiver(undoAcceptedReceiver)
//        unregisterReceiver(undoRejectedReceiver)
//        unregisterReceiver(opponentResignedReceiver)
//        unregisterReceiver(opponentOfferedDrawReceiver)
//        unregisterReceiver(opponentAcceptedDrawReceiver)
//        unregisterReceiver(opponentDeclinedDrawReceiver)
//        unregisterReceiver(chatMessageReceiver)

        saveData()

//        println("MAIN_ACTIVITY: onStop")
        if (!stayingInApp) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|away"))
        }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        if (!stayingInApp) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|away"))
        }

        super.onUserLeaveHint()
    }

    private fun savePreference(key: String, value: String) {
        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)

        with(preferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

//    private fun loadData() {
//        loadSavedGames()
//        loadReceivedInvites()
//        loadRecentOpponents()
//    }

//    private fun loadSavedGames() {
//        val lines = FileManager.read(this, MULTIPLAYER_GAME_FILE) ?: ArrayList()
//
//        for (gameData in lines) {
//            if (gameData.isBlank()) {
//                continue
//            }
//
//            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
//            val gameId = data[0]
//            val lastUpdated = data[1].toLong()
//            val opponentName = data[2]
//            val isPlayerWhite = data[3].toBoolean()
//            val gameStatus = GameStatus.fromString(data[4])
//            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
//            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
//            val newsData = data[7].removePrefix("[").removeSuffix("]").split("\\")
//
//            val moves = ArrayList<Move>()
//
//            for (move in moveList) {
//                if (move.isNotBlank()) {
//                    moves += Move.fromChessNotation(move)
//                }
//            }
//
//            val messages = ArrayList<ChatMessage>()
//            for (message in chatMessages) {
//                if (message.isNotBlank()) {
//                    val messageData = message.split(',')
//                    val timeStamp = messageData[0]
//                    val messageContent = messageData[1]
//                    val type = MessageType.fromString(messageData[2])
//
//                    messages += ChatMessage(timeStamp, messageContent, type)
//                }
//            }
//
//            val newsUpdates = ArrayList<News>()
//            for (news in newsData) {
//                if (news.isBlank()) {
//                    continue
//                }
//
//                newsUpdates += News.fromString(news)
//            }
//
//            val newGame = MultiPlayerGame(gameId, id!!, opponentName, isPlayerWhite, moves, messages, newsUpdates)
//            newGame.status = gameStatus
//
//            savedGames[gameId] = newGame
//
//            val doesCardExist = gameAdapter.updateGameCard(gameId, gameStatus, isPlayerWhite, false)
//
//            if (!doesCardExist) {
//                gameAdapter += GameCardItem(gameId, lastUpdated, opponentName, gameStatus, isPlayerWhite, false)
//            }
//        }
//    }
//
//    private fun loadReceivedInvites() {
//        val lines = FileManager.read(this, INVITES_FILE) ?: ArrayList()
//
//        for (line in lines) {
//            if (line.isBlank()) {
//                continue
//            }
//
//            val data = line.split('|')
//            val inviteId = data[0]
//            val opponentName = data[1]
//            val timeStamp = data[2].toLong()
//            val type = InviteType.fromString(data[3])
//
//            savedInvites[inviteId] = InviteData(opponentName, timeStamp, type)
//
//            val status = when (type) {
//                InviteType.PENDING -> GameStatus.INVITE_PENDING
//                InviteType.RECEIVED -> GameStatus.INVITE_RECEIVED
//            }
//
//            val hasUpdate = when (type) {
//                InviteType.PENDING -> false
//                InviteType.RECEIVED -> true
//            }
//
//            val doesCardExist = gameAdapter.containsCard(inviteId)
//            if (!doesCardExist) {
//                gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, status, null, hasUpdate)
//            }
//        }
//    }
//
//    private fun loadRecentOpponents() {
//        val lines = FileManager.read(this, RECENT_OPPONENTS_FILE) ?: ArrayList()
//
//        for (line in lines) {
//            if (line.isBlank()) {
//                continue
//            }
//
//            val data = line.split('|')
//            val opponentName = data[0]
//            val opponentId = data[1]
//            updateRecentOpponents(Pair(opponentName, opponentId))
//        }
//    }

    private fun saveData() {
        saveGames()
        saveReceivedInvites()
        saveRecentOpponents()
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

    private fun preloadModels() {
        PieceTextures.init(resources)

        Thread {
            OBJLoader.preload(resources, R.raw.pawn_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.bishop_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.knight_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.rook_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.queen_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.king_bytes)
        }.start()
    }

    private fun onButtonInitialized(textSize: Float) {
        if (textSize < maxTextSize) {
            maxTextSize = textSize
            findViewById<UIButton>(R.id.start_new_game_button).setButtonTextSize(maxTextSize)
            findViewById<UIButton>(R.id.single_player_button).setButtonTextSize(maxTextSize)
        }
    }

    private fun initUIComponents() {
        gameAdapter = GameAdapter(::onGameClicked, ::onGameDeleted)

        val gameRecyclerView = findViewById<RecyclerView>(R.id.game_list)
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

//        incomingInviteDialog.create(this)
        createGameDialog.create(id!!, this)

        findViewById<UIButton>(R.id.settings_button)
            .setColoredDrawable(R.drawable.settings_solid_icon)
            .setColor(Color.TRANSPARENT)
            .setChangeIconColorOnHover(false)
            .setChangeTextColorOnHover(true)
            .setOnClickListener {
                stayingInApp = true
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

        findViewById<UIButton>(R.id.single_player_button)
            .setText("Practice Mode")
            .setButtonTextSize(70.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setChangeTextColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                stayingInApp = true
                val intent = Intent(this, PractiseGameActivity::class.java)
                    .putExtra("id", id)
                    .putExtra("user_name", userName)
                    .putExtra("is_single_player", true)
                    .putExtra("is_playing_white", true)
                    .putExtra("game_id", "test_game")
                    .putExtra("opponent_name", "Opponent")
//                intent.flags =  Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }

        findViewById<UIButton>(R.id.start_new_game_button)
            .setText("Start new game")
            .setButtonTextSize(70.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setChangeTextColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                createGameDialog.show()
            }
    }

    override fun newGameStarted(gameData: Pair<String, GameCardItem>?) {
        if (gameData == null) {
            return
        }

        val opponentId = gameData.first
        val gameCard = gameData.second

        val doesCardExist = gameAdapter.updateGameCard(gameCard.id, gameCard.gameStatus, gameCard.isPlayingWhite, gameCard.hasUpdate)
        if (!doesCardExist) {
            gameAdapter += gameCard
        }

        updateRecentOpponents(Pair(gameCard.opponentName, opponentId))
    }

    override fun updateGames(games: HashMap<String, MultiPlayerGame>?) {
        if (games == null) {
            return
        }

        for ((gameId, game) in games) {
            val doesCardExist = gameAdapter.updateGameCard(gameId, game.status, game.isPlayingWhite, false)

            if (!doesCardExist) {
                gameAdapter += GameCardItem(gameId, game.lastUpdated, game.opponentName, game.status, game.isPlayingWhite, false)
            }
        }
    }

    override fun updateInvites(invites: HashMap<String, InviteData>?) {
        if (invites == null) {
            return
        }

        for ((inviteId, invite) in invites) {
            gameAdapter += GameCardItem(inviteId, invite.timeStamp, invite.opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)
        }
    }

    override fun updateGamesAndInvites(data: Pair<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>>?) {
        if (data == null) {
            return
        }

        updateGames(data.first)
        updateInvites(data.second)
    }

    companion object {

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "received_invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"

    }
}