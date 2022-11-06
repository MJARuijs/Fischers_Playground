package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton

class CreateOpeningActionButtonsFragment(requestRender: () -> Unit, private val onStartRecording: () -> Unit, private val onStopRecording: () -> Unit, private val onStartPracticing: () -> Unit) : ActionButtonsFragment(R.layout.create_opening_actionbar) {

    private lateinit var startRecordingButton: UIButton
    private lateinit var stopRecordingButton: UIButton
    private lateinit var startPracticeButton: UIButton

    override var numberOfButtons = 5

    init {
        this.requestRender = requestRender
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.0f, 1.25f, 0.25f, 0.25f)

        startRecordingButton = view.findViewById(R.id.start_recording_button)
        stopRecordingButton = view.findViewById(R.id.stop_recording_button)
        startPracticeButton = view.findViewById(R.id.start_practice_button)

        startRecordingButton
            .setText("Record")
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColoredDrawable(R.drawable.record_icon, Color.RED)
            .setColor(buttonBackgroundColor)
            .setCenterVertically(false)
            .setChangeIconColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                onStartRecording()
                startRecordingButton.disable()
                stopRecordingButton.enable()
            }

        stopRecordingButton
            .setText("Stop")
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColoredDrawable(R.drawable.stop_icon)
            .setColor(buttonBackgroundColor)
            .setCenterVertically(false)
            .setChangeIconColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .disable()
            .setOnClick {
                onStopRecording()
                startRecordingButton.enable()
                stopRecordingButton.disable()
            }

        startPracticeButton
            .setText("Practice")
            .setButtonTextSize(50.0f)
            .setColoredDrawable(R.drawable.student_icon, Color.WHITE)
            .setButtonTextColor(textColor)
            .setColor(235, 186, 145)
            .setCenterVertically(false)
            .setChangeIconColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                onStartPracticing()
            }

        buttons += startRecordingButton
        buttons += stopRecordingButton
        buttons += startPracticeButton
    }

    override fun onDestroy() {
        super.onDestroy()
        startRecordingButton.destroy()
        stopRecordingButton.destroy()
        startPracticeButton.destroy()
    }
}