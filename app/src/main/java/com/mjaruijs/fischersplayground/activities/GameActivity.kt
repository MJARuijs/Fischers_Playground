package com.mjaruijs.fischersplayground.activities

import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.GameState
import com.mjaruijs.fischersplayground.chess.SavedGames
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.opengl.SurfaceView

class GameActivity : AppCompatActivity() {

    private val inviteReceiver = MessageReceiver("INFO", "invite", ::onIncomingInvite)
    private val gameUpdateReceiver = MessageReceiver("GAME_UPDATE", "game_update", ::onGameUpdate)
    private val incomingInviteDialog = IncomingInviteDialog()

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")

    private var displayWidth = 0
    private var displayHeight = 0

    private var isPlayingWhite = false
    private lateinit var gameId: String
    private lateinit var opponentName: String

    private lateinit var board: Board
    private lateinit var gameState: GameState

    private lateinit var glView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActivityDecorations()

        incomingInviteDialog.create(this)

        if (!intent.hasExtra("is_playing_white")) {
            throw IllegalArgumentException("Missing essential information: is_player_white")
        }

        isPlayingWhite = intent.getBooleanExtra("is_playing_white", false)
        opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
        gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")

        glView = SurfaceView(this, ::onContextCreated, ::onClick, ::onDisplaySizeChanged)
        setContentView(glView)
    }

    private fun onContextCreated() {
        gameState = SavedGames.get(gameId) ?: GameState(gameId, isPlayingWhite)
        gameState.init()
        board = Board { square ->
            val possibleMoves = gameState.determinePossibleMoves(square)
            board.updatePossibleMoves(possibleMoves)
        }

        glView.setGameState(gameState)
        glView.setBoard(board)
    }

    private fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    private fun onClick(x: Float, y: Float) {
        val clickAction = board.onClick(x, y, displayWidth, displayHeight)
        val boardAction = gameState.processAction(clickAction)

        board.processAction(boardAction)
    }

    private fun onGameUpdate(content: String) {
        val firstSeparatorIndex = content.indexOf('|')
        val secondSeparatorIndex = content.indexOf('|', firstSeparatorIndex + 1)

        val gameId = content.substring(0, firstSeparatorIndex)
        val moveNotation = content.substring(firstSeparatorIndex + 1)
        val move = Move.fromChessNotation(moveNotation)

        gameState.moveOpponent(move)
        glView.requestRender()
    }

    private fun onIncomingInvite(content: String) {
        val firstSeparatorIndex = content.indexOf(';')
        val secondSeparatorIndex = content.indexOf(';', firstSeparatorIndex + 1)

        val invitingUsername = content.substring(0, firstSeparatorIndex)
        val invitingUserId = content.substring(firstSeparatorIndex + 1, secondSeparatorIndex)
        val inviteId = content.substring(secondSeparatorIndex + 1)

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun saveGame() {
        SavedGames.put(gameId, gameState)
    }

    override fun onRestart() {
        super.onRestart()
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
    }

    override fun onStop() {
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(gameUpdateReceiver)
        saveGame()
        super.onStop()
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }
}