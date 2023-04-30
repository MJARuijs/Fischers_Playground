package com.mjaruijs.fischersplayground.networking.message

import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.parcelable.ParcelableNull
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString

enum class Topic(val dataType: Parcelable.Creator<*>? = null) {

    SET_ID,
    SET_ID_AND_NAME,
    ID_LOGIN,
    EMAIL_LOGIN,
    UNKNOWN_EMAIL,
    ACCOUNT_ALREADY_EXISTS,
    CREATE_ACCOUNT,
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
    CHAT_MESSAGE(ChatMessage),
    USER_STATUS_CHANGED(ParcelableString),
    HEART_BEAT,
    CONFIRM_MESSAGE,
    CRASH_REPORT,
    NEW_OPENING,
    DELETE_OPENING,
    NEW_PRACTICE_SESSION,
    DELETE_PRACTICE_SESSION,
    COMPARE_DATA(ParcelableString),
    RESTORE_DATA(ParcelableNull),
    DELETE,
    SERVER_IP_CHANGED,
    DEBUG,
    RESEND_MESSAGE;

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