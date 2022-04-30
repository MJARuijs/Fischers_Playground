package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.Button

class UIButton(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    private val textPaint = Paint()
    private val paint = Paint()

    var text = ""
    var textSize = 200.0f

    init {
        paint.isAntiAlias = true
        paint.color = Color.GRAY

        textPaint.isAntiAlias = true
        textPaint.color = Color.WHITE
        textPaint.textSize = 200.0f
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setTextColor(color: Int): UIButton {
        textPaint.color = color
        return this
    }

    fun setColor(color: Int): UIButton {
        paint.color = color
        return this
    }

    fun setText(text: String): UIButton {
        this.text = text
        invalidate()
        return this
    }

    fun setTextSize(size: Float): UIButton {
        textSize = size
        textPaint.textSize = size
        invalidate()
        return this
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        val xPos = width / 2.0f
        val yPos = (height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 10f, 10f, paint)
        canvas.drawText(text, xPos, yPos, textPaint)
    }

}