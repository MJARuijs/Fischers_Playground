package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class CreateOpeningActionButtonsFragment : GameBarFragment() {

    private lateinit var addLineButton: UIButton2
    private lateinit var recordButton: UIButton2
    private lateinit var startPracticeButton: UIButton2

    private lateinit var onStartRecording: () -> Unit
    private lateinit var onAddLine: () -> Unit
    private lateinit var onStartPracticing: () -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.action_bar_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addLineButton = UIButton2(requireContext())
        recordButton = UIButton2(requireContext())
        startPracticeButton = UIButton2(requireContext())

        addLineButton
            .setText("Add Line")
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setIconPadding(0, 4, 0, 0)
            .setIcon(R.drawable.add_icon)
            .setOnClickListener {
                onAddLine()
            }

        recordButton
            .setText("Record")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.record_icon, Color.RED)
            .setIconPadding(0, 4, 0, 0)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onStartRecording()
            }

        startPracticeButton
            .setText("Practice")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.student_icon, Color.WHITE)
            .setIconPadding(0, 0, 0, 0)
            .setColor(Color.rgb(235, 186, 145))
            .setOnClickListener {
                onStartPracticing()
            }

        addButtonToLeft(startPracticeButton)
        addButtonToLeft(recordButton)
        addButtonToLeft(addLineButton)
    }

    companion object {
        const val TEXT_SIZE = 16f

        val BACKGROUND_COLOR = Color.rgb(0.15f, 0.15f, 0.15f)

        fun getInstance(game: Game, evaluateNavigationButtons: () -> Unit, onStartRecording: () -> Unit, onAddLine: () -> Unit, onStartPracticing: () -> Unit, onBackClicked: () -> Unit, onForwardClicked: () -> Unit): CreateOpeningActionButtonsFragment {
            val fragment = CreateOpeningActionButtonsFragment()
            fragment.init(game, evaluateNavigationButtons, onBackClicked, onForwardClicked)
            fragment.onStartRecording = onStartRecording
            fragment.onAddLine = onAddLine
            fragment.onStartPracticing = onStartPracticing
            return fragment
        }
    }
}