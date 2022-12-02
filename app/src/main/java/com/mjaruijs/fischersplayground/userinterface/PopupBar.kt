package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.*
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.mjaruijs.fischersplayground.R
import kotlin.math.roundToInt

class PopupBar(context: Context, attributes: AttributeSet?) : LinearLayout(context, attributes) {

    private val button: UIButton2
    private val collapsedConstraintSet = ConstraintSet()
    private val expandedConstraintSet = ConstraintSet()

    private val expandedHeight = dpToPx(HEIGHT)
    private lateinit var parentLayout: ConstraintLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.popup_bar, this, true)

        button = findViewById(R.id.popup_button)
        button.setColorResource(R.color.accent_color)
            .setTextSize(24f)

    }

    fun attachToLayout(layout: ConstraintLayout): PopupBar {
        parentLayout = layout
        collapsedConstraintSet.clone(layout)
        collapsedConstraintSet.connect(id, BOTTOM, PARENT_ID, BOTTOM)
        collapsedConstraintSet.connect(id, LEFT, PARENT_ID, LEFT)
        collapsedConstraintSet.connect(id, RIGHT, PARENT_ID, RIGHT)
        collapsedConstraintSet.constrainHeight(id, -3)
        collapsedConstraintSet.applyTo(layout)

        expandedConstraintSet.clone(layout)
        expandedConstraintSet.constrainHeight(id, expandedHeight)

        return this
    }

    override fun setOnClickListener(l: OnClickListener?) {
        button.setOnClickListener(l)
    }

    fun setText(text: String): PopupBar {
        button.setText(text)
        return this
    }

    fun show() {
        expandedConstraintSet.applyTo(parentLayout)
        val transition = ChangeBounds()
        transition.duration = 250L
        transition.interpolator = LinearInterpolator()
        TransitionManager.beginDelayedTransition(parentLayout, transition)
    }

    fun hide() {
        collapsedConstraintSet.applyTo(parentLayout)
        val transition = ChangeBounds()
        transition.duration = 250L
        transition.interpolator = LinearInterpolator()
        TransitionManager.beginDelayedTransition(parentLayout, transition)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    companion object {
        private const val TAG = "PopupBar"
        private const val HEIGHT = 60
    }
}