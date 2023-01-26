package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.core.content.getSystemService
import com.mjaruijs.fischersplayground.R

class CreateAccountDialog {

    private lateinit var dialog: Dialog

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_create_account)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setLayout() {
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun show(setUsername: (String, String) -> Unit) {
        val emailInputField = dialog.findViewById<EditText>(R.id.email_input_box)
        val usernameInputField = dialog.findViewById<EditText>(R.id.username_input_box)
        val commitButton = dialog.findViewById<CardView>(R.id.login_button)
        commitButton.setOnClickListener {
            val email = emailInputField.text.toString().trim()
            val userName = usernameInputField.text.toString().trim()
            setUsername(email, userName)
            dismiss()
        }

        val emailCard = dialog.findViewById<CardView>(R.id.email_input_card)
        emailCard.setOnClickListener {
            emailInputField.requestFocus()
            showKeyboard(emailInputField)
        }

        val usernameCard = dialog.findViewById<CardView>(R.id.username_input_card)
        usernameCard.setOnClickListener {
            usernameInputField.requestFocus()
            showKeyboard(usernameInputField)
        }

        dialog.show()
    }

    private fun showKeyboard(inputBox: EditText) {
        val imm = dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(inputBox, 0)
    }

    fun dismiss() {
        dialog.dismiss()
    }

}