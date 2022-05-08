package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Looper

class OpponentResignedDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("You won!")

//        dialog = dialogBuilder.create()
    }

    fun show(opponentUsername: String, closeGame: () -> Unit) {
        dialogBuilder.setMessage("$opponentUsername has resigned!")
        dialogBuilder.setPositiveButton("Ok") { _, _ ->
            closeGame()
        }

        dialog = dialogBuilder.show()
    }
}