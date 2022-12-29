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

        fun parseFromServer(content: String, userId: String): GameStatus {
            val data = content.split("~")
            val type = data[0]
            return if (type == "PLAYER_MOVE") {
                if (data.size == 1) {
                    throw IllegalArgumentException("parseFromServer content does not have enough data to establish PLAYER_MOVE: $content")
                }
                val playerId = data[1]
                if (playerId == userId) {
                    PLAYER_MOVE
                } else {
                    OPPONENT_MOVE
                }
            } else if (type == "GAME_OVER") {
                if (data.size == 1) {
                    throw IllegalArgumentException("parseFromServer content does not have enough data to establish GAME_OVER: $content")
                }
                val winnerId = data[1]
                if (winnerId == userId) {
                    GAME_WON
                } else {
                    GAME_LOST
                }
            } else if (type == "GAME_DRAW") {
                GAME_DRAW
            } else {
                throw IllegalArgumentException("Failed to parse content from server into GameStatus: $content")
            }
        }

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