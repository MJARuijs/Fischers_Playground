package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class PracticeOpeningNavigationBarFragment : GameBarFragment() {

    private lateinit var onHintClicked: () -> Unit
    private lateinit var onSolutionClicked: () -> Unit
    private lateinit var onRetryClicked: () -> Unit
    private lateinit var onNextClicked: () -> Unit
    private lateinit var onExitClicked: () -> Unit
    private lateinit var onNextMoveClicked: () -> Unit
    private lateinit var onCheckArrowsClicked: () -> Unit

    private lateinit var hintButton: UIButton2
    private lateinit var solutionButton: UIButton2
    private lateinit var retryButton: UIButton2
    private lateinit var nextButton: UIButton2
    private lateinit var exitButton: UIButton2
    private lateinit var nextMoveButton: UIButton2
    private lateinit var checkArrowsButton: UIButton2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hintButton = UIButton2(requireContext())
        hintButton
            .setText("Hint")
            .setIcon(R.drawable.hint_icon, Color.WHITE)
            .setIconPadding(0, 4, 0, 0)
            .setColor(BACKGROUND_COLOR)
            .setTextSize(TEXT_SIZE)
            .setOnClickListener {
                showSolutionButton()
                onHintClicked()
            }

        solutionButton = UIButton2(requireContext())
        solutionButton
            .setText("Solution")
            .setIcon(R.drawable.solution_icon, Color.WHITE)
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

        exitButton = UIButton2(requireContext())
        exitButton
            .setText("Exit")
            .setIcon(R.drawable.exit_icon)
            .setTextSize(TEXT_SIZE)
            .setColorResource(R.color.accent_color)
            .setOnClickListener {
                onExitClicked()
            }

        nextMoveButton = UIButton2(requireContext())
        nextMoveButton
            .setText("Next Move")
            .setTextSize(TEXT_SIZE)
            .setIconPadding(0, 4, 0, 0)
            .setIcon(R.drawable.arrow_forward)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onNextMoveClicked()
            }

        checkArrowsButton = UIButton2(requireContext())
        checkArrowsButton
            .setText("Check Arrows")
            .setTextSize(TEXT_SIZE)
            .setIconPadding(0, 4, 0, 0)
            .setIcon(R.drawable.check_circled_icon)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onCheckArrowsClicked()
            }

        addButton(hintButton)
        addButton(solutionButton)
        addButton(retryButton)
        addButton(nextButton)
        addButton(exitButton)
        addButton(nextMoveButton)
        addButton(checkArrowsButton)

        hintButton.show()
        solutionButton.hide()
        retryButton.hide()
        nextButton.hide()
        exitButton.hide()
        nextMoveButton.hide()
        checkArrowsButton.hide()

        backButton.hide()
        forwardButton.hide()
    }

    fun showHintButton() {
        solutionButton.hide()
        retryButton.hide()
        nextButton.hide()
        hintButton.show()
        nextMoveButton.hide()
        checkArrowsButton.hide()
    }

    fun showSolutionButton() {
        solutionButton.show()
        retryButton.hide()
        nextButton.hide()
        hintButton.hide()
        nextMoveButton.hide()
        checkArrowsButton.hide()
    }

    fun showRetryButton() {
        retryButton.show()
        solutionButton.hide()
        hintButton.hide()
        nextButton.hide()
        nextMoveButton.hide()
        checkArrowsButton.hide()
    }

    fun showNextButton() {
        retryButton.hide()
        solutionButton.hide()
        hintButton.hide()
        nextButton.show()
        checkArrowsButton.hide()
    }

    fun showExitButton() {
        retryButton.hide()
        solutionButton.hide()
        hintButton.hide()
        nextButton.hide()
        exitButton.show()
        nextMoveButton.hide()
        checkArrowsButton.hide()
    }

    fun showNextMoveButton() {
        retryButton.hide()
        solutionButton.hide()
        hintButton.hide()
        nextButton.hide()
        exitButton.hide()
        nextMoveButton.show()
        checkArrowsButton.hide()
    }

    fun showCheckArrowButton() {
        retryButton.hide()
        solutionButton.hide()
        hintButton.hide()
        nextButton.hide()
        exitButton.hide()
        nextMoveButton.hide()
        checkArrowsButton.show()
    }

    companion object {

         fun getInstance(game: Game, evaluateNavigationButtons: () -> Unit, onHintClicked: () -> Unit, onSolutionClicked: () -> Unit, onRetryClicked: () -> Unit, onNextClicked: () -> Unit, onExitClicked: () -> Unit, onNextMoveClicked: () -> Unit, onCheckArrowsClicked: () -> Unit): PracticeOpeningNavigationBarFragment {
             val fragment = PracticeOpeningNavigationBarFragment()
             fragment.init(game, evaluateNavigationButtons)
             fragment.onHintClicked = onHintClicked
             fragment.onSolutionClicked = onSolutionClicked
             fragment.onRetryClicked = onRetryClicked
             fragment.onNextClicked = onNextClicked
             fragment.onExitClicked = onExitClicked
             fragment.onNextMoveClicked = onNextMoveClicked
             fragment.onCheckArrowsClicked = onCheckArrowsClicked
             return fragment
         }

     }

}