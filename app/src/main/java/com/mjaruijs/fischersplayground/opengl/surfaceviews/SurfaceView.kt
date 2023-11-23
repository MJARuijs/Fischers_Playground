package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity.Companion.GRAPHICS_3D_KEY
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread
import com.mjaruijs.fischersplayground.util.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class SurfaceView(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private lateinit var renderer: OpenGLRenderer
    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onClick: (Float, Float) -> Unit
//    private lateinit var onLongClick: (Float, Float) -> Unit
    private lateinit var runOnUiThread: (() -> Unit) -> Unit

    private var startClickTimer = -1L
    private val holding = AtomicBoolean(false)
    private val holdingX = AtomicInteger(-1)
    private val holdingY = AtomicInteger(-1)
//    private val timerThread = FixedRateThread(10f, ::update)

    private var is3D = false

//    private var previousTime = 0L

    init {
        setEGLContextClientVersion(3)
//        setEGLConfigChooser(ConfigChooser(context))

        val preferences = context.getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        is3D = preferences.getBoolean(GRAPHICS_3D_KEY, is3D)

        renderer = OpenGLRenderer(context, resources, ::onContextCreated, ::requestRender, is3D)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
//        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun init(runOnUiThread: (() -> Unit) -> Unit, onSurfaceCreated: () -> Unit, onClick: (Float, Float) -> Unit, onDisplaySizeChanged: (Int, Int) -> Unit, isPlayerWhite: Boolean, onExceptionThrown: (String, Exception) -> Unit) {
        this.runOnUiThread = runOnUiThread
        this.onSurfaceCreated = onSurfaceCreated
        this.onClick = onClick
//        this.onLongClick = onLongClick

        renderer.runOnUiThread = runOnUiThread
        renderer.onDisplaySizeChanged = onDisplaySizeChanged
        renderer.isPlayerWhite = isPlayerWhite
        renderer.onExceptionThrown = onExceptionThrown

//        Thread {
//            while (display == null) {
//                Thread.sleep(1)
//            }
//
//            timerThread.setTps(display.refreshRate)
//        }.start()
    }

//    private fun update() {
//        val currentTime = System.nanoTime()
//        val diff = currentTime - previousTime
//        previousTime = currentTime
//        renderer.update(diff.toFloat() / 1000000000f)
//        Logger.debug(TAG, "Update: ${(diff.toFloat() / 1000000000f)}")
//    }

    fun getRenderer() = renderer

    private fun onContextCreated() {
//        timerThread.run()
        onSurfaceCreated()
    }

    fun setGame(game: Game) {
        renderer.setGame(game)
    }

    fun highlightSquare(square: Vector2) {
        renderer.addHighlightedSquare(square)
    }

    fun removeHighlightedSquare(square: Vector2) {
        renderer.removeHighlightedSquare(square)
    }

    fun clearHighlightedSquares() {
        renderer.clearHighlightedSquares()
    }

    fun enableLastMoveHighlights() {
        renderer.enableLastMoveHighlights()
    }

    fun disableLastMoveHighlights() {
        renderer.disableLastMoveHighlights()
        requestRender()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            startClickTimer = System.currentTimeMillis()
//            holdingX.set(event.x.roundToInt())
//            holdingY.set(event.y.roundToInt())

//            Thread {
//                holding.set(true)
//                while (holding.get()) {
//                    if (System.currentTimeMillis() - startClickTimer >= LONG_CLICK_DURATION) {
//                        holding.set(false)
//                        startClickTimer = -1
//                        runOnUiThread {
//                            requestRender()
//                        }
//                    }
//                }
//            }.start()
        }

        if (event.action == MotionEvent.ACTION_CANCEL) {
            startClickTimer = -1
        }

        if (event.action == MotionEvent.ACTION_UP) {
//            if (holding.get()) {
//                holding.set(false)
//                if (System.currentTimeMillis() - startClickTimer < MAX_CLICK_DURATION) {
                    onClick(event.x, event.y)
//                }
//
//                startClickTimer = -1
//            }

            requestRender()
        }

        return true
    }

    fun destroy() {
//        timerThread.stop()
        renderer.destroy()
    }

    companion object {
        private const val TAG = "SurfaceView"
        private const val MAX_CLICK_DURATION = 250L
        private const val LONG_CLICK_DURATION = 500L
    }

}