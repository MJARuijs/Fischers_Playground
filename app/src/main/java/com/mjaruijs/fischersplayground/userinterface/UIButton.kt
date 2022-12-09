package com.mjaruijs.fischersplayground.userinterface

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class UIButton(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    constructor(context: Context) : this(context, null)

    private var backgroundHoverColor = Color.argb(0.0f, 0.1f, 0.1f, 0.1f)
    private var textHoverColor = Color.argb(0.5f, 0.3f, 0.3f, 0.3f)

    private var drawablePaintColor = Color.WHITE
    private val drawablePaint = Paint()
    private val textPaint = Paint()
    private val paint = Paint()

    private val debugPaint = Paint()

    private var bitmap: Bitmap? = null
    private var bitmapBounds = Rect()

    private var maxTextSize = 0f
    private var buttonTextSize = 200.0f
    private var buttonTextColor = 0
    private var cornerRadius = 0.0f

    private var centerVertically = true
    private var textAlignment = TextAlignment.CENTER

    private var changeTextColorOnHover = true
    private var changeIconColorOnHover = true

    private var startClickTimer = -1L

    private var iconScaleType = ScaleType.SCALE_WITH_PARENT

    var disabled = false
    var buttonText = ""

    private var repeatOnHold = false
    private var delayBetweenHoldTriggers = 250L
    private val maxClickDelay = 250L

    private var onClick: (UIButton) -> Unit = {}
    private var onHold: () -> Unit = {}
    private var onRelease: () -> Unit = {}
    private var onButtonInitialized: (Float) -> Unit = {}

    private var runOnUiThread: (() -> Unit) -> Unit = {}

    private val buttonDown = AtomicBoolean(false)
    private val holding = AtomicBoolean(false)

//    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
//        vibratorManager.defaultVibrator
//    } else {
//        @Suppress("DEPRECATION")
//        context.getSystemService(Context.VIBRATOR_SERVICE)
//    }

    private var vibrateOnTrigger = false

    init {
        paint.isAntiAlias = true
        paint.color = Color.rgb(0.25f, 0.25f, 0.25f)

        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.textSize = 200.0f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true

        drawablePaint.isAntiAlias = true
        drawablePaint.color = Color.WHITE

        debugPaint.color = Color.GREEN

        setOnTouchListener { _, event ->
            if (disabled) {
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_BUTTON_PRESS -> {
                    onClick()
                }
                MotionEvent.ACTION_DOWN -> {
                    startClickTimer = System.currentTimeMillis()

                    if (changeIconColorOnHover || changeTextColorOnHover) {
                        addHoverColors()
                    }

                    buttonDown.set(true)
                    Thread {
                        if (repeatOnHold) {
                            var previousTriggerTime = startClickTimer

                            while (buttonDown.get()) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - startClickTimer >= maxClickDelay) {
                                    holding.set(true)
                                    break
                                }
                            }

                            while (holding.get()) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - previousTriggerTime >= delayBetweenHoldTriggers) {
                                    previousTriggerTime = currentTime
                                    runOnUiThread {
                                        onClick()
                                    }
                                }
                            }
                        }

                        onRelease()
                    }.start()
                }
                MotionEvent.ACTION_UP -> {
                    holding.set(false)
                    buttonDown.set(false)

                    if (changeIconColorOnHover || changeTextColorOnHover) {
                        removeHoverColors()
                    }

                    val buttonReleasedTime = System.currentTimeMillis()
                    if (buttonReleasedTime - startClickTimer < maxClickDelay) {
                        onClick()
                    }
                }
            }

            return@setOnTouchListener true
        }
    }

    private fun onClick() {
        val vibrateOnClick = context.getSharedPreferences(SettingsActivity.GAME_PREFERENCES_KEY, MODE_PRIVATE).getBoolean(SettingsActivity.VIBRATE_KEY, false)
        if (vibrateOnClick) {
            vibrate()
        }
        onClick(this)
    }

    private fun vibrate() {
//        (vibrator as Vibrator).vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun addHoverColors() {
        if (changeIconColorOnHover) {
            paint.color = addColors(paint.color, backgroundHoverColor)
        }
        if (changeTextColorOnHover) {
            textPaint.color = subtractColors(textPaint.color, textHoverColor)
        }

        invalidate()
    }

    private fun removeHoverColors() {
        if (changeIconColorOnHover) {
            paint.color = subtractColors(paint.color, backgroundHoverColor)
        }
        if (changeTextColorOnHover) {
            textPaint.color = addColors(textPaint.color, textHoverColor)
        }

        invalidate()
    }

    fun setOnClick(onClick: (UIButton) -> Unit): UIButton {
        this.onClick = onClick
        return this
    }

    fun setRepeatOnHold(delay: Long, runOnUiThread: (() -> Unit) -> Unit): UIButton {
        repeatOnHold = true
        delayBetweenHoldTriggers = delay
        this.runOnUiThread = runOnUiThread
        return this
    }

    fun isHeld() = holding.get()

    fun isDown() = buttonDown.get()

    fun setOnHoldListener(onHold: () -> Unit): UIButton {
        this.onHold = onHold
        return this
    }

    fun setOnReleaseListener(onRelease: () -> Unit): UIButton {
        this.onRelease = onRelease
        return this
    }

    fun setIconScaleType(scaleType: ScaleType): UIButton {
        this.iconScaleType = scaleType
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

    fun setVibrateOnTrigger(vibrate: Boolean): UIButton {
        vibrateOnTrigger = vibrate
        return this
    }

    fun enable(): UIButton {
        disabled = false
        textPaint.color = Color.WHITE
        drawablePaint.color = drawablePaintColor
        invalidate()
        return this
    }

    fun disable(): UIButton {
        holding.set(false)
        buttonDown.set(false)
        disabled = true
        textPaint.color = Color.GRAY
        drawablePaint.color = Color.GRAY
        invalidate()
        return this
    }

    fun setColoredDrawable(resourceId: Int): UIButton {
        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
        bitmap = drawable!!.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ALPHA_8)
        textAlignment = TextAlignment.BOTTOM
        return this
    }

    fun setColoredDrawable(resourceId: Int, color: Int): UIButton {
        drawablePaint.color = color
        return setColoredDrawable(resourceId)
    }

    fun setTexturedDrawable(resourceId: Int): UIButton {
        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
        bitmap = drawable!!.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        textAlignment = TextAlignment.BOTTOM
        return this
    }

    fun setOnButtonInitialized(onButtonInitialized: (Float) -> Unit): UIButton {
        this.onButtonInitialized = onButtonInitialized
        return this
    }

    fun setCenterVertically(center: Boolean): UIButton {
        centerVertically = center
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
        invalidate()
        return this
    }

    fun setButtonTextSize(size: Float): UIButton {
        buttonTextSize = size
        textPaint.textSize = size
        invalidate()

        calculateBitmapBounds()
        return this
    }

    fun setFinalTextSize(size: Float): UIButton {
        buttonTextSize = size
        textPaint.textSize = size
        invalidate()

        calculateBitmapBounds()
        return this
    }

    private fun calculateBitmapBounds() {
        val bottom = measuredHeight - abs(textPaint.descent()) - abs(textPaint.ascent())
        val maxDimension = min(measuredWidth.toFloat(), bottom)

        val left: Float
        val right: Float

        if (measuredWidth.toFloat() > maxDimension) {
            val difference = measuredWidth.toFloat() - maxDimension
            left = difference / 2f
            right = measuredWidth.toFloat() - difference / 2f
        } else {
            left = 0f
            right = measuredWidth.toFloat()
        }

        bitmapBounds = Rect(left.roundToInt(), 0, right.roundToInt(), bottom.roundToInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null) {
            if (iconScaleType == ScaleType.SCALE_WITH_PARENT) {
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

                bitmapBounds = Rect(left, top, right, bottom)
            } else if (iconScaleType == ScaleType.SQUARE) {
                if (h < w) {
                    val scaledHeight = (h * 0.3f).roundToInt()
                    val halfHeight = h / 2
                    val top = halfHeight - scaledHeight
                    val bottom = halfHeight + scaledHeight

                    val halfWidth = w / 2

                    val left = halfWidth - scaledHeight
                    val right = halfWidth + scaledHeight
                    val scale = 1f
                    bitmapBounds = Rect((left * scale).roundToInt(), (top * scale).roundToInt(), (right * scale).roundToInt(), (bottom * scale).roundToInt())
                }
            }

        }

        val maxTextSize = calculateMaxTextSize()
        onButtonInitialized(maxTextSize)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        val xPos = measuredWidth / 2.0f

        if (cornerRadius == 0.0f) {
            canvas.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), paint)
        } else {
            canvas.drawRoundRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), cornerRadius, cornerRadius, paint)
        }

        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, null, bitmapBounds, drawablePaint)
        }

        if (textAlignment == TextAlignment.CENTER) {
            canvas.drawText(buttonText, xPos, measuredHeight.toFloat() / 2f - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        } else if (textAlignment == TextAlignment.BOTTOM) {
            canvas.drawText(buttonText, xPos, measuredHeight.toFloat() - textPaint.descent(), textPaint)
        }
    }

    private fun calculateMaxTextSize(): Float {
        val size = 35f
        val bounds = Rect()
        textPaint.getTextBounds(buttonText, 0, buttonText.length, bounds)
        maxTextSize = size * measuredWidth.toFloat() / bounds.width()

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

    fun destroy() {
        bitmap?.recycle()
    }
}