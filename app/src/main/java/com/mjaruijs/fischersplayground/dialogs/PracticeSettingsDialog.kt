package com.mjaruijs.fischersplayground.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.mjaruijs.fischersplayground.R

class PracticeSettingsDialog {

    private lateinit var dialog: Dialog

    fun create(context: Context) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.practice_opening_dialog)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


    }

    fun dismiss() {
        dialog.dismiss()
    }


}