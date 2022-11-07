package com.mjaruijs.fischersplayground.fragments.actionbars

import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

open class GameBarFragment(var game: Game, private val onBackClicked: () -> Unit = {}, private val onForwardClicked: () -> Unit = {}) : ActionBarFragment() {

    protected lateinit var backButton: UIButton2
    protected lateinit var forwardButton: UIButton2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = UIButton2(requireContext())
        backButton
            .setText("Back")
            .setIcon(R.drawable.arrow_back)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)

            .disable()
            .setOnClickListener {
                onBackClicked(backButton)
            }

        forwardButton = UIButton2(requireContext())
        forwardButton
            .setText("Forward")
            .setIcon(R.drawable.arrow_forward)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)

            .disable()
            .setOnClickListener {
                onForwardClicked(forwardButton)
            }

        addButtons(backButton)
        addButtons(forwardButton)
    }

    fun hideButtons() {
        backButton.visibility = View.GONE
        forwardButton.visibility = View.GONE
    }

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

    fun evaluateNavigationButtons() {
        if (game.moves.isNotEmpty()) {
            if (game.getMoveIndex() != -1) {
                enableBackButton()
            } else {
                disableBackButton()
            }
            if (!game.isShowingCurrentMove()) {
                enableForwardButton()
            } else {
                disableForwardButton()
            }
        } else {
            disableButtons()
        }
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
            game.clearBoardData()
//            enableForwardButton()
//        }
        evaluateNavigationButtons()
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
            game.clearBoardData()
//            enableBackButton()
//        }
        evaluateNavigationButtons()
        onForwardClicked()
    }

}