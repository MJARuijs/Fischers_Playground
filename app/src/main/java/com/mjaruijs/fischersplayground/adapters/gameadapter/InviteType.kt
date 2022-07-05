package com.mjaruijs.fischersplayground.adapters.gameadapter

enum class InviteType {

    PENDING,
    RECEIVED;

    companion object {

        fun fromString(content: String): InviteType {
            if (content.uppercase() == "PENDING") {
                return PENDING
            }
            if (content.uppercase() == "RECEIVED") {
                return RECEIVED
            }
            throw IllegalArgumentException("Could not parse string into InviteType: $content")
        }

    }
}