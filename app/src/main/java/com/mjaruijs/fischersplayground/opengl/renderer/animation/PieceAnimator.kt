package com.mjaruijs.fischersplayground.opengl.renderer.animation

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.mjaruijs.fischersplayground.chess.game.ArrayBasedGameState
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import java.util.concurrent.atomic.AtomicBoolean

class PieceAnimator(state: ArrayBasedGameState, piecePosition: Vector2, translation: Vector2, requestRender: () -> Unit, var onFinish: () -> Unit, animationDuration: Long = 500L) {

    private val xFinished = AtomicBoolean(false)
    private val yFinished = AtomicBoolean(false)
    private val onStartExecuted = AtomicBoolean(false)

    private val piece = state[piecePosition]!!
    private val xAnimator = ValueAnimator.ofFloat(translation.x, 0.0f)
    private val yAnimator = ValueAnimator.ofFloat(translation.y, 0.0f)

    private val onFinishCalls = ArrayList<() -> Unit>()

    init {
        xAnimator.duration = animationDuration
        xAnimator.addUpdateListener {
            piece.translation.x = it.animatedValue as Float
            requestRender()
        }
        xAnimator.doOnStart {
            if (!onStartExecuted.get()) {
                onStartExecuted.set(true)
                println("EXECUTING ON START")
                onFinish()
            }
        }
        xAnimator.doOnEnd {
            xFinished.set(true)
            if (yFinished.get()) {
                finish()
            }
        }

        yAnimator.duration = animationDuration
        yAnimator.addUpdateListener {
            piece.translation.y = it.animatedValue as Float
            requestRender()
        }
        yAnimator.doOnStart {
            if (!onStartExecuted.get()) {
                onStartExecuted.set(true)
                println("EXECUTING ON START")
                onFinish()
            }
        }
        yAnimator.doOnEnd {
            yFinished.set(true)
            if (xFinished.get()) {
                finish()
            }
        }
    }

    fun addOnFinishCall(call: () -> Unit) {
        onFinishCalls += call
    }

    fun start() {
        xAnimator.start()
        yAnimator.start()
    }

    private fun finish() {
        for (call in onFinishCalls) {
            call()
        }
    }

}