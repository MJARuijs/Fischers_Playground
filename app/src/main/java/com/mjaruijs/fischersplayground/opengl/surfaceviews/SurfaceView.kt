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

class SurfaceView(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private val renderer: OpenGLRenderer
    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onClick: (Float, Float) -> Unit
    private lateinit var runOnUiThread: (() -> Unit) -> Unit

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(ConfigChooser(context))

        val preferences = context.getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        val is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)

        renderer = OpenGLRenderer(context, resources, ::onContextCreated, ::requestRender, is3D)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun init(runOnUiThread: (() -> Unit) -> Unit, onSurfaceCreated: () -> Unit, onClick: (Float, Float) -> Unit, onDisplaySizeChanged: (Int, Int) -> Unit, isPlayerWhite: Boolean) {
        this.runOnUiThread = runOnUiThread
        this.onSurfaceCreated = onSurfaceCreated
        this.onClick = onClick

        renderer.runOnUiThread = runOnUiThread
        renderer.onDisplaySizeChanged = onDisplaySizeChanged
        renderer.isPlayerWhite = isPlayerWhite
    }

    fun getRenderer() = renderer

    private fun onContextCreated() {
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

    @SuppressLint("ClickableViewAccessibility")
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
        renderer.destroy()
    }

}