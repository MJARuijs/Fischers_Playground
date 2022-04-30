package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread

class SurfaceView(context: Context, gameId: String, isPlayingWhite: Boolean) : GLSurfaceView(context) {

    private val tickRate = 60.0f

    private val renderer: OpenGLRenderer
    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    private var animating = false

    init {
        setEGLContextClientVersion(3)

        renderer = OpenGLRenderer(context, gameId, isPlayingWhite, ::onContextCreated)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun onContextCreated() {
        fixedRateThread.run()
    }

    fun move(fromPosition: Vector2, toPosition: Vector2) {
        renderer.move(fromPosition, toPosition)
        requestRender()
    }

    private fun update() {
        animating = renderer.update(1.0f / tickRate)

        if (animating) {
            requestRender()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            renderer.onTouch(event.x, event.y)
            requestRender()
        }

        return true
    }

    fun saveGame() {

    }

    fun destroy() {
        fixedRateThread.stop()
        renderer.destroy()
    }

}