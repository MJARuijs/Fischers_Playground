package com.mjaruijs.fischersplayground.activities.game

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
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
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.userinterface.BoardOverlay
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

abstract class GameActivity : AppCompatActivity() {

    open var activityName: String = "Game_Activity"

    lateinit var glView: SurfaceView
    lateinit var boardOverlay: BoardOverlay

//    protected lateinit var gameLayout: ConstraintLayout
    protected lateinit var vibrator: Vibrator

    protected val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    protected var displayWidth = 0
    protected var displayHeight = 0

    abstract var isSinglePlayer: Boolean

    protected var isPlayingWhite = true
    protected lateinit var dataManager: DataManager

    open lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
            val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

            hideActivityDecorations(fullScreen)
            dataManager = DataManager.getInstance(this)

            pieceChooserDialog.create(this)

            boardOverlay = findViewById(R.id.board_overlay)
            glView = findViewById(R.id.opengl_view)
            glView.init(::runOnUIThread, ::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite, ::onExceptionThrown)
        } catch (e: Exception) {
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        pieceChooserDialog.setLayout()
    }

    override fun onStop() {
        super.onStop()
        Logger.debug(activityName, "Stopping activity")
        pieceChooserDialog.dismiss()
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }

    private fun onExceptionThrown(fileName: String, e: Exception) {
//        networkManager.sendCrashReport(fileName, e.stackTraceToString(), applicationContext)
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
        game.onPawnPromoted = ::onPromotePawn
        game.onMoveMade = ::onMoveMade
    }

    open fun onMoveMade(move: Move) {
        Thread {
            while (getActionBarFragment() == null) {
                Logger.warn(activityName, "Move made, but thread is looping because actionBarFragment is null..")
                Thread.sleep(1000)
            }

            runOnUiThread {
                evaluateNavigationButtons()
            }
        }.start()
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
//            networkManager.sendCrashReport("crash_onclick_log.txt", e.stackTraceToString(), applicationContext)
        }
    }
//
//    open fun onLongClick(x: Float, y: Float) {
//
//    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    protected fun requestRender() {
        glView.requestRender()
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

    private fun runOnUIThread(runnable: () -> Unit) {
        runOnUiThread {
            runnable()
        }
    }

    private fun onPromotePawn(square: Vector2, team: Team): PieceType {
        runOnUiThread {
            pieceChooserDialog.show(square, team)
        }
        return PieceType.QUEEN
    }

    protected fun dpToPx(@Suppress("SameParameterValue") dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}