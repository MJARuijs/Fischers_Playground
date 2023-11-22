package com.mjaruijs.fischersplayground.activities.settings

import android.content.res.Resources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlin.math.roundToInt

class SettingsCollection(private val settingsLayout: ConstraintLayout, private val toBottomOf: Int) {

    val settings = ArrayList<Setting>()

    operator fun plusAssign(setting: Setting) {
        val topAnchor = if (settings.isEmpty()) {
            toBottomOf
        } else {
            settings.last().cardView.id
        }

        val smallOffset = dpToPx(settingsLayout.context.resources, 8)

        val set = ConstraintSet()
        settingsLayout.addView(setting.cardView)
        set.clone(settingsLayout)
        set.connect(setting.getId(), ConstraintSet.TOP, topAnchor, ConstraintSet.BOTTOM, smallOffset)
        set.connect(setting.getId(), ConstraintSet.LEFT, settingsLayout.id, ConstraintSet.LEFT, smallOffset)
        set.connect(setting.getId(), ConstraintSet.RIGHT, settingsLayout.id, ConstraintSet.RIGHT, smallOffset)
        set.applyTo(settingsLayout)

        settings += setting
    }

    private fun dpToPx(resources: Resources, dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

}