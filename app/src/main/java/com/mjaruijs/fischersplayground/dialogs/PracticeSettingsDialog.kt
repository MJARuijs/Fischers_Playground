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

class PracticeSettingsDialog(private val onStartPracticing: (Boolean) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var titleView: TextView
    private lateinit var enableShuffleCheckBox: CheckBox
    private lateinit var startPracticingButton: UIButton2

    fun create(activity: Activity) {
        dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_practice_settings)
        dialog.show()
        dialog.dismiss()
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        enableShuffleCheckBox = dialog.findViewById(R.id.practice_dialog_shuffle_checkbox)


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
                onStartPracticing(useShuffle)
            }
    }

    fun show(context: Context) {
        val preferences = context.getSharedPreferences(SettingsActivity.PRACTICE_PREFERENCES_KEY, MODE_PRIVATE)
        val isBoxChecked = preferences.getBoolean("use_shuffle", false)
        enableShuffleCheckBox.isChecked = isBoxChecked
        enableShuffleCheckBox.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean("use_shuffle", isChecked).apply()
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

}