package com.mjaruijs.fischersplayground.dialogs

import android.app.AlertDialog
import android.content.Context

class ResignDialog {

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBuilder: AlertDialog.Builder

    fun create(context: Context) {
        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setMessage("Are you sure you want to resign?")

//        dialog = dialogBuilder.create()
    }

    fun show(onResign: () -> Unit) {
        dialogBuilder.setPositiveButton("Yes") { _, _ ->
            onResign()
        }
        dialogBuilder.setNegativeButton("No") { _, _ -> }

        dialog = dialogBuilder.show()
    }

}