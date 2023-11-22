package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.math.vectors.Vector2

class MoveFeedbackIcon(context: Context, attributeSet: AttributeSet? = null) : LinearLayout(context, attributeSet) {

    private val card: CardView
    private val iconView: ImageView

    private var scaleOffsetX = 0.0f
    private var scaleOffsetY = 0.0f

    init {
        LayoutInflater.from(context).inflate(R.layout.move_feedback_icon, this, true)

        card = findViewById(R.id.card)
        iconView = findViewById(R.id.iconView)
    }

    fun setPosition(position: Vector2) {
        x = position.x + scaleOffsetX
        y = position.y + scaleOffsetY
    }

    fun setColor(color: Int) {
        card.setCardBackgroundColor(color)
    }

    fun setIcon(resourceId: Int) {
        iconView.setImageDrawable(ResourcesCompat.getDrawable(resources, resourceId, null))
    }

    fun scaleToSize(size: Int) {
        val scale = size.toFloat() / width.toFloat()

        scaleOffsetX = (size.toFloat() - width.toFloat()) / 2.0f
        scaleOffsetY = (size.toFloat() - width.toFloat()) / 2.0f

        x += scaleOffsetX
        y += scaleOffsetY

        scaleX = scale
        scaleY = scale
    }

    fun hide() {
        visibility = View.INVISIBLE
    }

    fun show() {
        visibility = View.VISIBLE
    }
}