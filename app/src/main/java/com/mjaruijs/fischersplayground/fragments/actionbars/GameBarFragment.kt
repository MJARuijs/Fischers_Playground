package com.mjaruijs.fischersplayground.fragments.actionbars

import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

open class GameBarFragment : ActionBarFragment() {

    lateinit var game: Game

    protected lateinit var backButton: UIButton2
    protected lateinit var forwardButton: UIButton2

    protected lateinit var evaluateNavigationButtons: () -> Unit
    protected lateinit var onBackClicked: () -> Unit
    protected lateinit var onForwardClicked: () -> Unit

    fun init(game: Game, evaluateNavigationButtons: () -> Unit, onBackClicked: () -> Unit = {}, onForwardClicked: () -> Unit = {}) {
        this.game = game
        this.evaluateNavigationButtons = evaluateNavigationButtons
        this.onBackClicked = onBackClicked
        this.onForwardClicked = onForwardClicked
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = UIButton2(requireContext())
        backButton
            .setText("Back")
            .setIcon(R.drawable.arrow_back)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setIconPadding(0, 4, 0, 0)
            .setRepeatOnHold()
            .disable()
            .setOnClickListener {
                if (backButton.isButtonEnabled()) {
                    onBackClicked(backButton.isHeld())
                }
            }

        forwardButton = UIButton2(requireContext())
        forwardButton
            .setText("Forward")
            .setIcon(R.drawable.arrow_forward)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setIconPadding(0, 4, 0, 0)
            .setRepeatOnHold()
            .disable()
            .setOnClickListener {
                if (forwardButton.isButtonEnabled()) {
                    onForwardClicked(forwardButton.isHeld())
                }
            }

        addButtons(backButton)
        addButtons(forwardButton)
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
        layout.invalidate()
        layout.requestLayout()
    }

    fun disableBackButton() {
        backButton.disable()
    }

    private fun onBackClicked(isHeld: Boolean) {
        val animationSpeed = if (isHeld) {
            Game.FAST_ANIMATION_SPEED
        } else {
            Game.DEFAULT_ANIMATION_SPEED
        }

        game.showPreviousMove(false, animationSpeed)
        game.clearBoardData()
        evaluateNavigationButtons()
        onBackClicked()
    }

    private fun onForwardClicked(isHeld: Boolean) {
        val animationSpeed = if (isHeld) {
            Game.FAST_ANIMATION_SPEED
        } else {
            Game.DEFAULT_ANIMATION_SPEED
        }

        game.showNextMove(false, animationSpeed)
        game.clearBoardData()
        evaluateNavigationButtons()
        onForwardClicked()
    }

    companion object {

        private const val TAG = "GameBarFragment"

        fun getInstance(game: Game, evaluateNavigationButtons: () -> Unit, onBackClicked: () -> Unit = {}, onForwardClicked: () -> Unit = {}): GameBarFragment {
            val fragment = GameBarFragment()
            fragment.init(game, evaluateNavigationButtons, onBackClicked, onForwardClicked)
            return fragment
        }

    }

}