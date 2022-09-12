package com.mjaruijs.fischersplayground.opengl.renderer.animation

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class AnimationData(val timeStamp: Long, val piece: Piece, val fromPosition: Vector2, val toPosition: Vector2, val onAnimationFinished: () -> Unit = {}) : Comparable<AnimationData> {
    override fun compareTo(other: AnimationData): Int {
        if (other.timeStamp > timeStamp) {
            return -1
        }
        return 1
    }
}