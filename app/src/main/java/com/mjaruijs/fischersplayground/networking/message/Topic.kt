package com.mjaruijs.fischersplayground.networking.message

enum class Topic {

    GAME_UPDATE,
    CHAT_MESSAGE,
    INFO;

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