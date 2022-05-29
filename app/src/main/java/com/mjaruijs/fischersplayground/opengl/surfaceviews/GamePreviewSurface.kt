package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.listeners.ScaleListener
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer

class GamePreviewSurface(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private val tickRate = 60.0f

    private val renderer: OpenGLRenderer
//    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onCameraRotated: () -> Unit

    private var oldX = 0f
    private var oldY = 0f

    private val scaleListener = ScaleListener(1.0f, ::onZoomChanged)
    private var scaleGestureDetector = ScaleGestureDetector(context, scaleListener)

    private var isZooming = false
    private var zoomLevel = 0f

    init {
        setEGLContextClientVersion(3)


        renderer = OpenGLRenderer(context, ::onContextCreated, true)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun init(onSurfaceCreated: () -> Unit, onCameraRotated: () -> Unit) {
        this.onSurfaceCreated = onSurfaceCreated
        this.onCameraRotated = onCameraRotated
    }

    private fun onZoomChanged(zoom: Float) {
        if (zoom < 1.0f) {
            zoomLevel -= zoom * 100
        } else {
            zoomLevel += zoom * 100
        }
        println("ZOOM CHANGED: $zoom $zoomLevel")

//        if (zoomLevel > 10.0f) {
//            zoomLevel = 0.0f
//        }
        renderer.zoomCamera(zoomLevel)
        requestRender()
    }

    private fun onContextCreated() {
//        fixedRateThread.run()
        onSurfaceCreated()
    }

    fun getRenderer() = renderer

    fun setGame(game: Game) {
        renderer.setGame(game)
    }

    private fun update() {
//        val animating = renderer.update(1.0f / tickRate)

//        if (animating) {
//            requestRender()
//        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.pointerCount >= 2) {
            isZooming = true
            return scaleGestureDetector.onTouchEvent(event)
        }

        if (isZooming && event.action == MotionEvent.ACTION_UP) {
            isZooming = false
        }

        if (isZooming) {
            return false
        }

        val rotationSpeed = 0.001f;

        if (event.action == MotionEvent.ACTION_MOVE) {
            val dY = event.y - oldY

            if (dY != 0.0f) {
                renderer.rotateCamera(Vector3(-dY * rotationSpeed, 0f, 0f))
                onCameraRotated()
            }
            requestRender()
        }

        oldX = event.x
        oldY = event.y
        return true
    }

    fun destroy() {
//        fixedRateThread.stop()
        renderer.destroy()
    }

}