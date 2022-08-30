package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.userinterface.UIButton

open class ActionButtonsFragment(layoutResource: Int, val requestRender: () -> Unit) : Fragment(layoutResource) {

    var maxTextSize = Float.MAX_VALUE

    lateinit var backButton: UIButton
    lateinit var forwardButton: UIButton

    lateinit var game: Game

    lateinit var networkManager: NetworkManager

    open var numberOfButtons: Int = 2
    var numberOfButtonsInitialized = 0

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkManager = NetworkManager.getInstance()

        val textOffset = 65
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
            .setTextYOffset(textOffset)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .disable()
            .setOnClickListener {
                if ((it as UIButton).disabled) {
                    return@setOnClickListener
                }

                val buttonStates = game.showPreviousMove()
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    game.clearBoardData()
                    enableForwardButton()
                }
                requestRender()
            }

        forwardButton = view.findViewById(R.id.forward_button)
        forwardButton
            .setText("Forward")
            .setColoredDrawable(R.drawable.arrow_forward)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setChangeIconColorOnHover(false)
            .setTextYOffset(textOffset)
            .disable()
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                if ((it as UIButton).disabled) {
                    return@setOnClickListener
                }

                val buttonStates = game.showNextMove()
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    enableBackButton()
                }
                requestRender()
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

}