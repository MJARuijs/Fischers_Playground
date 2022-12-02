package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class PracticeOpeningActionButtonsFragment : GameBarFragment() {

    private lateinit var onHintClicked: () -> Unit
    private lateinit var onSolutionClicked: () -> Unit
    private lateinit var onRetryClicked: () -> Unit
    private lateinit var onNextClicked: () -> Unit

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
            .setIconPadding(0, 4, 0, 0)
            .setTextSize(TEXT_SIZE)
            .setOnClickListener {
                showSolutionButton()
                onHintClicked()
            }

        solutionButton = UIButton2(requireContext())
        solutionButton
            .setText("Solution")
            .setIcon(R.drawable.solution_icon)
            .setIconPadding(0, 4, 0, 0)
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
            .setIconPadding(0, 4, 0, 0)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onRetryClicked()
            }

        nextButton = UIButton2(requireContext())
        nextButton
            .setText("Next Line")
            .setIcon(R.drawable.next_arrow_circle_icon, Color.rgb(235, 186, 145))
            .setIconPadding(0, 4, 0, 0)
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

     companion object {

         fun getInstance(game: Game, evaluateNavigationButtons: () -> Unit, onHintClicked: () -> Unit, onSolutionClicked: () -> Unit, onRetryClicked: () -> Unit, onNextClicked: () -> Unit): PracticeOpeningActionButtonsFragment {
             val fragment = PracticeOpeningActionButtonsFragment()
             fragment.init(game, evaluateNavigationButtons)
             fragment.onHintClicked = onHintClicked
             fragment.onSolutionClicked = onSolutionClicked
             fragment.onRetryClicked = onRetryClicked
             fragment.onNextClicked = onNextClicked
             return fragment
         }

     }

}