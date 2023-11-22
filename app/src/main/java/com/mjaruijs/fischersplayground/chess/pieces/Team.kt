package com.mjaruijs.fischersplayground.chess.pieces

import android.graphics.Color
import java.lang.IllegalArgumentException

enum class Team(val color: Int) {

    BLACK(Color.BLACK),
    WHITE(Color.WHITE);

    operator fun not(): Team {
        return if (this == BLACK) {
            WHITE
        } else {
            BLACK
        }
    }

    companion object {
        fun fromString(value: String): Team {
            if (value == "WHITE") return WHITE
            if (value == "BLACK") return BLACK
            throw IllegalArgumentException("Failed to parse $value into a team")
        }
    }

}