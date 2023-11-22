package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

open class ActionButtonsFragment(private val layoutResource: Int, private val onBackClicked: () -> Unit = {}, private val onForwardClicked: () -> Unit = {}) : Fragment() {

    private var maxTextSize = Float.MAX_VALUE
    private var numberOfButtonsInitialized = 0

    protected lateinit var backButton: UIButton2
    protected lateinit var forwardButton: UIButton2

    lateinit var game: Game

    open var numberOfButtons: Int = 2

    val buttons = ArrayList<UIButton2>()

    fun enableBackButton() {
        backButton.enable()
    }

    fun enableForwardButton() {
        forwardButton.enable()
    }

    fun enableButtons() {
        backButton.enable()
        forwardButton.enable()
    }

    fun disableButtons() {
        backButton.disable()
        forwardButton.disable()
    }

    fun disableForwardButton() {
        forwardButton.disable()
    }

    fun disableBackButton() {
        backButton.disable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)

        backButton = view.findViewById(R.id.back_button)
        backButton
            .setText("Back")
            .setIcon(R.drawable.arrow_back)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)

            .disable()
            .setOnClickListener {
                onBackClicked(backButton)
            }
//        backButton
//            .setText("Back")
//            .setColoredDrawable(R.drawable.arrow_back)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .disable()
//            .setRepeatOnHold(100L, ::runOnUiThread)
//            .setOnClick {
//                onBackClicked(it)
//            }

        forwardButton = view.findViewById(R.id.forward_button)
        forwardButton
            .setText("Forward")
            .setIcon(R.drawable.arrow_forward)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)

            .disable()
            .setOnClickListener {
                onForwardClicked(forwardButton)
            }
//        forwardButton
//            .setText("Forward")
//            .setColoredDrawable(R.drawable.arrow_forward)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .disable()
//            .setRepeatOnHold(100L, ::runOnUiThread)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClick {
//                onForwardClicked(it)
//            }

        buttons += backButton
        buttons += forwardButton
    }

    private fun onBackClicked(button: UIButton2) {
        if (!button.isButtonEnabled()) {
            return
        }

//        val buttonStates = if (button.isHeld()) {
//            game.showPreviousMove(false, FAST_ANIMATION_SPEED)
//        } else {
            game.showPreviousMove(false)
//        }
//        if (buttonStates.first) {
//            button.disable()
//        }
//        if (buttonStates.second) {
//            game.clearBoardData()
//            enableForwardButton()
//        }
        onBackClicked()
    }

    private fun onForwardClicked(button: UIButton2) {
        if (!button.isButtonEnabled()) {
            return
        }

//        val buttonStates = if (button.isHeld()) {
//            game.showNextMove(false, FAST_ANIMATION_SPEED)
//        } else {
            game.showNextMove(false)
//        }

//        if (buttonStates.first) {
//            button.disable()
//        }
//        if (buttonStates.second) {
//            game.clearBoardData()
//            enableBackButton()
//        }
        onForwardClicked()
    }

    fun onButtonInitialized(textSize: Float) {
        if (textSize < maxTextSize) {
            maxTextSize = textSize
        }

        numberOfButtonsInitialized++

        if (numberOfButtonsInitialized == numberOfButtons) {
            for (button in buttons) {
//                button.setFinalTextSize(maxTextSize)
            }
        }
    }

    fun runOnUiThread(callback: () -> Unit) {
        requireActivity().runOnUiThread {
            callback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        backButton.destroy()
//        forwardButton.destroy()
    }

    companion object {
        val BACKGROUND_COLOR = Color.rgb(0.15f, 0.15f, 0.15f)

        const val TEXT_SIZE = 16f
    }

}