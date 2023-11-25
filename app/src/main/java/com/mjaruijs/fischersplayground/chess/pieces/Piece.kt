package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

class Piece(var type: PieceType, val team: Team, var square: Vector2, var translationOffset: Vector2 = Vector2()) {

    constructor(type: PieceType, team: Team, square: Vector2) : this(type, team, square, Vector2())

    fun getFile() = square.x.roundToInt()

    fun getRank() = square.y.roundToInt()

    fun moveTo(newSquare: Vector2) {
        Logger.debug(TAG, "Moving piece at $square to $newSquare")
        square = newSquare
    }

    fun copy(): Piece {
        return Piece(type, team, square.copy())
    }

    companion object {
        private const val TAG = "Piece"
    }
}