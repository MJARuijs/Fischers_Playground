package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color

enum class PlayerStatus(val color: Int) {

    OFFLINE(Color.rgb(0.5f, 0.5f, 0.5f)),
    ONLINE(Color.YELLOW),
    IN_GAME(Color.rgb(0.0f, 0.75f, 0.0f))

}