package com.mjaruijs.fischersplayground.networking.message

enum class Topic {

    SET_USER_ID,
    SET_USER_NAME,
    FIRE_BASE_TOKEN,
    SEARCH_PLAYERS,
    NEW_GAME,
    INVITE,
    INVITE_ACCEPTED,
    INVITE_REJECTED,
    MOVE,
    RESIGN,
    UNDO_REQUESTED,
    UNDO_ACCEPTED,
    UNDO_REJECTED,
    DRAW_OFFERED,
    DRAW_ACCEPTED,
    DRAW_REJECTED,
    CHAT_MESSAGE,
    USER_STATUS_CHANGED,
    NEWS,
    RECONNECT_TO_SERVER;

    companion object {
        fun fromString(value: String): Topic {
            for (topic in values()) {
                if (value == topic.toString()) {
                    return topic
                }
            }

            throw IllegalArgumentException("Tried to parse $value into a topic, but failed..")
        }
    }

}