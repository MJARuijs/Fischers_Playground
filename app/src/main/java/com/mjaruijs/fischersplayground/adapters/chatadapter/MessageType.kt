package com.mjaruijs.fischersplayground.adapters.chatadapter

enum class MessageType {

    SENT,
    RECEIVED;

    companion object {

        fun fromString(string: String): MessageType {
            if (string.uppercase() == "SENT") {
                return SENT
            }
            if (string.uppercase() == "RECEIVED") {
                return RECEIVED
            }
            throw IllegalArgumentException("Could not parse string into MessageType: $string")
        }
    }

}