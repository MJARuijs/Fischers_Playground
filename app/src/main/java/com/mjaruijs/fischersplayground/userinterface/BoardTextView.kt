package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.children
import androidx.core.view.doOnLayout
import com.mjaruijs.fischersplayground.R

class BoardTextView(context: Context, attributes: AttributeSet?) : LinearLayout(context, attributes) {

    private val layout: ConstraintLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.board_text_layout, this, true)

        layout = findViewById(R.id.character_layout)

        layout.doOnLayout {
            setCharactersBold(true)
            setTextColor()
        }
    }

    private fun setCharactersBold(useBold: Boolean) {
        if (useBold) {
            for ((i, child) in layout.children.withIndex()) {
                if (child is TextView) {
                    child.typeface = Typeface.DEFAULT_BOLD
                }
            }
        } else {
            for (child in layout.children) {
                if (child is TextView) {
                    child.typeface = Typeface.DEFAULT
                }
            }
        }
    }

    private fun setTextColor() {
        for ((i, child) in layout.children.withIndex()) {
            if (child is TextView) {
                if (child.text.isDigitsOnly()) {
                    if (child.text.toString().toInt() % 2 == 1) {
                        child.setTextColor(whiteColor)
                    } else {
                        child.setTextColor(darkColor)
                    }
                } else {
                    if (i % 2 == 0) {
                        child.setTextColor(whiteColor)
                    } else {
                        child.setTextColor(darkColor)
                    }
                }
            }
        }
    }

    companion object {

        private val whiteColor = Color.rgb(207, 189, 175)
        private val darkColor = Color.rgb(91, 70, 53)

    }

}