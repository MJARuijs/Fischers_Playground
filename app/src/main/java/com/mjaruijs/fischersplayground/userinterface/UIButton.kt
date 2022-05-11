package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

class UIButton(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    private val textPaint = Paint()
    private val paint = Paint()

    private val debugPaint = Paint()

    private var drawable: Drawable? = null
    private var bitmap: Bitmap? = null
    private var drawableCanvas: Canvas? = null

    var disabled = false

    var buttonText = ""
    var buttonTextSize = 200.0f
    var buttonTextColor = 0

    var drawablePadding = 0
    var textXOffset = 0
    var textYOffset = 0

    var cornerRadius = 0.0f

    var rect = Rect()

    init {
        paint.isAntiAlias = true
        paint.color = Color.rgb(0.25f, 0.25f, 0.25f)

        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.textSize = 200.0f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true

        debugPaint.color = Color.GREEN
    }

    fun setCornerRadius(radius: Float): UIButton {
        cornerRadius = radius
        return this
    }

    fun enable(): UIButton {
        disabled = false
//        drawable!!.colorFilter = BlendModeColorFilter(Color.WHITE, BlendMode.SRC_ATOP)
        textPaint.color = Color.WHITE
        invalidate()
        return this
    }

    fun disable(): UIButton {
        disabled = true
//        drawable!!.colorFilter = BlendModeColorFilter(Color.GRAY, BlendMode.SRC_ATOP)
        textPaint.color = Color.GRAY
        invalidate()
        return this
    }

    fun setDrawable(uri: String): UIButton {
        val resourceId = resources.getIdentifier(uri, null, null)
        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
//        setImageDrawable(drawable)
        return this
    }

    fun setDrawable(resourceId: Int): UIButton {
//        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

//        drawable = resources.getDrawable(resourceId, null)
        drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
//        bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable!!.intrinsicHeight, Bitmap.Config.ARGB_8888)
        bitmap = drawable!!.toBitmap(drawable!!.intrinsicWidth, drawable!!.intrinsicHeight, Bitmap.Config.ALPHA_8)
//        drawableCanvas = Canvas(bitmap!!)
        println("Bitmap dimensions: $buttonText ${bitmap?.width} ${bitmap?.height}")

        drawable!!.setBounds(0, 0, bitmap!!.width, bitmap!!.height)
//        setImageDrawable(drawable)

        return this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (bitmap != null) {
            val scale = 0.6f

//            val halfViewWidth = (w / 2)
//            val halfDrawableWidth = (bitmap!!.width * scale / 2)
//
//            val left = ((halfViewWidth - halfDrawableWidth).roundToInt())
//            val top = 0
//            val right = ((bitmap!!.width * scale + halfViewWidth - halfDrawableWidth)).roundToInt()
//            val bottom = (bitmap!!.height * scale).roundToInt()

            val halfViewWidth = (w / 2)
            val halfDrawableWidth = (w * scale / 2)

            val halfViewHeight = (h / 2)
            val halfDrawableHeight = (h * scale / 2)

            val left = ((halfViewWidth - halfDrawableWidth).roundToInt())
//            val top = ((halfViewHeight - halfDrawableHeight).roundToInt())
            val top = 0
            val right = ((w * scale + halfViewWidth - halfDrawableWidth)).roundToInt()
            val bottom = (h * scale).roundToInt()
//            val bottom = ((h * scale + halfViewHeight - halfDrawableHeight)).roundToInt()

//            val left = 0
//            val top = 0
//            val right = w
//            val bottom = h

            rect = Rect(left, top, right, bottom)
//            rect = Rect(73, 0, 215, 143)
            println("$buttonText $left $top $right $bottom :: $w $h")
//            clipBounds = Rect(left, top, right, bottom)
//            drawable!!.setBounds(left, top, right, bottom)
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

        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, null, rect, textPaint)
        }

        canvas.drawText(buttonText, xPos + textXOffset, yPos + textYOffset, textPaint)
    }

}