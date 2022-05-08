package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import kotlin.math.roundToInt

class UIButton(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    private val textPaint = Paint()
    private val paint = Paint()

    private val debugPaint = Paint()

    private var drawable: Drawable? = null
    private var bitmap: Bitmap? = null
    private var drawableCanvas: Canvas? = null

    var buttonText = ""
    var buttonTextSize = 200.0f
    var buttonTextColor = 0

    var drawablePadding = 0
    var textXOffset = 0
    var textYOffset = 0

    var cornerRadius = 0.0f

    init {
        paint.isAntiAlias = true
//        paint.color = Color.BLUE
        paint.color = Color.rgb(0.25f, 0.25f, 0.25f)
        debugPaint.color = Color.GREEN

        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.textSize = 200.0f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
    }

    fun setCornerRadius(radius: Float): UIButton {
        cornerRadius = radius
        return this
    }

    fun isEnabled(enabled: Boolean): UIButton {
        isEnabled = enabled
        return this
    }

    fun enable() {
        println("Enabled")
//        isEnabled = true
    }

    fun disable() {
//        isEnabled = false
        println("Disabled")
        drawable!!.colorFilter = BlendModeColorFilter(Color.GRAY, BlendMode.COLOR)
    }

    fun setDrawable(resourceId: Int): UIButton {
        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        drawable = ResourcesCompat.getDrawable(resources, resourceId, null)

        bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable!!.intrinsicHeight, Bitmap.Config.ARGB_8888)
//        println("INTRINSIC: ${drawable!!.intrinsicWidth}, ${drawable!!.intrinsicHeight}")
        drawableCanvas = Canvas(bitmap!!)
//        println("$text: $width, $height ${drawableCanvas!!.width} ${drawableCanvas!!.height}")
        drawable!!.setBounds(0, 0, drawableCanvas!!.width, drawableCanvas!!.height)

        return this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        println("$text: ONSIZE CHANGED: $w, $h")

        if (drawableCanvas != null) {
            val scale = 0.85f

            val halfViewWidth = (w / 2)
            val halfDrawableWidth = (drawableCanvas!!.width * scale / 2)

            val left = ((halfViewWidth - halfDrawableWidth).roundToInt())
            val top = 0
            val right = ((drawableCanvas!!.width * scale + halfViewWidth - halfDrawableWidth)).roundToInt()
            val bottom = (drawableCanvas!!.height * scale).roundToInt()

            drawable!!.setBounds(left, top, right, bottom)
        }
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
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(),  paint)
        } else {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, paint)
        }

        if (drawableCanvas != null) {
//            canvas.drawRect(drawable!!.bounds, debugPaint)
            drawable!!.draw(canvas)
        }

        canvas.drawText(buttonText, xPos + textXOffset, yPos + textYOffset, textPaint)

    }

}