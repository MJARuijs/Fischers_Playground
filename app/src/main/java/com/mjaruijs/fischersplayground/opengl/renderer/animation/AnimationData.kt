package com.mjaruijs.fischersplayground.opengl.renderer.animation

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class AnimationData(val timeStamp: Long, val piecePosition: Vector2, val translation: Vector2, val takenPiece: Piece?, val takenPiecePosition: Vector2?, var onStart: () -> Unit, var onFinish: () -> Unit, var nextAnimation: AnimationData?, var runInBackground: Boolean = true) : Comparable<AnimationData> {

    val onStartCalls = ArrayList<() -> Unit>()

    val onFinishCalls = ArrayList<() -> Unit>()

    init {
        onStartCalls += onStart
        onFinishCalls += onFinish
    }

    fun invokeOnStartCalls() {
        for (call in onStartCalls) {
            call.invoke()
        }
    }

    fun invokeOnFinishCalls() {
        for (call in onFinishCalls) {
            call.invoke()
        }
    }

    override fun compareTo(other: AnimationData): Int {
        if (other.timeStamp > timeStamp) {
            return -1
        }
        return 1
    }
}