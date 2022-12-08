package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class CreateVariationDialog(private val onConfirmClicked: (String) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var variationNameInput: EditText
    private lateinit var createVariationButton: UIButton2

    fun create(activity: Activity) {
        dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_create_variation)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        createVariationButton = dialog.findViewById(R.id.create_variation_button)
        createVariationButton
            .setIcon(R.drawable.check_mark_icon)
            .setIconPadding(8, 8, 8, 8)
            .setColor(Color.GRAY)
            .setCornerRadius(45f)
            .setOnClickListener {
                val variationName = variationNameInput.text.toString().trim()
                if (variationName.isNotBlank()) {
                    onConfirmClicked(variationName)
                }
            }

        variationNameInput = dialog.findViewById(R.id.variation_name_input)
        variationNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(string: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editText: Editable?) {
                if (editText == null) {
                    return
                }

                if (editText.isEmpty()) {
                    createVariationButton.setColor(Color.GRAY)
                } else {
                    createVariationButton.setColor(235, 186, 145)
                }
            }
        })
    }

    fun show() {
        dialog.show()
        variationNameInput.requestFocus()
    }

    fun dismiss() {
        dialog.dismiss()
    }

}