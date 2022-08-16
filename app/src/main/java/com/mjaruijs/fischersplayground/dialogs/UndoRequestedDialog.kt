package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class UndoRequestedDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Undo requested!")
    }

    fun show(gameId: String, opponentUsername: String, id: String) {
        dialogBuilder.setMessage("$opponentUsername is requesting to undo their last move")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "accepted_undo", "$gameId|$id"))
        }
        dialogBuilder.setNegativeButton("Reject") { _, _ ->
            NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "rejected_undo", "$gameId|$id"))
        }

        dialog = dialogBuilder.show()
    }

}