package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.os.VibrationEffect
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.DoubleButtonDialog
import com.mjaruijs.fischersplayground.dialogs.PieceChooserDialog
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.GameBarFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.SurfaceView
import com.mjaruijs.fischersplayground.util.FileManager
import kotlin.math.roundToInt

abstract class GameActivity : ClientActivity() {

    private lateinit var checkMateDialog: DoubleButtonDialog
    lateinit var glView: SurfaceView

    protected lateinit var gameLayout: ConstraintLayout

    private val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    private var displayWidth = 0
    private var displayHeight = 0

    abstract var isSinglePlayer: Boolean

    protected var isPlayingWhite = true

    lateinit var gameId: String
    lateinit var opponentName: String

    open lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        try {
            gameLayout = findViewById(R.id.game_layout)

            val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
            val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

            hideActivityDecorations(fullScreen)

            pieceChooserDialog.create(this)

            userId = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_ID_KEY, "")!!
            userName = getSharedPreferences(USER_PREFERENCE_FILE, MODE_PRIVATE).getString(USER_NAME_KEY, "")!!

            glView = findViewById(R.id.opengl_view)
            glView.init(::runOnUIThread, ::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite)
        } catch (e: Exception) {
            FileManager.append(this, "game_activity_crash_report.txt", e.stackTraceToString())
        }
    }

    override fun onResume() {
        super.onResume()

        checkMateDialog = DoubleButtonDialog(this, "Checkmate!", "View Board", ::viewBoardAfterFinish, "Exit", ::closeAndSaveGameAsWin, 0.7f)
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


    inline fun <reified T>findFragment(): T? {
        val fragment = supportFragmentManager.fragments.find { fragment -> fragment is T } ?: return null
        return fragment as T
    }

    fun getActionBarFragment() = findFragment<GameBarFragment>()

    open fun evaluateNavigationButtons() {
        if (game.moves.isNotEmpty()) {
            if (game.getMoveIndex() != -1) {
                (getActionBarFragment() as GameBarFragment).enableBackButton()
            } else {
                (getActionBarFragment() as GameBarFragment).disableBackButton()
            }
            if (!game.isShowingCurrentMove()) {
                (getActionBarFragment() as GameBarFragment).enableForwardButton()
            } else {
                (getActionBarFragment() as GameBarFragment).disableForwardButton()
            }
        }
        requestRender()
    }

    open fun onContextCreated() {
        restorePreferences()
    }

    open fun setGameCallbacks() {
        game.onPawnPromoted = ::onPawnPromoted
        game.onMoveMade = ::onMoveMade
    }

    open fun onMoveMade(move: Move) {
//        val actionBar = getActionBarFragment()
//        if (actionBar is GameBarFragment) {
        runOnUiThread {
            evaluateNavigationButtons()
        }
//            actionBar.evaluateNavigationButtons()
//        }
    }

    fun setGameForRenderer() {
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
            val vibrateOnClick = getSharedPreferences(SettingsActivity.GAME_PREFERENCES_KEY, MODE_PRIVATE).getBoolean(SettingsActivity.VIBRATE_KEY, false)
            if (vibrateOnClick) {
                vibrate()
            }
            game.onClick(x, y, displayWidth, displayHeight)
        } catch (e: Exception) {
            networkManager.sendCrashReport("crash_onclick_log.txt", e.stackTraceToString())
        }
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    protected fun requestRender() {
        glView.requestRender()
    }

    open fun onCheckMate(team: Team) {
        runOnUiThread {
            if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
                checkMateDialog.setMessage("You won!")
                    .setRightOnClick { closeAndSaveGameAsWin() }
                    .show()
            } else {
                checkMateDialog.setMessage("$opponentName has won!")
                    .setRightOnClick { closeAndSaveGameAsLoss() }
                    .show()
            }
        }
    }

    protected fun getPlayerFragment(): PlayerCardFragment? {
        return (supportFragmentManager.fragments.find { fragment -> fragment is PlayerCardFragment && fragment.tag == "player" } as PlayerCardFragment?)
    }

    protected fun getOpponentFragment(): PlayerCardFragment? {
        return (supportFragmentManager.fragments.find { fragment -> fragment is PlayerCardFragment && fragment.tag == "opponent" } as PlayerCardFragment?)
    }

    open fun finishActivity(status: GameStatus) {
        stayingInApp = true
        finish()
    }

    open fun viewBoardAfterFinish() {
        checkMateDialog.dismiss()
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

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun onPawnPromoted(square: Vector2, team: Team): PieceType {
        runOnUiThread {
            pieceChooserDialog.show(square, team)
        }
        return PieceType.QUEEN
    }

    protected fun dpToPx(@Suppress("SameParameterValue") dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}