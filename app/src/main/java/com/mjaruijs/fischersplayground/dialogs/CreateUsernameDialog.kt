package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.EditText
import androidx.cardview.widget.CardView
import com.mjaruijs.fischersplayground.R

class CreateUsernameDialog {

    private lateinit var dialog: Dialog

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.create_username_dialog)
        dialog.show()
        dialog.dismiss()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setLayout() {
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun show(setUsername: (String) -> Unit) {
        val usernameInputField = dialog.findViewById<EditText>(R.id.username_input_box)
        val commitButton = dialog.findViewById<CardView>(R.id.username_send_card)
        commitButton.setOnClickListener {
            val userName = usernameInputField.text.toString()
            setUsername(userName)
            dismiss()
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

}