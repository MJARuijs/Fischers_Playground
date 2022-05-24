package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.graphics.Color

enum class GameStatus(val color: Int, val sortingValue: Int) {

    INVITE_PENDING(Color.MAGENTA, 2),
    INVITE_RECEIVED(Color.BLUE, 2),
    PLAYER_MOVE(Color.WHITE, 1),
    OPPONENT_MOVE(Color.BLACK, 0),
    GAME_WON(Color.GREEN, -1),
    GAME_DRAW(Color.YELLOW, -1),
    GAME_LOST(Color.RED, -1)

}