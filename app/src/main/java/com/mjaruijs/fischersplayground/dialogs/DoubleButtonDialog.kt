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

class DoubleButtonDialog(parent: Activity, title: String, message: String, leftButtonIconLocation: Int?, leftButtonText: String?, private var onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int?, rightButtonText: String?, private var onRightButtonClick: () -> Unit) {

    constructor(parent: Activity, title: String, message: String, leftButtonIconLocation: Int, onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit) : this(parent, title, message, leftButtonIconLocation, null, onLeftButtonClick, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, title: String, message: String, leftButtonText: String, onLeftButtonClick: () -> Unit, rightButtonText: String, onRightButtonClick: () -> Unit) : this(parent, title, message, null, leftButtonText, onLeftButtonClick, null, rightButtonText, onRightButtonClick)

    constructor(parent: Activity, title: String, leftButtonIconLocation: Int, onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit) : this(parent, title, "", leftButtonIconLocation, null, onLeftButtonClick, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, title: String, leftButtonText: String, onLeftButtonClick: () -> Unit, rightButtonText: String, onRightButtonClick: () -> Unit) : this(parent, title, "", null, leftButtonText, onLeftButtonClick, null, rightButtonText, onRightButtonClick)

    constructor(parent: Activity, title: String, leftButtonIconLocation: Int, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit = {}) : this(parent, title, "", leftButtonIconLocation, null, {}, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, title: String, leftButtonText: String, rightButtonText: String, onRightButtonClick: () -> Unit = {}) : this(parent, title, "", null, leftButtonText, {}, null, rightButtonText, onRightButtonClick)

    constructor(parent: Activity, title: String, message: String, leftButtonIconLocation: Int, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit = {}) : this(parent, title, message, leftButtonIconLocation, null, {}, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, title: String, message: String, leftButtonText: String, rightButtonText: String, onRightButtonClick: () -> Unit = {}) : this(parent, title, message, null, leftButtonText, {}, null, rightButtonText, onRightButtonClick)

    private val dialog = Dialog(parent)
    private val messageView: TextView
    private val leftButton: UIButton
    private val rightButton: UIButton

    init {
        dialog.setContentView(R.layout.double_button_dialog)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
        leftButton = dialog.findViewById(R.id.left_dialog_button)
        rightButton = dialog.findViewById(R.id.right_dialog_button)
        messageView = dialog.findViewById(R.id.dialog_message)
        messageView.minWidth = (parent.resources.displayMetrics.widthPixels * 0.5f).roundToInt()
        messageView.text = message
        titleView.text = title
        leftButton.setIconScaleType(ScaleType.SQUARE)
            .setColor(57, 57, 57)
            .setCornerRadius(20.0f)
            .setOnClickListener {
                onLeftButtonClick()
                dismiss()
            }

        if (leftButtonIconLocation != null) {
            leftButton.setColoredDrawable(leftButtonIconLocation)
        }

        if (leftButtonText != null) {
            leftButton.setText(leftButtonText)
                .setButtonTextColor(Color.WHITE)
                .setButtonTextSize(50.0f)
        }

        rightButton.setIconScaleType(ScaleType.SQUARE)
            .setColor(235, 186, 145)
            .setCornerRadius(20.0f)
            .setOnClickListener {
                onRightButtonClick()
                dismiss()
            }

        if (rightButtonIconLocation != null) {
            rightButton.setColoredDrawable(rightButtonIconLocation)
        }

        if (rightButtonText != null) {
            rightButton.setText(rightButtonText)
                .setButtonTextColor(Color.WHITE)
                .setButtonTextSize(50.0f)
        }
    }

    fun setLeftOnClick(onClick: () -> Unit): DoubleButtonDialog {
        leftButton.setOnClickListener {
            onClick()
            dismiss()
        }
        return this
    }

    fun setRightOnClick(onClick: () -> Unit): DoubleButtonDialog {
        rightButton.setOnClickListener {
            onClick()
            dismiss()
        }
        return this
    }

    fun setMessage(message: String): DoubleButtonDialog {
        messageView.text = message
        return this
    }

    fun show(message: String){
        setMessage(message)
        show()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

}