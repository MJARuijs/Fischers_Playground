package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context

class UndoRejectedDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Request rejected!")
    }

    fun show(opponentUsername: String) {
        dialogBuilder.setMessage("$opponentUsername has rejected your request to undo your move. Sucker")
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            dialog.dismiss()
        }

        dialog = dialogBuilder.show()
    }

}