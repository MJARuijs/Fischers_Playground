package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionButtonsFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.SurfaceView
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger

abstract class GameActivity : ClientActivity() {

    private lateinit var checkMateDialog: SingleButtonDialog
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

        try {
            val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
            val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

            hideActivityDecorations(fullScreen)

            pieceChooserDialog.create(this)

            userId = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, "")!!
            userName = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_NAME_KEY, "")!!
            isSinglePlayer = (this is SinglePlayerGameActivity)

            if (this is SinglePlayerGameActivity) {
                opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
                isPlayingWhite = intent.getBooleanExtra("is_playing_white", true)
            }

            glView = findViewById(R.id.opengl_view)
            glView.init(::runOnUIThread, ::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite)

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

    override fun onResume() {
        super.onResume()

        checkMateDialog = SingleButtonDialog(this, "Checkmate!", "Exit", ::closeAndSaveGameAsWin)
        stayingInApp = false

        pieceChooserDialog.setLayout()
    }

    override fun onStop() {
        super.onStop()

        checkMateDialog.destroy()
        pieceChooserDialog.destroy()
    }

    override fun onDestroy() {
        glView.destroy()

        super.onDestroy()
    }

    private fun runOnUIThread(runnable: () -> Unit) {
        runOnUiThread {
            runnable()
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

        // TODO: Uncomment this part
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.player_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
            replace(R.id.opponent_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
        }
    }

    fun getActionBarFragment(): ActionButtonsFragment? {
        val fragment = supportFragmentManager.fragments.find { fragment -> fragment is ActionButtonsFragment }
        if (fragment != null) {
            return fragment as ActionButtonsFragment
        }
        return null
    }

    open fun onContextCreated() {
        restorePreferences()
    }

    fun setGameCallbacks() {
        game.onPawnPromoted = ::onPawnPromoted
        game.enableBackButton = ::enableBackButton
        game.enableForwardButton = ::enableForwardButton
        game.disableBackButton = ::disableBackButton
        game.disableForwardButton = ::disableForwardButton
        game.onPieceTaken = ::onPieceTaken
        game.onPieceRegained = ::onPieceRegained
        game.onCheckMate = ::onCheckMate

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
            requestRender()
        }.start()
    }

    open fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    open fun onClick(x: Float, y: Float) {
        try {
            game.onClick(x, y, displayWidth, displayHeight)
        } catch (e: Exception) {
            Logger.log(applicationContext, e.stackTraceToString(), "onclick_crash_log.txt")
        }
    }

    protected fun requestRender() {
        glView.requestRender()
    }

    private fun onCheckMate(team: Team) {
        runOnUiThread {
            if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
                checkMateDialog.setMessage("You won!")
                    .setOnClick { closeAndSaveGameAsWin() }
                    .show()
            } else {
                checkMateDialog.setMessage("$opponentName has won!")
                    .setOnClick { closeAndSaveGameAsLoss() }
                    .show()
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

    open fun finishActivity(status: GameStatus) {
        stayingInApp = true
        finish()
    }

    open fun closeAndSaveGameAsWin() {
        finishActivity(GameStatus.GAME_WON)
    }

    open fun closeAndSaveGameAsDraw() {
        finishActivity(GameStatus.GAME_DRAW)
    }

    open fun closeAndSaveGameAsLoss() {
        finishActivity(GameStatus.GAME_LOST)
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

    open fun setGameParameters(game: MultiPlayerGame) {
        if (game.moves.isNotEmpty()) {
            if (game.getMoveIndex() != -1) {
                getActionBarFragment()?.enableBackButton()
            }
            if (!game.isShowingCurrentMove()) {
                getActionBarFragment()?.enableForwardButton()
            }
        }
    }

    private fun onPawnPromoted(square: Vector2, team: Team): PieceType {
        runOnUiThread { pieceChooserDialog.show(square, team) }
        return PieceType.QUEEN
    }

    private fun enableBackButton() {
        getActionBarFragment()?.enableBackButton()
        requestRender()
    }

    private fun enableForwardButton() {
        getActionBarFragment()?.enableForwardButton()
        requestRender()
    }

    private fun disableBackButton() {
        getActionBarFragment()?.disableBackButton()
        requestRender()
    }

    private fun disableForwardButton() {
        getActionBarFragment()?.disableForwardButton()
        requestRender()
    }

}