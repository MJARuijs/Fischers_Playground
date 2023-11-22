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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class UIButton2(context: Context, attributes: AttributeSet? = null) : LinearLayout(context, attributes) {

    private var buttonCard: CardView
    private var buttonIcon: ImageView
    private var buttonText: TextView

    private var cardBackgroundColor = Color.TRANSPARENT
    private var iconColor = Color.WHITE

    private var buttonEnabled = true

    private var mirroredX = false
    private var mirroredY = false

    private var holding = AtomicBoolean(false)
//    private var shouldRepeatOnHold = false

    private var rippleEffect = RippleEffect.RECTANGLE

    init {
        LayoutInflater.from(context).inflate(R.layout.ui_button, this, true)

        buttonCard = findViewById(R.id.button_card)
        buttonIcon = findViewById(R.id.button_icon)
        buttonText = findViewById(R.id.button_text)

        buttonIcon.visibility = View.GONE
        buttonText.visibility = View.GONE
        buttonCard.foreground = ResourcesCompat.getDrawable(resources, R.drawable.ripple_rectangle, null)

        buttonText.setTextColor(Color.WHITE)
        textAlignment = View.TEXT_ALIGNMENT_CENTER

        buttonCard.setOnLongClickListener {
            if (!buttonEnabled) {
                return@setOnLongClickListener true
            }

//            if (shouldRepeatOnHold) {
//                holding.set(true)
//                handler.post(RepetitiveClicker())
//            }

            return@setOnLongClickListener true
        }
        buttonCard.setOnTouchListener { _, event ->
            if (!buttonEnabled) {
                holding.set(false)
                return@setOnTouchListener false
            }

            if (event.action == MotionEvent.ACTION_UP) {
                holding.set(false)
            }

            holding.get()
        }
    }

    fun setRippleEffect(rippleEffect: RippleEffect): UIButton2 {
        this.rippleEffect = rippleEffect
        if (rippleEffect == RippleEffect.OVAL) {
            buttonCard.foreground = ResourcesCompat.getDrawable(resources, R.drawable.ripple_oval, null)
        } else {
            buttonCard.foreground = ResourcesCompat.getDrawable(resources, R.drawable.ripple_rectangle, null)
        }
        return this
    }

    fun setTag(tag: String): UIButton2 {
        this.tag = tag
        return this
    }

    fun setRepeatOnHold(): UIButton2 {
        isLongClickable = true
//        shouldRepeatOnHold = true

        buttonCard.setOnLongClickListener {
            if (isLongClickable) {
                holding.set(true)
                handler.post(RepetitiveClicker())
            }

            true
        }
        return this
    }

    fun isHeld() = holding.get()

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        buttonCard.setOnClickListener {
            if (buttonEnabled) {
                l!!.onClick(it)
            }
        }
    }

    fun enable(): UIButton2 {
        buttonEnabled = true
        buttonIcon.setColorFilter(iconColor)
        buttonText.setTextColor(Color.WHITE)
        return this
    }

    fun disable(changeColors: Boolean = true): UIButton2 {
        buttonEnabled = false
        if (changeColors) {
            buttonIcon.setColorFilter(Color.GRAY)
            buttonText.setTextColor(Color.GRAY)
        }

        return this
    }

    fun isButtonEnabled() = buttonEnabled

    fun setIconPadding(left: Int, top: Int, right: Int, bottom: Int): UIButton2 {
        buttonIcon.setPadding(dpToPx(left), dpToPx(top), dpToPx(right), dpToPx(bottom))
        return this
    }

    fun hideIcon(): UIButton2 {
        buttonIcon.visibility = View.INVISIBLE
        return this
    }

    fun showIcon(): UIButton2 {
        buttonIcon.visibility = View.VISIBLE
        return this
    }

    fun setIcon(resourceId: Int, color: Int = Color.TRANSPARENT, mirroredX: Boolean = false, mirroredY: Boolean = false): UIButton2 {
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
        buttonCard.foreground = ResourcesCompat.getDrawable(resources, R.drawable.ripple_rectangle, null)
        setIconColor(color)

        return this
    }

    private fun setIconColor(color: Int): UIButton2 {
        buttonIcon.setColorFilter(color)
        iconColor = color
        return this
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        buttonCard.setOnLongClickListener(listener)
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

    fun setColor(r: Int, g: Int, b: Int): UIButton2 {
        val color = Color.rgb(r, g, b)
        return setColor(color)
    }

    fun setColorResource(resourceId: Int): UIButton2 {
        val color = resources.getColor(resourceId, null)
        return setColor(color)
    }

    fun setColor(color: Int): UIButton2 {
        cardBackgroundColor = color
        buttonCard.setCardBackgroundColor(color)
        return this
    }

    fun setCornerRadius(radius: Float): UIButton2 {
        buttonCard.radius = radius
        return this
    }

    fun hideText(): UIButton2 {
        buttonText.visibility = View.GONE
        return this
    }

    fun showText(): UIButton2 {
        buttonText.visibility = View.VISIBLE
        return this
    }

    override fun setTextAlignment(alignment: Int) {
        buttonText.textAlignment = alignment
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
            if (holding.get()) {
                Logger.debug(TAG, "RepetitiveClicker")
                callOnClick()
                if (handler != null) {
                    handler?.postDelayed(RepetitiveClicker(), Game.FAST_ANIMATION_SPEED.toLong())
                }
            }
        }
    }

    companion object {
        private const val TAG = "UIButton2"
        private const val MAX_CLICK_DELAY = 500L
        private const val onHoldColorChange = 0.2f
    }

}