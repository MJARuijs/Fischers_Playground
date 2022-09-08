package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context

class OpponentDeclinedDrawDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Draw declined!")
    }

    fun show(opponentUsername: String) {
        println("SHOWING DRAW DIALOG")
        dialogBuilder.setMessage("$opponentUsername has declined your offer for a draw")
        dialogBuilder.setPositiveButton("Ok") { _, _ ->

        }

        dialog = dialogBuilder.show()
    }
}