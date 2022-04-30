package com.mjaruijs.fischersplayground.chess.pieces

import java.lang.IllegalArgumentException

enum class Team {

    BLACK,
    WHITE;

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