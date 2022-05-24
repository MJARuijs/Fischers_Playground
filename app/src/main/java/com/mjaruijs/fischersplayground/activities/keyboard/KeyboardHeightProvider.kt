package com.mjaruijs.fischersplayground.activities.keyboard

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.PopupWindow
import com.mjaruijs.fischersplayground.R

class KeyboardHeightProvider(private val activity: Activity) : PopupWindow(activity) {

    private var keyboardHeight = 0

    private var popupView = activity.layoutInflater.inflate(R.layout.popup_window, null, false)

    var parentView: View
    var observer: KeyboardHeightObserver? = null

    init {
        contentView = popupView

        inputMethodMode = INPUT_METHOD_NEEDED

        parentView = activity.findViewById(android.R.id.content)

        width = 0
        height = LayoutParams.MATCH_PARENT

        popupView.viewTreeObserver.addOnGlobalLayoutListener {
            handleOnGlobalLayout()
        }
    }

    fun start() {
        if (!isShowing && parentView.windowToken != null) {
            setBackgroundDrawable(ColorDrawable(0))
            showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
        }
    }

    fun close() {
        observer = null
        dismiss()
    }

    private fun handleOnGlobalLayout() {
        val screenSize = Point()
        activity.windowManager.defaultDisplay.getSize(screenSize)
//        activity.windowManager.currentWindowMetrics.bounds

        val rect = Rect()
        popupView.getWindowVisibleDisplayFrame(rect)

        val keyboardHeight = screenSize.y - rect.bottom

        if (this.keyboardHeight != keyboardHeight) {
            this.keyboardHeight = keyboardHeight
            notifyKeyboardHeightChanged(keyboardHeight)
        }
    }

    private fun notifyKeyboardHeightChanged(height: Int) {
        if (observer != null) {
            observer!!.onKeyboardHeightChanged(height)
        }
    }

}