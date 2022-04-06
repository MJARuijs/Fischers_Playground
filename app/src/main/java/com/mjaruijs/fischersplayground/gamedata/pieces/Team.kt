package com.mjaruijs.fischersplayground.gamedata.pieces

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

}