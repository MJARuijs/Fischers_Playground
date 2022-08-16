package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context

class CheckMateDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Checkmate!")
    }

    fun show(winnerName: String, onClick: () -> Unit) {
        dialogBuilder.setMessage("$winnerName has won!")
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            onClick()
        }

        dialog = dialogBuilder.show()
    }
}