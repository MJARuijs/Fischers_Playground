package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.ScaleType
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import kotlin.math.roundToInt

class DoubleButtonDialog(parent: Activity, cancelable: Boolean, title: String, message: String, leftButtonIconLocation: Int?, leftButtonText: String?, private var onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int?, rightButtonText: String?, private var onRightButtonClick: () -> Unit, minWidth: Float = 0.5f) {

    constructor(parent: Activity, cancelable: Boolean, title: String, message: String, leftButtonIconLocation: Int, onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit) : this(parent, cancelable, title, message, leftButtonIconLocation, null, onLeftButtonClick, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, message: String, leftButtonText: String, onLeftButtonClick: () -> Unit, rightButtonText: String, onRightButtonClick: () -> Unit) : this(parent, cancelable, title, message, null, leftButtonText, onLeftButtonClick, null, rightButtonText, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, leftButtonIconLocation: Int, onLeftButtonClick: () -> Unit, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit) : this(parent, cancelable, title, "", leftButtonIconLocation, null, onLeftButtonClick, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, leftButtonText: String, onLeftButtonClick: () -> Unit, rightButtonText: String, onRightButtonClick: () -> Unit, minWidth: Float) : this(parent, cancelable, title, "", null, leftButtonText, onLeftButtonClick, null, rightButtonText, onRightButtonClick, minWidth)

    constructor(parent: Activity, cancelable: Boolean, title: String, leftButtonIconLocation: Int, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit = {}) : this(parent, cancelable, title, "", leftButtonIconLocation, null, {}, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, leftButtonText: String, rightButtonText: String, onRightButtonClick: () -> Unit = {}) : this(parent, cancelable, title, "", null, leftButtonText, {}, null, rightButtonText, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, message: String, leftButtonIconLocation: Int, rightButtonIconLocation: Int, onRightButtonClick: () -> Unit = {}) : this(parent, cancelable, title, message, leftButtonIconLocation, null, {}, rightButtonIconLocation, null, onRightButtonClick)

    constructor(parent: Activity, cancelable: Boolean, title: String, message: String, leftButtonText: String, rightButtonText: String, onRightButtonClick: () -> Unit = {}) : this(parent, cancelable, title, message, null, leftButtonText, {}, null, rightButtonText, onRightButtonClick)

    private val dialog = Dialog(parent)
    private val messageView: TextView
    private val leftButton: UIButton2
    private val rightButton: UIButton2

    init {
        dialog.setContentView(R.layout.dialog_double_button)
        dialog.show()
        dialog.dismiss()
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
        leftButton = dialog.findViewById(R.id.left_dialog_button)
        rightButton = dialog.findViewById(R.id.right_dialog_button)
        messageView = dialog.findViewById(R.id.dialog_message)
        messageView.minWidth = (parent.resources.displayMetrics.widthPixels * minWidth).roundToInt()
        messageView.text = message
        titleView.text = title

        leftButton.setColor(57, 57, 57)
            .setCornerRadius(20.0f)
            .setOnClickListener {
                onLeftButtonClick()
                dismiss()
            }

        if (leftButtonIconLocation != null) {
            leftButton.setIcon(leftButtonIconLocation)
        }

        if (leftButtonText != null) {
            leftButton.setText(leftButtonText)
                .setTextSize(BUTTON_TEXT_SIZE)
        }

        rightButton.setColorResource(R.color.accent_color)
            .setCornerRadius(20.0f)
            .setOnClickListener {
                onRightButtonClick()
                dismiss()
            }

        if (rightButtonIconLocation != null) {
            rightButton.setIcon(rightButtonIconLocation)
        }

        if (rightButtonText != null) {
            rightButton.setText(rightButtonText)
                .setTextSize(BUTTON_TEXT_SIZE)
        }
    }

    fun setCancelable(cancelable: Boolean): DoubleButtonDialog {
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        return this
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

    companion object {
        private const val BUTTON_TEXT_SIZE = 24f
    }

}