package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class OpponentOfferedDrawDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
    }

    fun show(gameId: String, id: String, opponentName: String, onAccept: () -> Unit) {
        dialogBuilder.setMessage("$opponentName is offering a draw!")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            onAccept()
        }
        dialogBuilder.setNegativeButton("Decline") { _, _ ->
            NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "declined_draw", gameId))
        }

        dialog = dialogBuilder.show()
    }

}