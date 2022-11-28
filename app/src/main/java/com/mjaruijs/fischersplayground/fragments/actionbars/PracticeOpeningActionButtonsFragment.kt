package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class PracticeOpeningActionButtonsFragment(game: Game, private val onHintClicked: () -> Unit, private val onSolutionClicked: () -> Unit, private val onRetryClicked: () -> Unit, private val onNextClicked: () -> Unit) : GameBarFragment(game) {

    private lateinit var hintButton: UIButton2
    private lateinit var solutionButton: UIButton2
    private lateinit var retryButton: UIButton2
    private lateinit var nextButton: UIButton2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hintButton = UIButton2(requireContext())
        hintButton
            .setText("Hint")
            .setIcon(R.drawable.hint_icon)
            .setColor(BACKGROUND_COLOR)
            .setTextSize(TEXT_SIZE)
            .setOnClickListener {
                showSolutionButton()
                onHintClicked()
            }

        solutionButton = UIButton2(requireContext())
        solutionButton
            .setText("Solution")
            .setIcon(R.drawable.solution_icon)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                showHintButton()
                onSolutionClicked()
            }

        retryButton = UIButton2(requireContext())
        retryButton
            .setText("Retry")
            .setIcon(R.drawable.retry_icon, mirroredX = true)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onRetryClicked()
            }

        nextButton = UIButton2(requireContext())
        nextButton
            .setText("Next Line")
            .setIcon(R.drawable.next_arrow_icon, Color.rgb(235, 186, 145))
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                showHintButton()
                onNextClicked()
            }

        addButtons(hintButton)
        addButtons(solutionButton)
        addButtons(retryButton)
        addButtons(nextButton)

        hintButton.show()
        solutionButton.hide()
        retryButton.hide()
        nextButton.hide()

        backButton.hide()
        forwardButton.hide()
    }

    fun showHintButton() {
        solutionButton.hide()
        retryButton.hide()
        nextButton.hide()
        hintButton.show()
    }

    fun showSolutionButton() {
        solutionButton.show()
        retryButton.hide()
        nextButton.hide()
        hintButton.hide()
    }

    fun showRetryButton() {
        retryButton.show()
        solutionButton.hide()
        hintButton.hide()
        nextButton.hide()
    }

    fun showNextButton() {
        retryButton.hide()
        solutionButton.hide()
        hintButton.hide()
        nextButton.show()
    }

}