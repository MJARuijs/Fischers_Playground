package com.mjaruijs.fischersplayground.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.dialogs.InvitePlayerDialog
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameAdapter
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.news.News
import com.mjaruijs.fischersplayground.chess.SavedGames
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.news.NewsType
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.util.Time
import java.util.*
import kotlin.collections.ArrayList

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

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")

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

        findViewById<ImageView>(R.id.settings_button)
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

        findViewById<UIButton>(R.id.start_new_game_button)
            .setText("Start new game")
            .setButtonTextSize(100.0f)
            .setColor(Color.rgb(235, 186, 145))
            .setCornerRadius(45.0f)
            .setOnClickListener {
                invitePlayerDialog.show()
            }

        gameAdapter = GameAdapter(::onGameClicked)

//        gameAdapter += GameCardItem("0", 0,  "Opponent 1", GameStatus.INVITE_RECEIVED)              // 2    7
//        gameAdapter += GameCardItem("1", 0, "Opponent 2", GameStatus.PLAYER_MOVE)                   // 5    1
//        gameAdapter += GameCardItem("2", 0, "Opponent 3", GameStatus.GAME_LOST)                     // 7    4
//        gameAdapter += GameCardItem("3", 2, "Opponent 4", GameStatus.OPPONENT_MOVE)                 // 3    5
//        gameAdapter += GameCardItem("4", 1,  "Opponent 5", GameStatus.PLAYER_MOVE)                  // 4    2
//        gameAdapter += GameCardItem("5", 1, "Opponent 6", GameStatus.GAME_WON)                      // 6    6
//        gameAdapter += GameCardItem("6", 1, "Opponent 7", GameStatus.INVITE_PENDING)                // 1    3

        val gameRecyclerView = findViewById<RecyclerView>(R.id.game_list)
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter
    }

    private fun isInitialized() = NetworkManager.isRunning()

    private fun updateRecentOpponents(newOpponent: Pair<String, String>) {
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

        val newGameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        SavedGames.put(inviteId, MultiPlayerGame(inviteId, id!!, opponentName, playingWhite))

        val timeStamp = Time.getTimeStamp()
        gameAdapter.updateGameCard(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, createGameIfAbsent = true, hasUpdate = true)

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

        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)

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

        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
    }

    private fun onOpponentOfferedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_OFFERED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
    }

    private fun onOpponentAcceptedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_ACCEPTED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
        game.news = News(NewsType.OPPONENT_DECLINED_DRAW)
        SavedGames.put(gameId, game)

        gameAdapter.hasUpdate(gameId)
//        gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
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

    private fun parseActiveGameData(activeGamesData: String) {
        val gamesData = activeGamesData.split(',')

        for (gameData in gamesData) {
            if (gameData.isBlank()) {
                continue
            }

            val data = gameData.split('|')
            val gameId = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val isPlayerWhite = data[3].toBoolean()
            val currentPlayerToMove = data[4]
            val moveList = data[5].removePrefix("[").removeSuffix("]").split(' ')
            val winner = data[6]

            val moves = ArrayList<Move>()

            for (move in moveList) {
                if (move.isNotBlank()) {
                    moves += Move.fromChessNotation(move)
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

            SavedGames.put(gameId, MultiPlayerGame(gameId, id!!, opponentName, isPlayerWhite, moves))
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
        val updatedGameIds = updatedGameData.split(',')

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
    }

    override fun onResume() {
        super.onResume()
        val games = SavedGames.getAll()
        for (game in games) {
            val gameId = game.first
            val lastUpdated = game.second.lastUpdated
            val opponentName = game.second.opponentName
            val status = game.second.status
            val isPlayerWhite = game.second.isPlayingWhite

            gameAdapter.updateGameCard(gameId, lastUpdated, opponentName, status, isPlayerWhite, true, false)
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
        super.onStop()
    }

    private fun showKeyboard(view: View) {
        if (view.requestFocus()) {
            val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager? ?: return
            inputManager.showSoftInput(view.findFocus(), 0)
        }
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }
}