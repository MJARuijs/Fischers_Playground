package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class UndoRequestedDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder
    private lateinit var onClick: (DialogResult) -> Unit

    fun create(context: Context, onClick: (DialogResult) -> Unit) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Undo requested!")
        this.onClick = onClick
    }

    fun show(gameId: String, opponentUsername: String, id: String) {
        println("SHOWING DIALOG")
        dialogBuilder.setMessage("$opponentUsername is requesting to undo their last move")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            onClick(DialogResult.ACCEPT)
        }
        dialogBuilder.setNegativeButton("Reject") { _, _ ->
            onClick(DialogResult.DECLINE)
        }

        dialog = dialogBuilder.show()
    }

}