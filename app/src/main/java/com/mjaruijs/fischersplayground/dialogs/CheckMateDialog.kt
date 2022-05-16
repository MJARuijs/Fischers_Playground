package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic

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