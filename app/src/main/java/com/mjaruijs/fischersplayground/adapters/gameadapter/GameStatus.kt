package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.graphics.Color
import java.util.*

enum class GameStatus(val color: Int, val sortingValue: Int) {

    INVITE_PENDING(Color.MAGENTA, 2),
    INVITE_RECEIVED(Color.BLUE, 2),
    PLAYER_MOVE(Color.WHITE, 1),
    OPPONENT_MOVE(Color.BLACK, 0),
    GAME_WON(Color.GREEN, -1),
    GAME_DRAW(Color.YELLOW, -1),
    GAME_LOST(Color.RED, -1);

    companion object {
        fun fromString(content: String): GameStatus {
            for (value in values()) {
                if (content.uppercase(Locale.ROOT) == value.toString()) {
                    return value
                }
            }

            throw IllegalArgumentException("Could not make a GameStatus out of $content")
        }
    }



}