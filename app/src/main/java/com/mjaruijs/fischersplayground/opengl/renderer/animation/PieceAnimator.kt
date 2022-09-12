package com.mjaruijs.fischersplayground.opengl.renderer.animation

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import java.util.concurrent.atomic.AtomicBoolean

class PieceAnimator(val timeStarted: Long, val piece: Piece, requestRender: () -> Unit, var onFinish: () -> Unit, animationDuration: Long = 1000L) : Comparable<PieceAnimator> {

    private val xFinished = AtomicBoolean(false)
    private val yFinished = AtomicBoolean(false)
    private val xAnimator = ValueAnimator.ofFloat(piece.animatedPosition.x, piece.boardPosition.x)
    private val yAnimator = ValueAnimator.ofFloat(piece.animatedPosition.y, piece.boardPosition.y)

    private val onFinishCalls = ArrayList<() -> Unit>()

    init {
        xAnimator.duration = animationDuration
        xAnimator.addUpdateListener {
            piece.animatedPosition.x = it.animatedValue as Float
//            piece.translation.x = it.animatedValue as Float
            requestRender()
        }
        xAnimator.doOnStart {
            println("OnStart position for: ${piece.type} ${piece.team}: ${piece.animatedPosition}, ${piece.boardPosition}")
//            piece.boardPosition.x = piece.newSquare!!.x
        }
        xAnimator.doOnEnd {
            xFinished.set(true)
            if (yFinished.get()) {
                finish()
            }
        }

        yAnimator.duration = animationDuration
        yAnimator.addUpdateListener {
//            println(it.animatedValue as Float)
            piece.animatedPosition.y = it.animatedValue as Float
            requestRender()
        }
        yAnimator.doOnStart {
//            piece.boardPosition.y = piece.newSquare!!.y
        }
        yAnimator.doOnEnd {
            yFinished.set(true)
            if (xFinished.get()) {
                finish()
//                piece.onAnimationFinish()
            }
        }
    }

    fun addOnFinishCall(call: () -> Unit) {
        onFinishCalls += call
    }

    fun start() {
//        piece.shouldAnimate = false
        xAnimator.start()
        yAnimator.start()
    }

    fun finish() {
        println("OnFinish position for: ${piece.type} ${piece.team} : ${piece.animatedPosition}, ${piece.boardPosition}")
        for (call in onFinishCalls) {
            call()
        }
        piece.onAnimationFinish()
        onFinish()
    }

    override fun compareTo(other: PieceAnimator): Int {
        if (other.timeStarted > timeStarted) {
            return -1
        }
        return 1
    }


}