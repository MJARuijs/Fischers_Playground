package com.mjaruijs.fischersplayground.opengl.renderer.animation

import com.mjaruijs.fischersplayground.chess.game.ArrayBasedGameState
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class AnimationData(val timeStamp: Long, val state: ArrayBasedGameState, val piecePosition: Vector2, val translation: Vector2, val takenPiece: Piece?, val takenPiecePosition: Vector2?, val onStart: () -> Unit, val onFinish: () -> Unit, var nextAnimation: AnimationData?) : Comparable<AnimationData> {
    override fun compareTo(other: AnimationData): Int {
        if (other.timeStamp > timeStamp) {
            return -1
        }
        return 1
    }
}