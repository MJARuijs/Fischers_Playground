package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.userinterface.UIButton

open class ActionButtonsFragment(layoutResource: Int) : Fragment(layoutResource) {

    lateinit var requestRender: () -> Unit
    lateinit var networkManager: NetworkManager

    private var maxTextSize = Float.MAX_VALUE
    private var numberOfButtonsInitialized = 0

    private lateinit var backButton: UIButton
    private lateinit var forwardButton: UIButton

    lateinit var game: Game

    open var numberOfButtons: Int = 2

    val buttons = ArrayList<UIButton>()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)

        backButton = view.findViewById(R.id.back_button)
        backButton
            .setText("Back")
            .setColoredDrawable(R.drawable.arrow_back)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .disable()
            .setRepeatOnHold(100L, ::runOnUiThread)
            .setOnClick {
                if (it.disabled) {
                    return@setOnClick
                }

                val buttonStates = if (it.isHeld()) {
                    game.showPreviousMove(false, 100L)
                } else {
                    game.showPreviousMove(false)
                }
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    game.clearBoardData()
                    enableForwardButton()
                }
            }

        forwardButton = view.findViewById(R.id.forward_button)
        forwardButton
            .setText("Forward")
            .setColoredDrawable(R.drawable.arrow_forward)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setChangeIconColorOnHover(false)
            .disable()
            .setRepeatOnHold(100L, ::runOnUiThread)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                if (it.disabled) {
                    return@setOnClick
                }

                val buttonStates = if (it.isHeld()) {
                    game.showNextMove(100L)
                } else {
                    game.showNextMove()
                }

                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    game.clearBoardData()
                    enableBackButton()
                }
            }

        buttons += backButton
        buttons += forwardButton
    }

    fun onButtonInitialized(textSize: Float) {
        if (textSize < maxTextSize) {
            maxTextSize = textSize
        }

        numberOfButtonsInitialized++

        if (numberOfButtonsInitialized == numberOfButtons) {
            for (button in buttons) {
                button.setFinalTextSize(maxTextSize)
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
        backButton.destroy()
        forwardButton.destroy()
    }

}