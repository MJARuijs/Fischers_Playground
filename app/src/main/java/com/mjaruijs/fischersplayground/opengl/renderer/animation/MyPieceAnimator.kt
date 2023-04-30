package com.mjaruijs.fischersplayground.opengl.renderer.animation

import com.mjaruijs.fischersplayground.chess.game.GameState
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.abs
import kotlin.math.sign

class MyPieceAnimator(private val requestRender: () -> Unit) {

    private lateinit var piece: Piece

    private var totalX = 0.0f
    private var totalY = 0.0f
    private var animationDuration = 0L

    private var xFinished = false
    private var yFinished = false

    private val onFinishCalls = ArrayList<() -> Unit>()

    private var running = false

    fun startAnimation(state: GameState, piecePosition: Vector2, translation: Vector2, onStartCalls: ArrayList<() -> Unit>, onFinishCalls: ArrayList<() -> Unit>, animationDuration: Long) {
        piece = state[piecePosition] ?: throw IllegalArgumentException("No piece was found at square: $piecePosition.. Failed to animate..\n$state\n")

        totalX = translation.x
        totalY = translation.y

        this.animationDuration = animationDuration

        xFinished = translation.x == 0.0f
        yFinished = translation.y == 0.0f

        piece.translation = translation

        for (onStartCall in onStartCalls) {
            onStartCall()
        }

        this.onFinishCalls.clear()
        this.onFinishCalls.addAll(onFinishCalls)

        running = true

        Logger.debug(TAG, "Started animation $translation $animationDuration")
    }

    fun update(deltaTime: Float) {
        if (!running) {
            return
        }

        if (!xFinished) {
            if (sign(totalX) < 0.0f) {
                piece.translation.x += abs(totalX) * deltaTime * (1000f / animationDuration.toFloat())
            } else {
                piece.translation.x -= abs(totalX) * deltaTime * (1000f / animationDuration.toFloat())
            }
        }
        if (!yFinished) {
            if (sign(totalY) < 0.0f) {
                piece.translation.y += abs(totalY) * deltaTime * (1000f / animationDuration.toFloat())
            } else {
                piece.translation.y -= abs(totalY) * deltaTime * (1000f / animationDuration.toFloat())
            }
        }

        if ((totalX > 0.0f && piece.translation.x <= 0.0f) || (totalX < 0.0f && piece.translation.x >= 0.0f)) {
            xFinished = true
            piece.translation.x = 0.0f
        }
        if ((totalY > 0.0f && piece.translation.y <= 0.0f) || (totalY < 0.0f && piece.translation.y >= 0.0f)) {
            yFinished = true
            piece.translation.y = 0.0f
        }

        if (xFinished && yFinished) {
            for (onFinishCall in onFinishCalls) {
                onFinishCall()
            }
            Logger.debug(TAG, "FINISHED ANIMATION")
            running = false
        } else {
            requestRender()
        }
    }

    companion object {
        private const val TAG = "MyPieceAnimator"
    }

}