package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PracticeActionButtonsFragment(onBackClicked: () -> Unit, onForwardClicked: () -> Unit, private val onVariationClicked: () -> Unit) : ActionButtonsFragment(R.layout.practice_actionbar, onBackClicked, onForwardClicked) {

    private lateinit var saveButton: UIButton
    private lateinit var addVariationButton: UIButton

    override var numberOfButtons = 4

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.0f, 0.25f, 0.25f, 0.25f)

        saveButton = view.findViewById(R.id.start_recording_button)
        addVariationButton = view.findViewById(R.id.stop_recording_button)

        saveButton
            .setText("Save")
            .setTexturedDrawable(R.drawable.save_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {

            }

        addVariationButton
            .setText("Add Variation")
            .setColoredDrawable(R.drawable.variation_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .disable()
            .setOnClick {
                onVariationClicked()
            }

//        buttons += saveButton
//        buttons += addVariationButton
    }

    override fun onDestroy() {
        super.onDestroy()
        saveButton.destroy()
        addVariationButton.destroy()
    }

    fun enableVariationsButton() {
        addVariationButton.enable()
    }
}