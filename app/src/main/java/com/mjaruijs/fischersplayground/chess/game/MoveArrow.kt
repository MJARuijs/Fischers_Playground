package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class MoveArrow(val startSquare: Vector2, val endSquare: Vector2) {

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other !is MoveArrow) {
            return false
        }
        if (!FloatUtils.compare(startSquare, other.startSquare)) {
            return false
        }
        if (!FloatUtils.compare(endSquare, other.endSquare)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = startSquare.hashCode()
        result = 31 * result + endSquare.hashCode()
        return result
    }

}