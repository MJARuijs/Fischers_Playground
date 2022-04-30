package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.GameState
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread

class SurfaceView(context: Context, private val onSurfaceCreated: () -> Unit, private val onClick: (Float, Float) -> Unit, onDisplaySizeChanged: (Int, Int) -> Unit) : GLSurfaceView(context) {

    private val tickRate = 60.0f

    private val renderer: OpenGLRenderer
    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    init {
        setEGLContextClientVersion(3)

        renderer = OpenGLRenderer(context, ::onContextCreated, onDisplaySizeChanged)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun onContextCreated() {
        fixedRateThread.run()
        onSurfaceCreated()
    }

    fun setBoard(board: Board) {
        renderer.setBoard(board)
    }

    fun setGameState(gameState: GameState) {
        renderer.setGameState(gameState)
    }

    private fun update() {
        val animating = renderer.update(1.0f / tickRate)

        if (animating) {
            requestRender()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            onClick(event.x, event.y)
            requestRender()
        }

        return true
    }

    fun destroy() {
        fixedRateThread.stop()
        renderer.destroy()
    }

}