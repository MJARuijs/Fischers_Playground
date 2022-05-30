package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.CAMERA_ZOOM_KEY
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread

class GamePreviewSurface(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private val tickRate = 60.0f

    private val renderer: OpenGLRenderer
    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    private lateinit var onSurfaceCreated: () -> Unit
    private lateinit var onCameraRotated: () -> Unit
    private lateinit var savePreference: (String, Float) -> Unit

    private var oldX = 0f
    private var oldY = 0f

    private val scaleListener = ScaleListener()
    private var scaleGestureDetector = ScaleGestureDetector(context, scaleListener)

    private var isZooming = false

    init {
        setEGLContextClientVersion(3)

        renderer = OpenGLRenderer(context, ::onContextCreated, true)
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun init(onSurfaceCreated: () -> Unit, onCameraRotated: () -> Unit, savePreference: (String, Float) -> Unit) {
        this.onSurfaceCreated = onSurfaceCreated
        this.onCameraRotated = onCameraRotated
        this.savePreference = savePreference
    }

    private fun onContextCreated() {
        fixedRateThread.run()
        onSurfaceCreated()
    }

    fun getRenderer() = renderer

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

        val rotationSpeed = 0.001f

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


    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

//        private var scaleFocusX = 0f
//        private var scaleFocusY = 0f
        var scaleCoef = 1.0f

        override fun onScale(detector: ScaleGestureDetector?): Boolean {

            if (detector == null) {
                return false
            }

            val scale = detector.scaleFactor * scaleCoef

            scaleCoef = scale

            renderer.zoomCamera(10.0f - scaleCoef)
            savePreference(CAMERA_ZOOM_KEY, 10.0f - scaleCoef)

            requestRender()

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            invalidate()

//            scaleFocusX = detector.focusX
//            scaleFocusY = detector.focusY

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
//            scaleFocusX = 0f
//            scaleFocusY = 0f
        }

    }

}