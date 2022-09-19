package com.mjaruijs.fischersplayground.activities.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import com.mjaruijs.fischersplayground.R
import kotlin.math.roundToInt

class Setting(context: Context, text: String, category: String, private val key: String, onToggle: (Boolean) -> Unit = {}) {

    private val preferences: SharedPreferences = context.getSharedPreferences(category, MODE_PRIVATE)
    val cardView = CardView(context)

    init {
        val enabled = preferences.getBoolean(key, false)

        cardView.id = View.generateViewId()
        cardView.setCardBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.background_color, null))
        cardView.radius = 20.0f * context.resources.displayMetrics.density
        cardView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val constraintLayout = ConstraintLayout(context)
        constraintLayout.id = View.generateViewId()

        val textView = TextView(context)
        textView.id = View.generateViewId()
        textView.text = text
        textView.textSize = 20f
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextColor(Color.rgb(235, 186, 145))

        val checkBox = CheckBox(context)
        checkBox.id = View.generateViewId()
        checkBox.buttonTintList = ColorStateList.valueOf(Color.rgb(235, 186, 145))
        checkBox.isChecked = enabled

        cardView.setOnClickListener {
            val isChecked = checkBox.isChecked
            savePreference(!isChecked)
            checkBox.isChecked = !isChecked
            onToggle(!isChecked)
        }

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            savePreference(isChecked)
            onToggle(isChecked)
        }

        constraintLayout.addView(textView)
        constraintLayout.addView(checkBox)

        val smallOffset = dpToPx(context.resources, 8)
        val bigOffset = dpToPx(context.resources, 16)

        val constraints = ConstraintSet()
        constraints.clone(constraintLayout)
        constraints.connect(textView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, smallOffset)
        constraints.connect(textView.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM, smallOffset)
        constraints.connect(textView.id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, bigOffset)

        constraints.connect(checkBox.id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, bigOffset)
        constraints.connect(checkBox.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, smallOffset)
        constraints.connect(checkBox.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM, smallOffset)
        constraints.applyTo(constraintLayout)

        cardView.addView(constraintLayout)

        onToggle(enabled)
    }

    fun getId() = cardView.id

    fun savePreference(value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    private fun dpToPx(resources: Resources, dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun dpToPx(resources: Resources, dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

}