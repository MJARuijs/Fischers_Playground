package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.content.Context.MODE_PRIVATE
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
    private lateinit var runOnUiThread: (() -> Unit) -> Unit

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(ConfigChooser())

        val preferences = context.getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)

        renderer = OpenGLRenderer(context, resources, ::onContextCreated, is3D)
//        renderer = OpenGLRenderer.getInstance(context, ::onContextCreated, is3D)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun init(runOnUiThread: (() -> Unit) -> Unit, onSurfaceCreated: () -> Unit, onClick: (Float, Float) -> Unit, onDisplaySizeChanged: (Int, Int) -> Unit, isPlayerWhite: Boolean) {
        this.runOnUiThread = runOnUiThread
        this.onSurfaceCreated = onSurfaceCreated
        this.onClick = onClick

        renderer.requestRender = ::requestRender
        renderer.onDisplaySizeChanged = onDisplaySizeChanged
        renderer.isPlayerWhite = isPlayerWhite
        renderer.runOnUiThread = runOnUiThread
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
//        val animating = renderer.update()

//        if (animating) {
//            requestRender()
//        }
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