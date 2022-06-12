package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameAdapter
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.SavedGames
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.dialogs.InvitePlayerDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.news.News
import com.mjaruijs.fischersplayground.news.NewsType
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.util.Time
import java.util.*

class MainActivity : AppCompatActivity() {

    private var id: String? = null
    private var userName: String? = null

    private val idReceiver = MessageReceiver(Topic.INFO, "id", ::onIdReceived)
    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)
    private val playersReceiver = MessageReceiver(Topic.INFO, "search_players_result", ::onPlayersReceived)
    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val activeGameReceiver = MessageReceiver(Topic.INFO, "user_data", ::onActiveGamesReceived)
    private val gameUpdateReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
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

    private val invitePlayerDialog = InvitePlayerDialog(::onInvite)
    private val incomingInviteDialog = IncomingInviteDialog()

    private val recentOpponents = Stack<Pair<String, String>>()

    private lateinit var gameAdapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideActivityDecorations()

        registerReceivers()

        val preferences = getPreferences(MODE_PRIVATE)
        id = preferences.getString("ID", "")
        userName = preferences.getString("USER_NAME", "")

        if (!isInitialized()) {
            PieceTextures.init(this)

            Thread {
                OBJLoader.preload(this, R.raw.pawn_bytes, "pawn")
            }.start()

            Thread {
                OBJLoader.preload(this, R.raw.bishop_bytes, "bishop")
            }.start()

            Thread {
                OBJLoader.preload(this, R.raw.knight_bytes, "parsed_knight")
            }.start()

            Thread {
                OBJLoader.preload(this, R.raw.rook_bytes, "rook")
            }.start()

            Thread {
                OBJLoader.preload(this, R.raw.queen_bytes, "queen")
            }.start()

            Thread {
                OBJLoader.preload(this, R.raw.king_bytes, "king")
            }.start()

            NetworkManager.run(this)

            if (id != null && id!!.isNotBlank()) {
                NetworkManager.sendMessage(Message(Topic.INFO, "id", "$id"))
            }
        }

        if (userName == null || userName!!.isBlank()) {
            showCreateUsernameDialog()
        } else {
            findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        }

        incomingInviteDialog.create(this)
        invitePlayerDialog.create(id!!, this)

        findViewById<UIButton>(R.id.settings_button)
            .setColoredDrawable(R.drawable.settings_solid_icon)
            .setColor(Color.TRANSPARENT)
            .setChangeIconColorOnHover(false)
            .setChangeTextColorOnHover(true)
            .setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

        findViewById<UIButton>(R.id.start_new_game_button)
            .setText("Start new game")
            .setButtonTextSize(100.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setOnClickListener {
                invitePlayerDialog.show()
            }

        findViewById<UIButton>(R.id.single_player_button)
            .setText("Single player")
            .setButtonTextSize(100.0f)
            .setColor(Color.rgb(235, 186, 145))
            .setCornerRadius(45.0f)
            .setOnClickListener {
                val intent = Intent(this, GameActivity::class.java)
                    .putExtra("id", id)
                    .putExtra("user_name", userName)
                    .putExtra("is_single_player", true)
                    .putExtra("is_playing_white", true)
                    .putExtra("game_id", "test_game")
                    .putExtra("opponent_name", "Opponent")
                startActivity(intent)
            }

        gameAdapter = GameAdapter(::onGameClicked)

        val gameRecyclerView = findViewById<RecyclerView>(R.id.game_list)
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
    }

    private fun isInitialized() = NetworkManager.isRunning()

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

        invitePlayerDialog.setRecentOpponents(recentOpponents)
    }

    private fun onInvite(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        updateRecentOpponents(Pair(opponentName, opponentId))
    }

    private fun onGameClicked(gameCard: GameCardItem) {
        gameAdapter.clearUpdate(gameCard, id!!)

        if (gameCard.gameStatus == GameStatus.INVITE_RECEIVED) {
            incomingInviteDialog.showInvite(gameCard.opponentName, gameCard.id)
        } else if (gameCard.gameStatus != GameStatus.INVITE_PENDING) {
            val intent = Intent(this, GameActivity::class.java)
                .putExtra("id", id)
                .putExtra("user_name", userName)
                .putExtra("is_playing_white", gameCard.isPlayingWhite)
                .putExtra("game_id", gameCard.id)
                .putExtra("opponent_name", gameCard.opponentName)
            startActivity(intent)
        }
    }

    private fun onNewGameStarted(content: String) {
        val data = content.split('|')

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()

        val underscoreIndex = inviteId.indexOf('_')
        val opponentId = inviteId.substring(0, underscoreIndex)

        SavedGames.put(inviteId, MultiPlayerGame(inviteId, id!!, opponentName, playingWhite))

        val timeStamp = Time.getFullTimeStamp()
        val newGameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE
        val doesCardExist = gameAdapter.updateGameCard(inviteId, newGameStatus, playingWhite, hasUpdate)

        if (!doesCardExist) {
            gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate)
        }

        updateRecentOpponents(Pair(opponentName, opponentId))
    }

    private fun onActiveGamesReceived(content: String) {
        val startIndex = content.indexOf('{') + 1
        val endIndex = content.indexOf('}')
        val activeGamesData = content.substring(startIndex, endIndex)
        parseActiveGameData(activeGamesData)

        val receivedInvitesStartIndex = content.indexOf('{', endIndex) + 1
        val receivedInvitesEndIndex = content.indexOf('}', receivedInvitesStartIndex)
        val receivedInvitesData = content.substring(receivedInvitesStartIndex, receivedInvitesEndIndex)
        parseReceivedInvitesData(receivedInvitesData)

        val pendingInvitesStartIndex = content.indexOf('{', receivedInvitesEndIndex) + 1
        val pendingInvitesEndIndex = content.indexOf('}', pendingInvitesStartIndex)
        val pendingInvitesData = content.substring(pendingInvitesStartIndex, pendingInvitesEndIndex)
        parsePendingInvitesData(pendingInvitesData)

        val recentOpponentsStartIndex = content.indexOf('{', pendingInvitesEndIndex) + 1
        val recentOpponentsEndIndex = content.indexOf('}', recentOpponentsStartIndex)
        val recentOpponentsData = content.substring(recentOpponentsStartIndex, recentOpponentsEndIndex)
        parseRecentOpponents(recentOpponentsData)

        val updatedGameStartIndex = content.indexOf('{', recentOpponentsEndIndex) + 1
        val updatedGameEndIndex = content.indexOf('}', updatedGameStartIndex)
        val updatedGamesData = content.substring(updatedGameStartIndex, updatedGameEndIndex)
        processUpdatedGames(updatedGamesData)
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val invitingUsername = data[0]
        val invitingUserId = data[1]
        val inviteId = data[2]
        val timeStamp = data[3].toLong()

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
        gameAdapter += GameCardItem(inviteId, timeStamp, invitingUsername, GameStatus.INVITE_RECEIVED, hasUpdate = true)
    }

    private fun showCreateUsernameDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Create username")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        dialogBuilder.setView(input)
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            val userName = input.text.toString()
            val preferences = getPreferences(MODE_PRIVATE)

            with(preferences.edit()) {
                putString("USER_NAME", userName)
                apply()
            }

            this.userName = userName

            findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
            NetworkManager.sendMessage(Message(Topic.INFO, "user_name", userName))
        }

        dialogBuilder.show()
    }

    private fun onPlayersReceived(content: String) {
        val playersData = content.split(')')

        invitePlayerDialog.clearPlayers()
        for (playerData in playersData) {
            if (playerData.isBlank()) {
                continue
            }

            val data = playerData.removePrefix("(").removeSuffix(")").split('|')
            val name = data[0]
            val id = data[1]
            invitePlayerDialog.addPlayers(name, id)
        }
    }

    private fun onIdReceived(id: String) {
        val preferences = getPreferences(MODE_PRIVATE)
        this.id = id

        with(preferences.edit()) {
            putString("ID", id)
            apply()
        }
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE, move.timeStamp)

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.moveOpponent(move, false)

        SavedGames.put(gameId, game)
    }

    private fun onOpponentResigned(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_RESIGNED)
        SavedGames.put(gameId, game)

        gameAdapter.updateCardStatus(gameId, GameStatus.GAME_WON)
    }

    private fun onOpponentOfferedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_OFFERED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
    }

    private fun onOpponentAcceptedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_ACCEPTED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_DECLINED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
    }

    private fun onUndoRequested(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]
        val opponentUserId = data[2]

        gameAdapter.hasUpdate(gameId)
        SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_REQUESTED_UNDO)
    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        gameAdapter.hasUpdate(gameId)
        SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed)
        SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
    }

    private fun onUndoRejected(gameId: String) {
        gameAdapter.hasUpdate(gameId)
        SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_REJECTED_UNDO)
    }

    private fun onChatMessageReceived(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        val message = ChatMessage(timeStamp, messageContent, MessageType.RECEIVED)
        SavedGames.get(gameId)?.chatMessages?.add(message)
    }

    private fun parseActiveGameData(activeGamesData: String) {
        val gamesData = activeGamesData.split(',')

        for (gameData in gamesData) {
            if (gameData.isBlank()) {
                continue
            }

            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
            val gameId = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val isPlayerWhite = data[3].toBoolean()
            val currentPlayerToMove = data[4]
            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
            val winner = data[7]

            val moves = ArrayList<Move>()

            for (move in moveList) {
                if (move.isNotBlank()) {
                    moves += Move.fromChessNotation(move)
                }
            }

            val messages = ArrayList<ChatMessage>()
            for (message in chatMessages) {
                if (message.isNotBlank()) {
                    val messageData = message.split('~')
                    val senderId = messageData[0]
                    val timeStamp = messageData[1]
                    val messageContent = messageData[2]

                    val type = if (senderId == id) MessageType.SENT else MessageType.RECEIVED

                    messages += ChatMessage(timeStamp, messageContent, type)
                }
            }

            val gameStatus = if (winner.isBlank()) {
                if (currentPlayerToMove == id) {
                    GameStatus.PLAYER_MOVE
                } else {
                    GameStatus.OPPONENT_MOVE
                }
            } else if (winner == id) {
                GameStatus.GAME_WON
            } else if (winner == "draw") {
                GameStatus.GAME_DRAW
            } else {
                GameStatus.GAME_LOST
            }

            SavedGames.put(gameId, MultiPlayerGame(gameId, id!!, opponentName, isPlayerWhite, moves, messages))
            gameAdapter += GameCardItem(gameId, lastUpdated, opponentName, gameStatus, isPlayerWhite, hasUpdate = false)
        }
    }

    private fun parseReceivedInvitesData(receivedInviteData: String) {
        val invitesData = receivedInviteData.split(',')

        for (inviteData in invitesData) {
            if (inviteData.isBlank()) {
                continue
            }

            val data = inviteData.split('|')
            val gameId = data[0]
            val timeStamp = data[1].toLong()
            val invitingUsername = data[2]

            gameAdapter += GameCardItem(gameId, timeStamp, invitingUsername, GameStatus.INVITE_RECEIVED, hasUpdate = false)
        }
    }

    private fun parsePendingInvitesData(pendingInvitesData: String) {
        val invitesData = pendingInvitesData.split(',')

        for (inviteData in invitesData) {
            if (inviteData.isBlank()) {
                continue
            }

            val data = inviteData.split('|')
            val gameId = data[0]
            val timeStamp = data[1].toLong()
            val invitingUsername = data[2]

            gameAdapter += GameCardItem(gameId, timeStamp, invitingUsername, GameStatus.INVITE_PENDING, hasUpdate = false)
        }
    }

    private fun parseRecentOpponents(recentOpponentsData: String) {
        val opponentsData = recentOpponentsData.split(',')

        for (opponentData in opponentsData) {
            if (opponentData.isBlank()) {
                continue
            }

            val data = opponentData.split('|')
            val opponentName = data[0]
            val opponentId = data[1]

            updateRecentOpponents(Pair(opponentName, opponentId))
        }
    }

    private fun processUpdatedGames(updatedGameData: String) {
        val updatedGameIds = updatedGameData.split(' ')

        for (gameId in updatedGameIds) {
            if (gameId.isBlank()) {
                continue
            }

            gameAdapter.hasUpdate(gameId)
        }
    }

    private fun registerReceivers() {
        registerReceiver(idReceiver, infoFilter)
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(playersReceiver, infoFilter)
        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(activeGameReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
        registerReceiver(requestUndoReceiver, gameUpdateFilter)
        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
        registerReceiver(chatMessageReceiver, chatFilter)
    }

    override fun onResume() {
        super.onResume()
        println("ON RESUME")
        val games = SavedGames.getAll()
        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|online"))
//        NetworkManager.sendMessage(Message(Topic.GAME_UPDATE))
        for (game in games) {
            val gameId = game.first
            val lastUpdated = game.second.lastUpdated
            val opponentName = game.second.opponentName
            val status = game.second.status
            val isPlayerWhite = game.second.isPlayingWhite

            val doesGameExist = gameAdapter.updateGameCard(gameId, status, isPlayerWhite)
            if (!doesGameExist) {
                gameAdapter += GameCardItem(gameId, lastUpdated, opponentName, status, isPlayerWhite, true)
            }
        }

        registerReceivers()
    }

    override fun onStop() {
        unregisterReceiver(idReceiver)
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(playersReceiver)
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(activeGameReceiver)
        unregisterReceiver(gameUpdateReceiver)
        unregisterReceiver(requestUndoReceiver)
        unregisterReceiver(undoAcceptedReceiver)
        unregisterReceiver(undoRejectedReceiver)
        unregisterReceiver(opponentResignedReceiver)
        unregisterReceiver(opponentOfferedDrawReceiver)
        unregisterReceiver(opponentAcceptedDrawReceiver)
        unregisterReceiver(opponentDeclinedDrawReceiver)
        unregisterReceiver(chatMessageReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        println("ON DESTROY MAIN_ACTIVITY")
        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|offline"))
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
//        println("USER LEAVING")
        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|away"))
        super.onUserLeaveHint()
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }
}