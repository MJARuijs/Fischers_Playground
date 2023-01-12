package com.mjaruijs.fischersplayground.chess.news

import android.os.Parcelable
import com.mjaruijs.fischersplayground.chess.game.MoveData

enum class NewsType(val dataType: Any? = null) {

    OPPONENT_MOVED(MoveData),
    OPPONENT_RESIGNED,
    OPPONENT_OFFERED_DRAW,
    OPPONENT_ACCEPTED_DRAW,
    OPPONENT_REJECTED_DRAW,
    OPPONENT_REQUESTED_UNDO,
    OPPONENT_ACCEPTED_UNDO(Int),
    OPPONENT_REJECTED_UNDO,
    CHAT_MESSAGE,
    NO_NEWS;

    companion object {

        fun fromString(content: String): NewsType {

            for (value in values()) {
               if (value.toString().uppercase() == content.uppercase()) {
                   return value
               }
            }

            throw IllegalArgumentException("Could not match string to NewsType: $content")
        }

    }



}