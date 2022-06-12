package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.GRAPHICS_3D_KEY
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread

class SurfaceView(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private val tickRate = 60.0f

    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    private var renderer: OpenGLRenderer
    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onClick: (Float, Float) -> Unit

    private val paint = Paint()

    init {
        paint.color = Color.rgb(0f, 0f, 1f)
        setEGLContextClientVersion(3)

        val preferences = context.getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)

        renderer = OpenGLRenderer(context, ::onContextCreated, is3D)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun init(onSurfaceCreated: () -> Unit, onClick: (Float, Float) -> Unit, onDisplaySizeChanged: (Int, Int) -> Unit, isPlayerWhite: Boolean) {
        this.onSurfaceCreated = onSurfaceCreated
        this.onClick = onClick

        renderer.onDisplaySizeChanged = onDisplaySizeChanged
        renderer.isPlayerWhite = isPlayerWhite
    }

    fun getRenderer() = renderer

    private fun onContextCreated() {
        fixedRateThread.run()
        onSurfaceCreated()
    }

    fun setGame(game: Game) {
        renderer.setGame(game)
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