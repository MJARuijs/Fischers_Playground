package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.GameState
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.fragments.actionbars.CreateOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time
import java.util.LinkedList

class CreateOpeningActivity : GameActivity() {

    override var activityName = "practice_activity_2"

    override var isSinglePlayer = true

    private val moves = ArrayList<Move>()

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var opening: Opening

    private var recording = false
    private var hasUnsavedChanges = false
    private var practicing = false

    private lateinit var gameState: String

    private val lines = LinkedList<OpeningLine>()
    private lateinit var currentLine: OpeningLine
    private lateinit var nextLine: OpeningLine
    private var currentMoveIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        findViewById<FragmentContainerView>(R.id.upper_fragment_container).visibility = View.GONE

        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp())

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"

        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create CreateOpeningActivity. Missing essential information: opening_team.."))
        opening = dataManager.getOpening(openingName, openingTeam)
        
        loadCreatingActionButtons()
    }

    override fun onContextCreated() {
        super.onContextCreated()
        runOnUiThread {
            val constraints = ConstraintSet()
            constraints.clone(gameLayout)
            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)

            constraints.applyTo(gameLayout)
            gameLayout.invalidate()
        }

        setGameCallbacks()
        setGameForRenderer()
    }

    override fun onResume() {
        getActionBarFragment().game = game
        super.onResume()
    }

    override fun onPause() {
        saveOpening()
        super.onPause()
    }

    override fun onMoveMade(move: Move) {
        if (recording) {
            moves += move
        }

        if (practicing) {
            if (move.team == openingTeam) {
                checkMoveCorrectness(move)
            }
        }
    }

    private fun onStartRecording() {
        recording = true
        moves.clear()
        gameState = game.state.toString()
    }

    private fun onStopRecording() {
        recording = false

        val newLine = OpeningLine(gameState, moves)
        opening.addLine(newLine)

        hasUnsavedChanges = true
    }

    private fun onHintClicked() {

    }

    private fun onStartPracticing() {
        if (hasUnsavedChanges) {
            saveOpening()
        }

        practicing = true

        loadPracticeActionButtons()

        for (line in opening.lines.shuffled()) {
            lines += line
        }

        currentLine = lines.pop()

        if (lines.isNotEmpty()) {
            nextLine = lines.pop()
        }

        setUpLineState()
    }

    private fun setUpLineState() {
        game.state = GameState.fromString(currentLine.startingState)

        val firstMove = currentLine.moves[currentMoveIndex++]
        (game as SinglePlayerGame).move(firstMove)
        requestRender()
    }

    private fun checkMoveCorrectness(move: Move) {
        if (move == currentLine.moves[currentMoveIndex]) {
            Logger.debug("MyTag", "Correct!")
            currentMoveIndex++
            (game as SinglePlayerGame).move(currentLine.moves[currentMoveIndex++])
        } else {
            Logger.debug("MyTag", "Wrong! Correct move was: ${currentLine.moves[currentMoveIndex].toChessNotation()}")
        }
    }

    private fun loadPracticeActionButtons() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeOpeningActionButtonsFragment(::requestRender, ::onHintClicked))
        }
    }

    private fun loadCreatingActionButtons() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, CreateOpeningActionButtonsFragment(::requestRender, ::onStartRecording, ::onStopRecording, ::onStartPracticing))
        }
    }

    private fun saveOpening() {
        dataManager.setOpening(openingName, openingTeam, opening)
        dataManager.saveOpenings(applicationContext)
        hasUnsavedChanges = false
    }

}