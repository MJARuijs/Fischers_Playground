package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.ScaleType
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PracticeOpeningActionButtonsFragment(requestRender: () -> Unit, private val onHintClicked: () -> Unit) : ActionButtonsFragment(R.layout.practice_opening_actionbar) {

    private lateinit var hintButton: UIButton

    override var numberOfButtons = 3

    init {
        this.requestRender = requestRender
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(1.0f, 0.95f, 0.35f, 0.35f)

        hintButton = view.findViewById(R.id.hint_button)
        hintButton
            .setText("Hint")
//            .setIconScaleType(ScaleType.SQUARE)

            .setColor(buttonBackgroundColor)
            .setColoredDrawable(R.drawable.solution_icon)
            .setButtonTextSize(50.0f)
            .setButtonTextColor(textColor)
            .setCenterVertically(false)
            .setChangeIconColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                onHintClicked()
            }

//        view.findViewById<UIButton>(R.id.place_holder_button).visibility = View.GONE
        backButton.visibility = View.GONE
//        forwardButton.visibility = View.GONE

        buttons += hintButton
    }

    override fun onDestroy() {
        super.onDestroy()
        hintButton.destroy()
    }

}