package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer

class GamePreviewSurface(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private lateinit var renderer: OpenGLRenderer
    private var onSurfaceCreated: () -> Unit = {}

    init {
        setEGLContextClientVersion(3)


    }

    fun init(onSurfaceCreated: () -> Unit) {
        renderer = OpenGLRenderer(context, ::onContextCreated, false)

//        setEGLConfigChooser(ConfigChooser())
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        this.onSurfaceCreated = onSurfaceCreated
    }

    private fun onContextCreated() {
        onSurfaceCreated()
    }

    fun getRenderer() = renderer

    fun setGame(game: Game) {
        renderer.setGame(game)
    }

    fun destroy() {
        renderer.destroy()
    }
}