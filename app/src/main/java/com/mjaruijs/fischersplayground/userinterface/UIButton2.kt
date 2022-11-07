package com.mjaruijs.fischersplayground.userinterface

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.mjaruijs.fischersplayground.R

class UIButton2(context: Context, attributes: AttributeSet? = null): LinearLayout(context, attributes) {

    private var buttonCard: CardView
    private var buttonIcon: ImageView
    private var buttonText: TextView

    private var iconColor = Color.WHITE
    private var onHoldColor = Color.rgb(1.0f - onHoldColorChange, 1.0f - onHoldColorChange, 1.0f - onHoldColorChange)

    private var textColor = Color.WHITE

    private var buttonEnabled = true

    private var mirroredX = false
    private var mirroredY = false

    init {
        LayoutInflater.from(context).inflate(R.layout.ui_button, this, true)

        buttonCard = findViewById(R.id.button_card)
        buttonIcon = findViewById(R.id.button_icon)
        buttonText = findViewById(R.id.button_text)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setIcon(resourceId: Int, mirroredX: Boolean = false, mirroredY: Boolean = false): UIButton2 {
        this.mirroredX = mirroredX
        this.mirroredY = mirroredY

        if (mirroredX) {
            buttonIcon.scaleX *= -1
        }

        if (mirroredY) {
            buttonIcon.scaleY *= -1
        }

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Could not find resource with id: $resourceId")
        buttonIcon.visibility = View.VISIBLE
        buttonIcon.setImageDrawable(drawable)
        buttonIcon.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
//                buttonIcon.setColorFilter(iconColor)
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
//                buttonIcon.setColorFilter(onHoldColor)
            }
            false
        }

        return this
    }

    fun enable(): UIButton2 {
        buttonEnabled = true
        buttonIcon.setColorFilter(iconColor)
        buttonText.setTextColor(textColor)
        return this
    }

    fun disable(): UIButton2 {
        buttonEnabled = false
        buttonIcon.setColorFilter(Color.GRAY)
        buttonText.setTextColor(Color.GRAY)
        return this
    }

    fun isButtonEnabled() = buttonEnabled

    fun setIconScale(scale: Float): UIButton2 {
        buttonIcon.scaleX = scale
        buttonIcon.scaleY = scale

        if (mirroredX) {
            buttonIcon.scaleX *= -1
        }

        if (mirroredY) {
            buttonIcon.scaleY *= -1
        }
        return this
    }

    fun setIconColor(color: Int): UIButton2 {
        buttonIcon.setColorFilter(color)
        iconColor = color

        val onHoldRed = Color.red(iconColor).toFloat() / 255f - onHoldColorChange
        val onHoldGreen = Color.green(iconColor).toFloat() / 255f - onHoldColorChange
        val onHoldBlue = Color.blue(iconColor).toFloat() / 255f - onHoldColorChange

        onHoldColor = Color.rgb(onHoldRed, onHoldGreen, onHoldBlue)
        return this
    }

    fun setColor(color: Int): UIButton2 {
        buttonCard.setCardBackgroundColor(color)
        return this
    }

    fun setCornerRadius(radius: Float): UIButton2 {
        buttonCard.radius = radius
        return this
    }

    fun setText(text: String): UIButton2 {
        buttonText.text = text
        buttonText.visibility = View.VISIBLE
        return this
    }

    fun setTextColor(color: Int): UIButton2 {
        textColor = color
        buttonText.setTextColor(color)
        return this
    }

    fun setTextSize(size: Float): UIButton2 {
        buttonText.textSize = size
        return this
    }

    fun show() {
        visibility = View.VISIBLE
        buttonCard.visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
        buttonCard.visibility = View.GONE
    }

    companion object {
        private const val onHoldColorChange = 0.2f
    }

}