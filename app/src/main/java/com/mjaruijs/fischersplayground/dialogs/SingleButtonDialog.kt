package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.ScaleType
import com.mjaruijs.fischersplayground.userinterface.UIButton
import kotlin.math.roundToInt

class SingleButtonDialog(parent: Activity, title: String, message: String, buttonIconLocation: Int?, buttonText: String?, private var onButtonClick: () -> Unit = {}) {

    constructor(parent: Activity, title: String, message: String, buttonLocation: Int, onClick: () -> Unit = {}) : this(parent, title, message, buttonLocation, null, onClick)

    constructor(parent: Activity, title: String, message: String, buttonText: String?, onClick: () -> Unit = {}) : this(parent, title, message, null, buttonText, onClick)

    constructor(parent: Activity, title: String, buttonLocation: Int, onClick: () -> Unit = {}) : this(parent, title, "", buttonLocation, null, onClick)

    constructor(parent: Activity, title: String, buttonText: String?, onClick: () -> Unit = {}) : this(parent, title, "", null, buttonText, onClick)

    private val dialog = Dialog(parent)
    private val messageView: TextView
    private val button: UIButton

    init {
        dialog.setContentView(R.layout.dialog_single_button)
        dialog.show()
        dialog.dismiss()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
        button = dialog.findViewById(R.id.left_dialog_button)
        messageView = dialog.findViewById(R.id.dialog_message)
        messageView.minWidth = (parent.resources.displayMetrics.widthPixels * 0.5f).roundToInt()
        messageView.text = message
        titleView.text = title
        button.setIconScaleType(ScaleType.SQUARE)
            .setColor(235, 186, 145)
            .setCornerRadius(20.0f)
            .setOnClick {
                onButtonClick()
                dismiss()
            }

        if (buttonIconLocation != null) {
            button.setColoredDrawable(buttonIconLocation)
        }

        if (buttonText != null) {
            button.setText(buttonText)
                .setButtonTextColor(Color.WHITE)
                .setButtonTextSize(50.0f)
        }
    }

    fun setOnClick(onClick: () -> Unit): SingleButtonDialog {
        button.setOnClick {
            onClick()
            dismiss()
        }
        return this
    }

    fun setMessage(message: String): SingleButtonDialog {
        messageView.text = message
        return this
    }

    fun show(message: String) {
        setMessage(message)
        show()
    }

    fun show(onClick: () -> Unit) {
        setOnClick(onClick)
        show()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun destroy() {
        dismiss()
        button.destroy()
    }
}