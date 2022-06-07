package com.mjaruijs.fischersplayground.opengl.surfaceviews

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.CAMERA_ZOOM_KEY
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.renderer.OpenGLRenderer
import com.mjaruijs.fischersplayground.util.FixedRateThread
import kotlin.math.abs

class GameSettingsSurface(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {

    private val tickRate = 60.0f

    private var renderer: OpenGLRenderer
    private val fixedRateThread = FixedRateThread(tickRate, ::update)

    private var onSurfaceCreated: () -> Unit = {}
    private lateinit var onCameraRotated: () -> Unit
    private lateinit var savePreference: (String, Float) -> Unit

    private var oldX = 0f
    private var oldY = 0f

    private var isZooming = false
    var isActive = false

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

    private var currentDistance = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || !isActive) {
            return false
        }

        if (event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 2) {
            isZooming = true

            val pointerId1 = event.getPointerId(0)
            val pointerId2 = event.getPointerId(1)

            val coords1 = MotionEvent.PointerCoords()
            val coords2 = MotionEvent.PointerCoords()

            try {
                event.getPointerCoords(pointerId1, coords1)
                event.getPointerCoords(pointerId2, coords2)

                val pointer1Location = Vector2(coords1.x, coords1.y)
                val pointer2Location = Vector2(coords2.x, coords2.y)

                val absoluteDifference = (pointer1Location - pointer2Location).absolute()
                val distance = absoluteDifference.length()

                if (currentDistance == 0f) {
                    currentDistance = distance
                }

                val difference = (distance - currentDistance)

                if (difference == 0f) {
                    return true
                }

                val scale = 100f

                if (abs(difference) > 1f) {
                    if (difference > 0) {
                        renderer.incrementCameraZoom(-difference / scale)
                    } else if (difference < 0) {
                        renderer.incrementCameraZoom(-difference / scale)
                    }

                    currentDistance = distance
                    savePreference(CAMERA_ZOOM_KEY, renderer.getCameraZoom())
                }

                requestRender()
            } catch (e: IllegalArgumentException) {

            }
            return true
        }

        if (isZooming && event.action == MotionEvent.ACTION_UP) {
            isZooming = false
            currentDistance = 0f
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
}