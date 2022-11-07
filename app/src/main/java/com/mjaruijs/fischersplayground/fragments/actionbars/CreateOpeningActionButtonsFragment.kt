package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class CreateOpeningActionButtonsFragment(game: Game, private val onStartRecording: () -> Unit, private val onStopRecording: () -> Unit, private val onStartPracticing: () -> Unit, onBackClicked: () -> Unit, onForwardClicked: () -> Unit) : GameBarFragment(game, onBackClicked, onForwardClicked) {

    private lateinit var startRecordingButton: UIButton2
    private lateinit var stopRecordingButton: UIButton2
    private lateinit var startPracticeButton: UIButton2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startRecordingButton = UIButton2(requireContext())
        stopRecordingButton = UIButton2(requireContext())
        startPracticeButton = UIButton2(requireContext())

        startRecordingButton
            .setText("Record")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.record_icon)
            .setIconColor(Color.RED)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onStartRecording()
                startRecordingButton.disable()
                stopRecordingButton.enable()
            }

        stopRecordingButton
            .setText("Stop")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.stop_icon)
            .setIconColor(Color.WHITE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onStopRecording()
                startRecordingButton.enable()
                stopRecordingButton.disable()
            }

        startPracticeButton
            .setText("Practice")
            .setTextSize(TEXT_SIZE)
            .setIcon(R.drawable.student_icon)
            .setIconColor(Color.WHITE)
            .setColor(Color.rgb(235, 186, 145))
            .setOnClickListener {
                onStartPracticing()
            }

        addButtonsToLeft(startPracticeButton)
        addButtonsToLeft(stopRecordingButton)
        addButtonsToLeft(startRecordingButton)
    }
}