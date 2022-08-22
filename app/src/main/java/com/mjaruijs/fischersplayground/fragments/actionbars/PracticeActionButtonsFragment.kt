package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PracticeActionButtonsFragment(requestRender: () -> Unit) : ActionButtonsFragment(R.layout.practice_actionbar, requestRender) {

    private lateinit var startRecordingButton: UIButton
    private lateinit var stopRecordingButton: UIButton

    override var numberOfButtons = 4

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textOffset = 0
        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)

        startRecordingButton = view.findViewById(R.id.start_recording_button)
        startRecordingButton
            .setText("Record")
            .setColoredDrawable(R.drawable.record_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setTextYOffset(textOffset)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {

            }

        stopRecordingButton = view.findViewById(R.id.stop_recording_button)
        stopRecordingButton
            .setText("Stop")
            .setColoredDrawable(R.drawable.stop_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setTextYOffset(textOffset)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {

            }

        buttons += startRecordingButton
        buttons += stopRecordingButton
    }

}