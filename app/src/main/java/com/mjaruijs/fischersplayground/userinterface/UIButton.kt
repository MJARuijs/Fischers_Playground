package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class UIButton(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    private var backgroundHoverColor = Color.argb(0.0f, 0.1f, 0.1f, 0.1f)
    private var textHoverColor = Color.argb(0.5f, 0.3f, 0.3f, 0.3f)

    private val textPaint = Paint()
    private val paint = Paint()

    private val debugPaint = Paint()

    private var bitmap: Bitmap? = null

    private var maxTextSize = 0f
    private var buttonTextSize = 200.0f
    private var buttonTextColor = 0

    private var drawablePadding = 0
    private var textXOffset = 0
    private var textYOffset = 0

    private var cornerRadius = 0.0f

    private var rect = Rect()

    private var centerVertically = true
    var disabled = false
    var buttonText = ""

    private var changeTextColorOnHover = true
    private var changeIconColorOnHover = true

    private var heldDown = false
    private var startClickTimer = -1L

    var onHold: () -> Unit = {}
    var onRelease: () -> Unit = {}
    var onButtonInitialized: (Float) -> Unit = {}

    private var holding = AtomicBoolean(false)

    init {
        paint.isAntiAlias = true
        paint.color = Color.rgb(0.25f, 0.25f, 0.25f)

        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.textSize = 200.0f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true

        debugPaint.color = Color.GREEN

        setOnTouchListener { _, event ->
            if (disabled) {
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_BUTTON_PRESS -> performClick()
                MotionEvent.ACTION_DOWN -> {
                    startClickTimer = System.currentTimeMillis()
                    heldDown = true
                    holding.set(true)

                    if (changeIconColorOnHover || changeTextColorOnHover) {
                        if (changeIconColorOnHover) {
                            paint.color = addColors(paint.color, backgroundHoverColor)
                        }
                        if (changeTextColorOnHover) {
                            textPaint.color = subtractColors(textPaint.color, textHoverColor)
                        }

                        invalidate()
                    }

                    Thread {
                        while (holding.get()) {
                            onHold()
                        }

                        onRelease()
                    }.start()
                }
                MotionEvent.ACTION_UP -> {
                    if (changeIconColorOnHover || changeTextColorOnHover) {
                        if (changeIconColorOnHover) {
                            paint.color = subtractColors(paint.color, backgroundHoverColor)
                        }
                        if (changeTextColorOnHover) {
                            textPaint.color = addColors(textPaint.color, textHoverColor)
                        }

                        invalidate()
                    }

                    val buttonReleasedTime = System.currentTimeMillis()
                    if (buttonReleasedTime - startClickTimer < 500) {
                        performClick()
                    }

                    heldDown = false
                    holding.set(false)
                }
            }

            return@setOnTouchListener true
        }
    }

    fun setOnHoldListener(onHold: () -> Unit): UIButton {
        this.onHold = onHold
        return this
    }

    fun setOnReleaseListener(onRelease: () -> Unit): UIButton {
        this.onRelease = onRelease
        return this
    }

    fun setChangeTextColorOnHover(setting: Boolean): UIButton {
        changeTextColorOnHover = setting
        return this
    }

    fun setChangeIconColorOnHover(setting: Boolean): UIButton {
        changeIconColorOnHover = setting
        return this
    }

    fun setCornerRadius(radius: Float): UIButton {
        cornerRadius = radius
        return this
    }

    fun enable(): UIButton {
        disabled = false
        textPaint.color = Color.WHITE
        invalidate()
        return this
    }

    fun disable(): UIButton {
        disabled = true
        textPaint.color = Color.GRAY
        invalidate()
        return this
    }

    fun setColoredDrawable(resourceId: Int): UIButton {
        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
        bitmap = drawable!!.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ALPHA_8)
        return this
    }

    fun setTexturedDrawable(resourceId: Int): UIButton {
        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
        bitmap = drawable!!.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        return this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (bitmap != null) {
            val scale = 0.5f

            val halfViewWidth = (w / 2)
            val halfDrawableWidth = (w * scale / 2)

            val left = ((halfViewWidth - halfDrawableWidth).roundToInt())
            val right = ((w * scale + halfViewWidth - halfDrawableWidth)).roundToInt()

            val drawableWidth = right - left

            val top: Int
            val bottom: Int

            if (centerVertically) {
                val halfViewHeight = (h / 2)
                val halfDrawableHeight = (h * scale / 2)

                top = ((halfViewHeight - halfDrawableHeight).roundToInt())
                bottom = ((h * scale + halfViewHeight - halfDrawableHeight)).roundToInt()
            } else {
                top = 0

                val maxHeight = (h * scale).roundToInt()
                bottom = max(maxHeight, drawableWidth)
            }

//            val bottom = (h * scale).roundToInt()

            rect = Rect(left, top, right, bottom)
        }

        val maxTextSize = calculateMaxTextSize()
        onButtonInitialized(maxTextSize)
    }

    fun setOnButtonInitialized(onButtonInitialized: (Float) -> Unit): UIButton {
        this.onButtonInitialized = onButtonInitialized
        return this
    }

    fun setCenterVertically(center: Boolean): UIButton {
        centerVertically = center
        return this
    }

    fun setDrawablePadding(padding: Int): UIButton {
        drawablePadding = padding
        return this
    }

    fun setButtonTextColor(color: Int): UIButton {
        textPaint.color = color
        buttonTextColor = color
        return this
    }

    fun setColor(r: Int, g: Int, b: Int): UIButton {
        paint.color = Color.argb(1.0f, r.toFloat() / 255f, g.toFloat() / 255f, b.toFloat() / 255f)
        return this
    }

    fun setColor(color: Int): UIButton {
        paint.color = color
        return this
    }

    fun setText(text: String): UIButton {
        this.buttonText = text
//        setTextSize(buttonText, width.toFloat())
        invalidate()
        return this
    }

    fun setButtonTextSize(size: Float): UIButton {
        buttonTextSize = size
        textPaint.textSize = size

        invalidate()
        return this
    }

    fun setTextXOffset(offset: Int): UIButton {
        textXOffset = offset
        return this
    }

    fun setTextYOffset(offset: Int): UIButton {
        textYOffset = offset
        return this
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        val xPos = width / 2.0f
        val yPos = (height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)

        if (cornerRadius == 0.0f) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, paint)
        }

        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, null, rect, textPaint)
        }

        if (bitmap == null) {
            canvas.drawText(buttonText, xPos + textXOffset, yPos + textYOffset, textPaint)
        } else {
            canvas.drawText(buttonText, xPos + textXOffset, rect.height() + abs(textPaint.ascent()), textPaint)
        }
    }

    private fun calculateMaxTextSize(): Float {
        val size = 35f
        val bounds = Rect()
        textPaint.getTextBounds(buttonText, 0, buttonText.length, bounds)
        maxTextSize = size * width.toFloat() / bounds.width()

//        println("Max text size for : $buttonText: $maxTextSize")

        return maxTextSize
    }

    private fun addColors(a: Int, b: Int): Int {
        val colorA = Color.valueOf(a)
        val colorB = Color.valueOf(b)

        var newR = colorA.red() + colorB.red()
        var newG = colorA.green() + colorB.green()
        var newB = colorA.blue() + colorB.blue()
        var newA = colorA.alpha() + colorB.alpha()

        if (newR > 1.0f) {
            newR = 1.0f
        }
        if (newG > 1.0f) {
            newG = 1.0f
        }
        if (newB > 1.0f) {
            newB = 1.0f
        }
        if (newA > 1.0f) {
            newA = 1.0f
        }

        return Color.argb(newA, newR, newG, newB)
    }

    private fun subtractColors(a: Int, b: Int): Int {
        val colorA = Color.valueOf(a)
        val colorB = Color.valueOf(b)

        var newR = colorA.red() - colorB.red()
        var newG = colorA.green() - colorB.green()
        var newB = colorA.blue() - colorB.blue()
        var newA = colorA.alpha() - colorB.alpha()

        if (newR < 0.0f) {
            newR = 0.0f
        }
        if (newG < 0.0f) {
            newG = 0.0f
        }
        if (newB < 0.0f) {
            newB = 0.0f
        }
        if (newA < 0.0f) {
            newA = 0.0f
        }

        return Color.argb(newA, newR, newG, newB)
    }
}