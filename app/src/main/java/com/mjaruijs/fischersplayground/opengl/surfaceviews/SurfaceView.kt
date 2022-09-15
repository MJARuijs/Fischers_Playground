package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.opengl.EGL14.*
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.GRAPHICS_3D_KEY
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLContext

class SurfaceView(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private var renderer: OpenGLRenderer
    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onClick: (Float, Float) -> Unit
    private lateinit var runOnUiThread: (() -> Unit) -> Unit

//    private lateinit var renderThread: RenderThread

    init {
//        eglMakeCurrent()
        setEGLContextClientVersion(3)
        setEGLConfigChooser(ConfigChooser())
        preserveEGLContextOnPause = true

//        val eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)
//        eglBindAPI(EGL_OPENGL_ES_API)
//        eglCreateContext(eglDisplay)

        val preferences = context.getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)

        println("Creating surfaceView: $preserveEGLContextOnPause")

//        renderThread = RenderThread()

        val creatingRenderer = AtomicBoolean(true)

//        renderThread.start()
//        renderThread.addTask {
            renderer = OpenGLRenderer(context, resources, ::onContextCreated, is3D)
//            creatingRenderer.set(false)
//        }

//        while (creatingRenderer.get()) {
//            Thread.sleep(1)
//        }

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
        onSurfaceCreated()
    }

    fun setGame(game: Game) {
        renderer.setGame(game)
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
        renderer.destroy()
    }

}