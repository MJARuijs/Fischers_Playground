package com.mjaruijs.fischersplayground.networking.message

import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString

enum class Topic(val dataType: Parcelable.Creator<*>? = null) {

    SET_USER_ID,
    SET_USER_NAME,
    CHANGE_USER_NAME,
    FIRE_BASE_TOKEN,
    SEARCH_PLAYERS,
    NEW_GAME(GameCardItem),
    INVITE(InviteData),
    INVITE_ACCEPTED,
    INVITE_REJECTED,
    MOVE(MoveData),
    UNDO_REQUESTED(ParcelableString),
    UNDO_ACCEPTED(ParcelablePair),
    UNDO_REJECTED(ParcelableString),
    RESIGN(ParcelableString),
    DRAW_OFFERED(ParcelableString),
    DRAW_ACCEPTED(ParcelableString),
    DRAW_REJECTED(ParcelableString),
    CHAT_MESSAGE(ChatMessage.Data),
    USER_STATUS_CHANGED(ParcelableString),
    HEART_BEAT,
    NEWS,
    CONFIRM_MESSAGE,
    RECONNECT_TO_SERVER,
    CRASH_REPORT,
    NEW_OPENING,
    COMPARE_OPENINGS(ParcelableString),
    RESTORE_OPENINGS;

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