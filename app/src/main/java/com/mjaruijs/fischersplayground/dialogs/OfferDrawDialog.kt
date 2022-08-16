package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class OfferDrawDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setMessage("Are you sure you want to offer a draw?")
    }

    fun show(gameId: String, id: String) {
        dialogBuilder.setPositiveButton("Yes") { _, _ ->
            NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "offer_draw", "$gameId|$id"))
        }
        dialogBuilder.setNegativeButton("No") { _, _ ->

        }

        dialog = dialogBuilder.show()
    }

}