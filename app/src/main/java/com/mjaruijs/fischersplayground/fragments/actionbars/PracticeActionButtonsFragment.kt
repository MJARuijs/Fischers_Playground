package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PracticeActionButtonsFragment(requestRender: () -> Unit, networkManager: NetworkManager) : ActionButtonsFragment(R.layout.practice_actionbar) {

    private lateinit var startRecordingButton: UIButton
    private lateinit var stopRecordingButton: UIButton

    private val recordedMoves = ArrayList<Move>()

    private var isRecording = false

    override var numberOfButtons = 4

    init {
        this.requestRender = requestRender
        this.networkManager = networkManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)

        startRecordingButton = view.findViewById(R.id.start_recording_button)
        stopRecordingButton = view.findViewById(R.id.stop_recording_button)

        startRecordingButton
            .setText("Record")
            .setTexturedDrawable(R.drawable.record_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                isRecording = true
                stopRecordingButton.enable()
//                (it as UIButton).setColor(255, 0, 0)
            }

        stopRecordingButton
            .setText("Stop")
            .setColoredDrawable(R.drawable.stop_icon)
            .setButtonTextSize(50.0f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .disable()
            .setOnClickListener {
                isRecording = false
                stopRecordingButton.disable()
            }

        buttons += startRecordingButton
        buttons += stopRecordingButton
    }

    override fun onResume() {
        game.onMoveMade = { move ->
            if (isRecording) {
                recordedMoves += move
            }
        }
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        startRecordingButton.destroy()
        stopRecordingButton.destroy()
    }

}