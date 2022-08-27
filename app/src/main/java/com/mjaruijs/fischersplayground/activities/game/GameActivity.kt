package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.PlayerStatus
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionButtonsFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.SurfaceView
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_MOVE_MADE
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_SET_GAME_STATUS
import com.mjaruijs.fischersplayground.util.FileManager

abstract class GameActivity : ClientActivity() {

    val undoRequestedDialog = UndoRequestedDialog()
    val undoRejectedDialog = UndoRejectedDialog()
    val resignDialog = ResignDialog()
    val offerDrawDialog = OfferDrawDialog()
    val opponentResignedDialog = OpponentResignedDialog()
    val opponentOfferedDrawDialog = OpponentOfferedDrawDialog()
    val opponentAcceptedDrawDialog = OpponentAcceptedDrawDialog()
    val opponentRejectedDrawDialog = OpponentDeclinedDrawDialog()

    private val checkMateDialog = CheckMateDialog()
    private val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    private var displayWidth = 0
    private var displayHeight = 0

    private var isSinglePlayer = true
    var isPlayingWhite = true

    lateinit var gameId: String
    lateinit var opponentName: String

    open lateinit var game: Game

    lateinit var glView: SurfaceView

    protected var loadFragments = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
//        println("GAME_ACTIVITY: onCreate")

        try {

            val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
            val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

            hideActivityDecorations(fullScreen)
            registerReceivers()

            undoRequestedDialog.create(this)
            undoRejectedDialog.create(this)
            offerDrawDialog.create(this)
            opponentResignedDialog.create(this)
            opponentOfferedDrawDialog.create(this)
            opponentAcceptedDrawDialog.create(this)
            opponentRejectedDrawDialog.create(this)
            checkMateDialog.create(this)
            pieceChooserDialog.create(this)

            userId = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, "")!!
            userName = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_NAME_KEY, "")!!
            isSinglePlayer = (this is SinglePlayerGameActivity)

            if (this is SinglePlayerGameActivity) {
                opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
                isPlayingWhite = intent.getBooleanExtra("is_playing_white", true)
            }

//        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|$gameId"))

            glView = findViewById(R.id.opengl_view)
            glView.init(::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite)

            if (savedInstanceState == null) {
                if (isSinglePlayer) {
                    loadFragments()
                } else {
                    loadFragments = true
                }
            }
        } catch (e: Exception) {
            FileManager.append(this, "game_activity_crash_report.txt", e.stackTraceToString())
        }
    }

    fun loadFragments() {
        val playerBundle = Bundle()
        playerBundle.putString("player_name", userName)
        playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
        playerBundle.putBoolean("hide_status_icon", true)

        val opponentBundle = Bundle()
        opponentBundle.putString("player_name", opponentName)
        opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
        opponentBundle.putBoolean("hide_status_icon", isSinglePlayer)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.player_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
            replace(R.id.opponent_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
        }
    }

    fun getActionBarFragment(): ActionButtonsFragment {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ActionButtonsFragment } as ActionButtonsFragment)
    }

    open fun onContextCreated() {
//        println("GAME_ACTIVITY: context created")
//        if (isSinglePlayer) {
//            game = SinglePlayerGame()
//        } else {
//            game = savedGames[gameId] ?: MultiPlayerGame(gameId, id, opponentName, isPlayingWhite)

//            runOnUiThread {
//                getChatFragment().addMessages((game as MultiPlayerGame).chatMessages)
//            }
//        }



//        glView.setGame(game)

        restorePreferences()

//        if (game is MultiPlayerGame) {
//            runOnUiThread {
//                processNews((game as MultiPlayerGame))
//            }
//        }
    }

    fun setGameCallbacks() {
        game.onPawnPromoted = ::onPawnPromoted
        game.enableBackButton = ::enableBackButton
        game.enableForwardButton = ::enableForwardButton
        game.onPieceTaken = ::onPieceTaken
        game.onPieceRegained = ::onPieceRegained
        game.onCheckMate = ::onCheckMate
        game.onMoveMade = ::onMoveMade

        glView.setGame(game)
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        val cameraRotation = preferences.getString(SettingsActivity.CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(SettingsActivity.FOV_KEY, 45)
        val pieceScale = preferences.getFloat(SettingsActivity.PIECE_SCALE_KEY, 1.0f)

        if (cameraRotation.isNotBlank()) {
            glView.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView.getRenderer().setFoV(fov)
        glView.getRenderer().setPieceScale(pieceScale)
    }

    private fun onPawnUpgraded(square: Vector2, pieceType: PieceType, team: Team) {
        game.upgradePawn(square, pieceType, team)
        Thread {
            Thread.sleep(10)
            glView.invalidate()
            glView.requestRender()
        }.start()
    }

    open fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    open fun onClick(x: Float, y: Float) {
        game.onClick(x, y, displayWidth, displayHeight)
    }

    private fun onMoveMade(move: Move) {
        sendMessage(FLAG_MOVE_MADE, Pair(gameId, move))
    }

    private fun onCheckMate(team: Team) {
        runOnUiThread {
            if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
                checkMateDialog.show(userName, ::closeAndSaveGameAsWin)
            } else {
                checkMateDialog.show(opponentName, ::closeAndSaveGameAsLoss)
            }
        }
    }

    private fun onPieceTaken(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")
            (opponentFragment as PlayerCardFragment).addTakenPiece(pieceType, team)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            val playerFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "player" } ?: throw IllegalArgumentException("No fragment for opponent was found..")
            (playerFragment as PlayerCardFragment).addTakenPiece(pieceType, team)
        }
    }

    private fun onPieceRegained(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "player" } ?: throw IllegalArgumentException("No fragment for player was found..")
            (opponentFragment as PlayerCardFragment).removeTakenPiece(pieceType, team)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            val playerFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for opponent was found..")
            (playerFragment as PlayerCardFragment).removeTakenPiece(pieceType, team)
        }
    }

//    private fun processNews(game: MultiPlayerGame) {
//        for (news in game.newsUpdates) {
//            when (news.newsType) {
//                NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show(opponentName, ::closeAndSaveGameAsWin)
//                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show(gameId, id, opponentName, ::acceptDraw)
//                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show(gameId, opponentName, ::closeAndSaveGameAsDraw)
//                NewsType.OPPONENT_DECLINED_DRAW -> opponentDeclinedDrawDialog.show(opponentName)
//                NewsType.OPPONENT_REQUESTED_UNDO -> undoRequestedDialog.show(gameId, opponentName, id)
//                NewsType.OPPONENT_ACCEPTED_UNDO -> {
//                    game.undoMoves(news.data)
//                    glView.requestRender()
//                }
//                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
//                NewsType.NO_NEWS -> {}
//            }
//        }
//        game.clearNews()
//    }

    private fun onNewGameStarted(content: String) {
        val data = content.split('|')

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()

//        val newGame = MultiPlayerGame(inviteId, id, opponentName, playingWhite)
//        savedGames[inviteId] = newGame
    }

//    private fun onOpponentResigned(content: String) {
//        val data = content.split('|')
//        val gameId = data[0]
//        val opponentUsername = data[1]
//
//        if (this.gameId == gameId) {
//            opponentResignedDialog.show(opponentUsername, ::closeAndSaveGameAsWin)
//        } else {
////            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
////            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_RESIGNED))
//        }
//    }
//
//    private fun onOpponentOfferedDraw(content: String) {
//        val data = content.split('|')
//
//        val gameId = data[0]
//        val opponentUsername = data[1]
//
//        if (this.gameId == gameId) {
//            opponentOfferedDrawDialog.show(gameId, id, opponentUsername, ::acceptDraw)
//        } else {
////            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
////            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_OFFERED_DRAW))
//        }
//    }
//
//    private fun onOpponentAcceptedDraw(content: String) {
//        val data = content.split('|')
//
//        val gameId = data[0]
//        val opponentUsername = data[1]
//
//        if (this.gameId == gameId) {
//            opponentAcceptedDrawDialog.show(gameId, opponentUsername, ::closeAndSaveGameAsDraw)
//        } else {
////            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
////            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_DRAW))
//        }
//    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentRejectedDrawDialog.show(opponentUsername)
        } else {
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_DECLINED_DRAW))
        }
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        if (this.gameId == gameId) {
            (game as MultiPlayerGame).moveOpponent(move, false)
            glView.requestRender()
        } else {
//            val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//            game.moveOpponent(move, false)
//            savedGames[gameId] = game
        }
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val invitingUsername = data[0]
        val inviteId = data[1]

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
    }

//    override fun onUndoRequested(content: String) {
//        val data = content.split('|')
//        val gameId = data[0]
//        val opponentUsername = data[1]
//
//        if (this.gameId == gameId) {
//            undoRequestedDialog.show(gameId, opponentUsername, id)
//        } else {
////            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REQUESTED_UNDO))
//        }
//    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        if (this.gameId == gameId) {
            (game as MultiPlayerGame).undoMoves(numberOfMovesReversed)
            glView.requestRender()
        } else {
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed))
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
        }
    }

    private fun onUndoRejected(gameId: String) {
        if (this.gameId == gameId) {
            undoRejectedDialog.show(opponentName)
        } else {
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REJECTED_UNDO))
        }
    }

    private fun onUserStatusReceived(content: String) {
        setOpponentStatusIcon(content)
    }

    private fun onChatMessageReceived(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        val message = ChatMessage(timeStamp, messageContent, MessageType.RECEIVED)

        if (this.gameId == gameId) {
//            getChatFragment().addReceivedMessage(message)
        } else {
//            savedGames[gameId]?.chatMessages?.add(message)
        }
    }

//    private fun onChatMessageSent(message: ChatMessage) {
//        NetworkManager.sendMessage(Message(Topic.CHAT_MESSAGE, "", "$gameId|$id|${message.timeStamp}|${message.message}"))
//        if (game is MultiPlayerGame) {
//            (game as MultiPlayerGame).chatMessages += message
//        }
//    }

//    private fun finishActivity() {
//        val resultIntent = Intent()
////        resultIntent.putExtra("saved_games", gamesToString())
//
//        if (game is MultiPlayerGame) {
//            resultIntent.putExtra("gameId", gameId)
//            resultIntent.putExtra("lastUpdated", game.lastUpdated)
//            resultIntent.putExtra("opponentName", opponentName)
//            resultIntent.putExtra("status", (game as MultiPlayerGame).status.toString())
//            resultIntent.putExtra("isPlayingWhite", isPlayingWhite)
//            resultIntent.putExtra("hasUpdate", true)
////            setResult(Activity.RESULT_OK, resultIntent)
//        } else {
////            setResult(Activity.RESULT_CANCELED, resultIntent)
//        }

//        finish()
//    }

    private fun finishActivity(status: GameStatus) {
//        if (game is MultiPlayerGame) {
//            (game as MultiPlayerGame).status = status
//        }
        sendMessage(FLAG_SET_GAME_STATUS, Pair(gameId, status))

//        saveGames()
//        saveGame()

//        val resultIntent = Intent()
//        resultIntent.putExtra("gameId", gameId)
//        resultIntent.putExtra("lastUpdated", game.lastUpdated)
//        resultIntent.putExtra("opponentName", opponentName)
//        resultIntent.putExtra("status", status)
//        resultIntent.putExtra("isPlayingWhite", isPlayingWhite)
//        resultIntent.putExtra("hasUpdate", true)
//        setResult(Activity.RESULT_OK, resultIntent)

//        println("GAME_ACTIVITY: finish")
        finish()
    }

    fun closeAndSaveGameAsWin() {
        finishActivity(GameStatus.GAME_WON)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_WON
//        finish()
    }

    fun closeAndSaveGameAsDraw() {
        finishActivity(GameStatus.GAME_DRAW)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_DRAW
//        finish()
    }

    fun closeAndSaveGameAsLoss() {
        finishActivity(GameStatus.GAME_LOST)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
//        finish()
    }

    private fun hideActivityDecorations(isFullscreen: Boolean) {
        supportActionBar?.hide()

        if (isFullscreen) {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setOpponentStatusIcon(gameId: String) {
//        println("GETTING FRAGMENT")
        val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")

//        println("TRYING TO SET STATUS: ${this.gameId} :: $gameId")

        when {
//            this.gameId == gameId -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_GAME)
            gameId == "online" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
            gameId == "away" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.AWAY)
            gameId == "offline" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.OFFLINE)
            else -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
        }
    }

//    private fun saveGame() {
//        println("SAVING GAME: $gameId ${(game as MultiPlayerGame).status}")
////        SavedGames.put(gameId, game as MultiPlayerGame)
//        FileManager.write(this, "game.txt", (game as MultiPlayerGame).toString())
//    }

    private fun registerReceivers() {
//        registerReceiver(inviteReceiver, infoFilter)
//        registerReceiver(newGameReceiver, infoFilter)
//        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
//        registerReceiver(requestUndoReceiver, gameUpdateFilter)
//        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
//        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
//        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
//        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
//        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
//        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
//        registerReceiver(chatMessageReceiver, chatFilter)
//        registerReceiver(userStatusReceiver, statusFilter)
    }

    override fun setGame(game: MultiPlayerGame) {
        super.setGame(game)
        if (game.moves.isNotEmpty()) {
            if (game.getMoveIndex() != -1) {
                getActionBarFragment().enableBackButton()
            }
            if (!game.isShowingCurrentMove()) {
                getActionBarFragment().enableForwardButton()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        stayingInApp = false
        registerReceivers()

        pieceChooserDialog.setLayout()
    }

    override fun onPause() {
//        if (game is MultiPlayerGame) {
//            saveGame()
//        }

//        println("GAME ACTIVITY: onPause")
//        saveGames()

//        unregisterReceiver(inviteReceiver)
//        unregisterReceiver(newGameReceiver)
//        unregisterReceiver(gameUpdateReceiver)
//        unregisterReceiver(requestUndoReceiver)
//        unregisterReceiver(undoAcceptedReceiver)
//        unregisterReceiver(undoRejectedReceiver)
//        unregisterReceiver(opponentResignedReceiver)
//        unregisterReceiver(opponentOfferedDrawReceiver)
//        unregisterReceiver(opponentAcceptedDrawReceiver)
//        unregisterReceiver(opponentDeclinedDrawReceiver)
//        unregisterReceiver(chatMessageReceiver)
//        unregisterReceiver(userStatusReceiver)

        super.onPause()
    }

    override fun onDestroy() {
//        println("ON DESTROY GAME_ACTIVITY $isFinishing")
        glView.destroy()
//        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|$gameId|offline"))

        super.onDestroy()
    }

    override fun onUserLeaveHint() {
//        NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$playerId|$gameId|away"))
        super.onUserLeaveHint()
    }



//    private fun parseSavedGames(content: String) {
//        val gamesData = content.split(',')
//
//        for (gameData in gamesData) {
//            val data = gameData.split('|')
//            val gameId = data[0]
//            val lastUpdated = data[1].toLong()
//            val opponentName = data[2]
//            val gameStatus = GameStatus.fromString(data[3])
//            val isPlayingWhite = data[4].toBoolean()
//            val hasUpdate = data[5].toBoolean()
//
//
//        }
//    }

//    private fun gamesToString(): String {
//        var data = ""
//
//        for (game in savedGames) {
//            data += "${game.key}|${game.value.lastUpdated}|${game.value.opponentName}|${game.value.status}|${game.value.isPlayingWhite}|true,"
//        }
//
//        return data
//    }



    private fun onPawnPromoted(square: Vector2, team: Team): PieceType {
        runOnUiThread { pieceChooserDialog.show(square, team) }
        return PieceType.QUEEN
    }

    private fun enableBackButton() {
        getActionBarFragment().enableBackButton()
//        findViewById<UIButton>(R.id.back_button).enable()
        glView.requestRender()
    }

    private fun enableForwardButton() {
        getActionBarFragment().enableForwardButton()
        glView.requestRender()
//        findViewById<UIButton>(R.id.forward_button).enable()
    }

}