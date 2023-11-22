package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.ScaleType
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class CreateOpeningDialog(private val onTeamSelected: (String, Team) -> Unit) {

    private val accentColor = Color.rgb(235, 186, 145)
    private val grayColor = Color.GRAY

    private lateinit var dialog: Dialog

    private lateinit var whiteButton: UIButton2
    private lateinit var blackButton: UIButton2
    private lateinit var createOpeningButton: UIButton2

    private lateinit var openingNameInput: EditText

    private var selectedTeam: Team? = null

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_create_opening)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        whiteButton = dialog.findViewById(R.id.opening_white_button)
        whiteButton
            .setIcon(R.drawable.white_king)
            .setCornerRadius(20.0f)
            .setIconPadding(8, 8, 8, 8)
            .setColor(grayColor)
            .setOnClickListener {
                selectedTeam = Team.WHITE
                whiteButton.setColor(accentColor)
                blackButton.setColor(grayColor)
                blackButton.invalidate()

                if (openingNameInput.text.isNotBlank()) {
                    createOpeningButton.enable()
                    createOpeningButton.setColor(accentColor)
                }
            }

        blackButton = dialog.findViewById(R.id.opening_black_button)
        blackButton
            .setIcon(R.drawable.black_king)
            .setCornerRadius(20.0f)
            .setIconPadding(8, 8, 8, 8)

            .setColor(grayColor)
            .setOnClickListener {
                selectedTeam = Team.BLACK

                blackButton.setColor(accentColor)
                whiteButton.setColor(grayColor)
                whiteButton.invalidate()

                if (openingNameInput.text.isNotBlank()) {
                    createOpeningButton.enable()
                    createOpeningButton.setColor(accentColor)
                }
            }

        createOpeningButton = dialog.findViewById(R.id.create_opening_button)
        createOpeningButton
            .setIcon(R.drawable.check_mark_icon)
            .setColor(Color.GRAY)
            .disable(false)
            .setCornerRadius(45.0f)
            .setOnClickListener {
                if (selectedTeam != null) {
                    onTeamSelected(openingNameInput.text.toString().trim(), selectedTeam!!)
                }
            }

        openingNameInput = dialog.findViewById(R.id.opening_name_input)
        openingNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(string: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editText: Editable?) {
                if (editText == null) {
                    return
                }

                if (editText.isEmpty()) {
                    createOpeningButton.disable(false)
                    createOpeningButton.setColor(grayColor)
                } else {
                    if (selectedTeam != null) {
                        createOpeningButton.enable()
                        createOpeningButton.setColor(accentColor)
                    }
                }
            }
        })
    }

    fun show() {
        dialog.show()
        openingNameInput.requestFocus()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun destroy() {
        dismiss()
    }

}