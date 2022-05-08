package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context

class OpponentAcceptedDrawDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("It's a draw!")
    }

    fun show(gameId: String, opponentUsername: String, onClick: () -> Unit) {
        dialogBuilder.setMessage("$opponentUsername has accepted your offer for a draw")
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            onClick()
        }

        dialog = dialogBuilder.show()
    }

}