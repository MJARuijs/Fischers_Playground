package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class CreateOpeningActionButtonsFragment(game: Game, private val onStartRecording: () -> Unit, private val onAddLine: () -> Unit, private val onStartPracticing: () -> Unit, onBackClicked: () -> Unit, onForwardClicked: () -> Unit) : GameBarFragment(game, onBackClicked, onForwardClicked) {

    private lateinit var addLineButton: UIButton2
    private lateinit var recordButton: UIButton2
    private lateinit var startPracticeButton: UIButton2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addLineButton = UIButton2(requireContext())
        recordButton = UIButton2(requireContext())
        startPracticeButton = UIButton2(requireContext())

        addLineButton
            .setText("Add Line")
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setIcon(R.drawable.add_icon)
            .setOnClickListener {
                onAddLine()
            }

        recordButton
            .setText("Record")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.record_icon, Color.RED)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onStartRecording()
            }

        startPracticeButton
            .setText("Practice")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.student_icon)
            .setColor(Color.rgb(235, 186, 145))
            .setOnClickListener {
                onStartPracticing()
            }

        addButtonsToLeft(startPracticeButton)
        addButtonsToLeft(recordButton)
        addButtonsToLeft(addLineButton)
    }

}