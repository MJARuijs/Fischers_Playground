package com.mjaruijs.fischersplayground.opengl.renderer.animation

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.mjaruijs.fischersplayground.chess.game.GameState
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import java.util.concurrent.atomic.AtomicBoolean

class PieceAnimator(state: GameState, piecePosition: Vector2, val translation: Vector2, requestRender: () -> Unit, private val onStartCalls: ArrayList<() -> Unit>, private val onFinishCalls: ArrayList<() -> Unit>, animationDuration: Long) {

    private var piece: Piece

    private val xFinished = AtomicBoolean(false)
    private val yFinished = AtomicBoolean(false)
    private val onStartExecuted = AtomicBoolean(false)

    private val xAnimator = ValueAnimator.ofFloat(translation.x, 0.0f)
    private val yAnimator = ValueAnimator.ofFloat(translation.y, 0.0f)

    init {
        piece = state[piecePosition] ?: throw IllegalArgumentException("No piece was found at square: $piecePosition.. Failed to animate..\n$state\n")

//        if (!FloatUtils.compare(translation.x, 0.0f)) {
            xAnimator.duration = animationDuration
            xAnimator.addUpdateListener {
                piece.translationOffset.x = it.animatedValue as Float
//                Logger.debug(TAG, "Animating x translation: ${it.animatedValue} $animationDuration")
                requestRender()
            }
            xAnimator.doOnStart {
                onStart()
            }
            xAnimator.doOnEnd {
                xFinished.set(true)
                if (yFinished.get()) {
                    onFinish()
                }
            }
//        }

//        if (!FloatUtils.compare(translation.y, 0.0f)) {
            yAnimator.duration = animationDuration
            yAnimator.addUpdateListener {
//                Logger.debug(TAG, "Animating y translation: ${it.animatedValue} $animationDuration")
                piece.translationOffset.y = it.animatedValue as Float
                requestRender()
            }
            yAnimator.doOnStart {
                onStart()
            }
            yAnimator.doOnEnd {
                yFinished.set(true)
                if (xFinished.get()) {
                    onFinish()
                }
            }
//        }
    }

    fun addOnFinishCall(vararg call: () -> Unit) {
        onFinishCalls += call
    }

    private fun onStart() {
        if (!onStartExecuted.get()) {
            onStartExecuted.set(true)
            piece.translationOffset = translation
            for (onStartCall in onStartCalls) {
                onStartCall()
            }
        }
    }

    fun start() {
        xAnimator.start()
        yAnimator.start()
    }

    private fun onFinish() {
        Thread {
            for (call in onFinishCalls) {
                call()
            }
        }.start()
    }

    companion object {
        private const val TAG = "PieceAnimator"
    }


}