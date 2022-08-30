package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

class IncomingInviteDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Incoming invite!")
        dialog = dialogBuilder.create()
    }

    fun showInvite(invitingUser: String, inviteId: String, networkManager: NetworkManager) {
        dialogBuilder.setMessage("$invitingUser is challenging you for a match!")
        dialogBuilder.setPositiveButton("Accept") { _, _ ->
            networkManager.sendMessage(NetworkMessage(Topic.INFO, "accept_invite", inviteId))
        }
        dialogBuilder.setNegativeButton("Decline") { _, _ ->
            networkManager.sendMessage(NetworkMessage(Topic.INFO, "decline_invite", inviteId))
        }

        dialog = dialogBuilder.show()
    }

    fun dismiss() {
        if (this::dialog.isInitialized) {
            dialog.dismiss()
        }
    }

}