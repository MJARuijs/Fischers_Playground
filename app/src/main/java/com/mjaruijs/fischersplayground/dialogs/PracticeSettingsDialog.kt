package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.CheckBox
import android.widget.TextView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import kotlin.math.roundToInt

class PracticeSettingsDialog(private val onStartPracticing: (Boolean, Boolean) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var titleView: TextView
    private lateinit var enableShuffleCheckBox: CheckBox
    private lateinit var practiceArrowsCheckBox: CheckBox
    private lateinit var startPracticingButton: UIButton2

    fun create(activity: Activity) {
        dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_practice_settings)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        enableShuffleCheckBox = dialog.findViewById(R.id.practice_dialog_shuffle_checkbox)
        practiceArrowsCheckBox = dialog.findViewById(R.id.practice_arrows_checkbox)

        titleView = dialog.findViewById(R.id.practice_dialog_title)
        titleView.minWidth = (activity.resources.displayMetrics.widthPixels * 0.75f).roundToInt()

        startPracticingButton = dialog.findViewById(R.id.start_practicing_button)
        startPracticingButton
            .setColorResource(R.color.accent_color)
            .setCornerRadius(45f)
            .setText("Start Practicing")
            .setTextSize(24f)
            .setOnClickListener {
                val useShuffle = enableShuffleCheckBox.isChecked
                val practiceArrows = practiceArrowsCheckBox.isChecked
                onStartPracticing(useShuffle, practiceArrows)
            }
    }

    fun show(context: Context) {
        val preferences = context.getSharedPreferences(SettingsActivity.PRACTICE_PREFERENCES_KEY, MODE_PRIVATE)
        val isBoxChecked = preferences.getBoolean(USE_SHUFFLE_KEY, false)
        enableShuffleCheckBox.isChecked = isBoxChecked
        enableShuffleCheckBox.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(USE_SHUFFLE_KEY, isChecked).apply()
        }

        val practiceArrows = preferences.getBoolean(PRACTICE_ARROWS_KEY, false)
        practiceArrowsCheckBox.isChecked = practiceArrows
        practiceArrowsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(PRACTICE_ARROWS_KEY, isChecked).apply()
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    companion object {

        const val USE_SHUFFLE_KEY = "use_shuffle"
        const val PRACTICE_ARROWS_KEY = "practice_arrows"

    }

}