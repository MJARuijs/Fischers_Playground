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

class CreateOpeningDialog(private val onTeamSelected: (Team) -> Unit) {

    private val accentColor = Color.rgb(235, 186, 145)
    private val grayColor = Color.GRAY

    private lateinit var dialog: Dialog

    private lateinit var whiteButton: UIButton
    private lateinit var blackButton: UIButton
    private lateinit var createOpeningButton: UIButton

    private lateinit var openingNameInput: EditText

    private var selectedTeam: Team? = null

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.create_opening_dialog)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        whiteButton = dialog.findViewById(R.id.opening_white_button)
        whiteButton
            .setTexturedDrawable(R.drawable.white_king)
            .setCornerRadius(20.0f)
            .setColor(grayColor)
            .setOnClick {
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
            .setTexturedDrawable(R.drawable.black_king)
            .setCornerRadius(20.0f)
            .setColor(grayColor)
            .setOnClick {
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
            .setTexturedDrawable(R.drawable.check_mark_icon)
            .setColor(Color.GRAY)
            .disable()
            .setIconScaleType(ScaleType.SQUARE)
            .setCornerRadius(45.0f)
            .setOnClick {
                if (selectedTeam != null) {
                    onTeamSelected(selectedTeam!!)
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
                    createOpeningButton.disable()
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

        if (this::whiteButton.isInitialized) {
            whiteButton.destroy()
        }

        if (this::blackButton.isInitialized) {
            blackButton.destroy()
        }
    }

}