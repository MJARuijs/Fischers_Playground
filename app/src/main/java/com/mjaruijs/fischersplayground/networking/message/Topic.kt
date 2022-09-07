package com.mjaruijs.fischersplayground.networking.message

import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.data.UndoAcceptedData
import com.mjaruijs.fischersplayground.dialogs.UndoRequestedDialog

enum class Topic(val dataType: Parcelable.Creator<*>? = null) {

    SET_USER_ID,
    SET_USER_NAME,
    FIRE_BASE_TOKEN,
    SEARCH_PLAYERS,
    NEW_GAME(GameCardItem),
    INVITE(InviteData),
    INVITE_ACCEPTED,
    INVITE_REJECTED,
    MOVE(MoveData),
    RESIGN,
    UNDO_REQUESTED(UndoRequestedDialog.UndoRequestData),
    UNDO_ACCEPTED(UndoAcceptedData),
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