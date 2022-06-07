package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color

enum class PlayerStatus(val color: Int) {

    OFFLINE(Color.rgb(0.5f, 0.5f, 0.5f)),
    AWAY(Color.YELLOW),
    IN_OTHER_GAME(Color.RED),
    IN_GAME(Color.rgb(0.0f, 0.75f, 0.0f))

}