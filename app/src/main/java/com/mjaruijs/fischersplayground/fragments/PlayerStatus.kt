package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color

enum class PlayerStatus(val color: Int) {

    ONLINE(Color.rgb(0.0f, 0.75f, 0.0f)),
    IN_OTHER_GAME(Color.rgb(0.75f, 0.0f, 0.0f)),
    AWAY(Color.YELLOW),
    OFFLINE(Color.rgb(0.5f, 0.5f, 0.5f));

    companion object {

        fun fromString(input: String): PlayerStatus {
            for (value in values()) {
                if (input.uppercase() == value.toString().uppercase()) {
                    return value
                }
            }

            throw IllegalArgumentException("Could not parse input into a PlayerStatus: $input")
        }

    }

}