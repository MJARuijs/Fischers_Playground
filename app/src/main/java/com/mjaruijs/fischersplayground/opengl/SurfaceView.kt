package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer

class SurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: OpenGLRenderer

    init {
        setEGLContextClientVersion(3)

        renderer = OpenGLRenderer(context)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
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

    fun destroy() {
        renderer.destroy()
    }

}