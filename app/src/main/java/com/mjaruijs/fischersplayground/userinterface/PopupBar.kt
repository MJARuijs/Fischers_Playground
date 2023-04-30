package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.*
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

class PopupBar(context: Context, attributes: AttributeSet?) : LinearLayout(context, attributes) {

    private val collapsedConstraintSet = ConstraintSet()
    private val expandedConstraintSet = ConstraintSet()

    private val expandedHeight = dpToPx(HEIGHT)
    private val popupLayout: LinearLayout
    private lateinit var parentLayout: ConstraintLayout

    private var rightMostViewId = 0

    var isShowing = false

    init {
        LayoutInflater.from(context).inflate(R.layout.popup_bar, this, true)

        popupLayout = findViewById(R.id.popup_layout)
        rightMostViewId = popupLayout.id
    }

    fun attachToLayout(layout: ConstraintLayout): PopupBar {
        parentLayout = layout

        collapsedConstraintSet.connect(id, BOTTOM, PARENT_ID, BOTTOM)
        collapsedConstraintSet.connect(id, LEFT, PARENT_ID, LEFT)
        collapsedConstraintSet.connect(id, RIGHT, PARENT_ID, RIGHT)
        collapsedConstraintSet.constrainHeight(id, -3)
        collapsedConstraintSet.applyTo(layout)

        expandedConstraintSet.connect(id, BOTTOM, PARENT_ID, BOTTOM)
        expandedConstraintSet.connect(id, LEFT, PARENT_ID, LEFT)
        expandedConstraintSet.connect(id, RIGHT, PARENT_ID, RIGHT)
        expandedConstraintSet.constrainHeight(id, expandedHeight)

        return this
    }

    var i = 0

    fun addButton(view: View) {
        popupLayout.addView(view, BUTTON_LAYOUT)
    }

    fun show() {
        Logger.debug(TAG, "Showing")
        expandedConstraintSet.applyTo(parentLayout)
        val transition = ChangeBounds()
        transition.duration = 250L
        transition.interpolator = LinearInterpolator()
        TransitionManager.beginDelayedTransition(parentLayout, transition)
        isShowing = true
    }

    fun hide() {
        collapsedConstraintSet.applyTo(parentLayout)
        val transition = ChangeBounds()
        transition.duration = 250L
        transition.interpolator = LinearInterpolator()
        TransitionManager.beginDelayedTransition(parentLayout, transition)
        isShowing = false
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    companion object {
        private const val TAG = "PopupBar"
        private const val HEIGHT = 60

        val BUTTON_LAYOUT = LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

    }
}