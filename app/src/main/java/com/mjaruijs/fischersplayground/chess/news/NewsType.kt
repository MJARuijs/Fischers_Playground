package com.mjaruijs.fischersplayground.chess.news

enum class NewsType {

    OPPONENT_MOVED,
    OPPONENT_RESIGNED,
    OPPONENT_OFFERED_DRAW,
    OPPONENT_ACCEPTED_DRAW,
    OPPONENT_REJECTED_DRAW,
    OPPONENT_REQUESTED_UNDO,
    OPPONENT_ACCEPTED_UNDO,
    OPPONENT_REJECTED_UNDO,
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