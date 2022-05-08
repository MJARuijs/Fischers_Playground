package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.graphics.Color

enum class GameStatus(val color: Int, val sortingValue: Int) {

    INVITE_PENDING(Color.MAGENTA, 1),
    INVITE_RECEIVED(Color.BLUE, 1),
    PLAYER_MOVE(Color.WHITE, 0),
    OPPONENT_MOVE(Color.BLACK, 0),
    GAME_WON(Color.GREEN, -1),
    GAME_DRAW(Color.YELLOW, -1),
    GAME_LOST(Color.RED, -1)

}