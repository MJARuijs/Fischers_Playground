package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class OpponentOfferedDrawDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder
    private lateinit var onAccept: () -> Unit
    private lateinit var userId: String

    fun create(context: Context, userId: String, onAccept: () -> Unit) {
        dialogBuilder = AlertDialog.Builder(context)
        this.userId = userId
        this.onAccept = onAccept
    }

    fun show(gameId: String, opponentName: String, networkManager: NetworkManager) {
        dialogBuilder.setMessage("$opponentName is offering a draw!")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            onAccept()
        }
        dialogBuilder.setNegativeButton("Decline") { _, _ ->
            networkManager.sendMessage(NetworkMessage(Topic.DRAW_REJECTED, "$gameId|$userId"))
        }

        dialog = dialogBuilder.show()
    }

}