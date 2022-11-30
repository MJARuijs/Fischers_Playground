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
import androidx.core.view.setPadding
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class UIButton2(context: Context, attributes: AttributeSet? = null) : LinearLayout(context, attributes) {

    private var buttonCard: CardView
    private var buttonIcon: ImageView
    private var buttonText: TextView

    private var cardBackgroundColor = Color.TRANSPARENT
    private var onHoldCardColor = Color.rgb(1.0f - onHoldColorChange, 1.0f - onHoldColorChange, 1.0f - onHoldColorChange)

    private var iconColor = Color.WHITE
    private var onHoldIconColor = Color.rgb(1.0f - onHoldColorChange, 1.0f - onHoldColorChange, 1.0f - onHoldColorChange)

    private var textColor = Color.WHITE
    private var onHoldTextColor = Color.rgb(1.0f - onHoldColorChange, 1.0f - onHoldColorChange, 1.0f - onHoldColorChange)

    private var buttonEnabled = true

    private var mirroredX = false
    private var mirroredY = false

    private var holding = false
    private var hasLongClickCallbacks = false

    private var hasIcon = false


    private var startClickTimer = -1L

    init {
        Logger.mute(TAG)

        LayoutInflater.from(context).inflate(R.layout.ui_button, this, true)

        buttonCard = findViewById(R.id.button_card)
        buttonIcon = findViewById(R.id.button_icon)
        buttonText = findViewById(R.id.button_text)

        textAlignment = View.TEXT_ALIGNMENT_CENTER
        buttonCard.setOnTouchListener { _, event ->
            Logger.debug(TAG, "BUTTON EVENT: ${event.action}")
            if (event.action == MotionEvent.ACTION_BUTTON_PRESS) {
                Logger.debug(TAG, "BUTTON ACTION PRESS")
                performClick()
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                Logger.debug(TAG, "BUTTON ACTION DOWN")
                startClickTimer = System.currentTimeMillis()
                if (hasIcon) {
                    buttonIcon.setColorFilter(onHoldIconColor)
                    buttonText.setTextColor(onHoldTextColor)
                } else {
                    buttonCard.setCardBackgroundColor(onHoldCardColor)
                }
            }

            if (event.action == MotionEvent.ACTION_UP) {
                Logger.debug(TAG, "BUTTON ACTION UP")
                holding = false

                val buttonReleasedTime = System.currentTimeMillis()
                if (buttonReleasedTime - startClickTimer < MAX_CLICK_DELAY) {
                    callOnClick()
                }
                if (hasIcon) {
                    buttonIcon.setColorFilter(iconColor)
                    buttonText.setTextColor(textColor)
                } else {
                    buttonCard.setCardBackgroundColor(cardBackgroundColor)
                }
            }

            !hasLongClickCallbacks
        }

    }

    fun setRepeatOnHold(): UIButton2 {
        isLongClickable = true
        hasLongClickCallbacks = true

        buttonCard.setOnLongClickListener {
            if (isLongClickable) {
                holding = true
                handler.post(RepetitiveClicker())
            }

            true
        }
        return this
    }

    fun setIcon(resourceId: Int, color: Int = Color.WHITE, mirroredX: Boolean = false, mirroredY: Boolean = false): UIButton2 {
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

        setIconColor(color)

        hasIcon = true

        return this
    }

    fun isHeld() = holding

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

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        super.setOnLongClickListener(listener)
        hasLongClickCallbacks = true
    }

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

    private fun setIconColor(color: Int): UIButton2 {
        buttonIcon.setColorFilter(color)
        iconColor = color

        val normalAlpha = Color.alpha(iconColor).toFloat() / 255f
        val normalRed = Color.red(iconColor).toFloat() / 255f
        val normalGreen = Color.green(iconColor).toFloat() / 255f
        val normalBlue = Color.blue(iconColor).toFloat() / 255f

        val total = normalRed + normalBlue + normalGreen

        val colorModifier = if (total < 1.5f) 1 else -1

        val onHoldRed = max(0.0f, min(normalRed + onHoldColorChange * colorModifier / normalAlpha, 1.0f))
        val onHoldGreen = max(0.0f, min(normalGreen + onHoldColorChange * colorModifier / normalAlpha, 1.0f))
        val onHoldBlue = max(0.0f, min(normalBlue + onHoldColorChange * colorModifier / normalAlpha, 1.0f))

        onHoldIconColor = Color.argb(1.0f, onHoldRed, onHoldGreen, onHoldBlue)

        return this
    }

    fun setColor(color: Int): UIButton2 {
        cardBackgroundColor = color

        val normalAlpha = Color.alpha(cardBackgroundColor).toFloat() / 255f
        val normalRed = Color.red(cardBackgroundColor).toFloat() / 255f
        val normalGreen = Color.green(cardBackgroundColor).toFloat() / 255f
        val normalBlue = Color.blue(cardBackgroundColor).toFloat() / 255f

        val total = normalRed + normalBlue + normalGreen

        val colorModifier = if (total < 1.5f) 1 else -1

        val onHoldRed = max(0.0f, min(normalRed + onHoldColorChange * colorModifier / normalAlpha, 1.0f))
        val onHoldGreen = max(0.0f, min(normalGreen + onHoldColorChange * colorModifier / normalAlpha, 1.0f))
        val onHoldBlue = max(0.0f, min(normalBlue + onHoldColorChange * colorModifier / normalAlpha, 1.0f))

        onHoldCardColor = Color.argb(normalAlpha, onHoldRed, onHoldGreen, onHoldBlue)
        buttonCard.setCardBackgroundColor(color)
        return this
    }

    fun setCornerRadius(radius: Float): UIButton2 {
        buttonCard.radius = radius
        return this
    }

    override fun setTextAlignment(alignment: Int) {
        buttonText.textAlignment = alignment
        buttonText.setPadding(dpToPx(8), 0, 0, 0)
    }

    fun setTextPadding(left: Int, top: Int, right: Int, bottom: Int) {
        buttonText.setPadding(dpToPx(left), dpToPx(top), dpToPx(right), dpToPx(bottom))
    }

    fun setText(text: String): UIButton2 {
        buttonText.text = text
        buttonText.visibility = View.VISIBLE
        return this
    }

    fun setTextSize(size: Float): UIButton2 {
        buttonText.textSize = size
        return this
    }

    fun setTextPadding(pixels: Int): UIButton2 {
        buttonText.setPadding(dpToPx(pixels))
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    inner class RepetitiveClicker : Runnable {
        override fun run() {
            if (holding) {
                callOnClick()
                handler.postDelayed(RepetitiveClicker(), Game.FAST_ANIMATION_SPEED)
            }
        }

    }

    companion object {
        private const val TAG = "UIButton2"
        private const val MAX_CLICK_DELAY = 500L
        private const val onHoldColorChange = 0.2f
    }

}