package com.mjaruijs.fischersplayground.activities.game

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.OpeningMovesFragment2.Companion.LINE_MOVES_TEXT
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionBarFragment.Companion.BACKGROUND_COLOR
import com.mjaruijs.fischersplayground.fragments.actionbars.CreateOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.userinterface.MoveFeedbackIcon
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time
import java.util.*
import kotlin.math.roundToInt

class CreateOpeningActivity : GameActivity() {

    override var activityName = "practice_activity_2"

    override var isSinglePlayer = true

//    private val setupMoves = ArrayList<Move>()
//    private val lineMoves = ArrayList<Move>()

    private var selectedLine: OpeningLine? = null

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var opening: Opening

    private var recording = false
    private var hasUnsavedChanges = false
    private var practicing = false

    private var hintRequested = false
    private var madeMistakes = false

    private lateinit var gameState: String

    private val lines = LinkedList<OpeningLine>()
    private var currentLine: OpeningLine? = null
    private var nextLine: OpeningLine? = null
    private var currentMoveIndex = 0

    private lateinit var openingMovesFragment: OpeningMovePagerFragment
    private lateinit var moveFeedbackIcon: MoveFeedbackIcon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        findViewById<FragmentContainerView>(R.id.upper_fragment_container).visibility = View.GONE

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"

        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create CreateOpeningActivity. Missing essential information: opening_team.."))
        opening = dataManager.getOpening(openingName, openingTeam)

        if (opening.lines.isNotEmpty()) {
            selectedLine = opening.lines[0]
            for (move in opening.lines[0].setupMoves) {
//                setupMoves += move
            }

            for (move in opening.lines[0].lineMoves) {
//                lineMoves += move
            }
        }

        isPlayingWhite = openingTeam == Team.WHITE

        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp())

        moveFeedbackIcon = findViewById(R.id.move_feedback_icon)
        moveFeedbackIcon.setPosition(Vector2())
        moveFeedbackIcon.doOnLayout {
            moveFeedbackIcon.scaleToSize((getDisplayWidth().toFloat() / 8f / 2f).roundToInt())
            moveFeedbackIcon.hide()
        }

        openingMovesFragment = OpeningMovePagerFragment(::onLineSelected, ::onMoveClicked, opening.lines)

        loadCreatingActionButtons()

        supportActionBar?.show()
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar_view)
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = openingName
        supportActionBar?.setBackgroundDrawable(ColorDrawable(BACKGROUND_COLOR))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_opening_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_line_button -> {
                openingMovesFragment.deleteCurrentLine()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    override fun onPause() {
        saveOpening()
        super.onPause()
    }

    override fun onMoveMade(move: Move) {
        super.onMoveMade(move)

        glView.clearHighlightedSquares()

        if (practicing) {
            if (move.team == openingTeam) {
                runOnUiThread {
                    checkMoveCorrectness(move)
                }
            }
        } else {
            if (recording) {
//                lineMoves += move
                runOnUiThread {
                    openingMovesFragment.getCurrentOpeningFragment().addLineMove(move)
                }
            } else {
//                setupMoves += move
                runOnUiThread {
                    openingMovesFragment.getCurrentOpeningFragment().addSetupMove(move)
                }
            }
        }
    }

    private fun onLineSelected(line: OpeningLine, selectedMoveIndex: Int) {
//        val openingMoves = ArrayList<Move>()

//        for (move in line.setupMoves) {
//            openingMoves += move
//            setupMoves += move
//        }
//
//        for (move in line.lineMoves) {
//            openingMoves += move
//            lineMoves += move
//        }

        selectedLine = line
        game.swapMoves(line.getAllMoves(), selectedMoveIndex)
        evaluateActionButtons()
        requestRender()
    }

    // TODO: Maybe add a dialog that allows copying options based on current line
    // Don't copy
    // Copy as is: setup moves -> setup moves & line moves -> line moves
    // Copy setup moves only: setup moves -> setup moves & no line moves
    // Copy line moves: setup moves -> setup moves & line moves -> setup moves. This is the current implementation
    // Maybe allow copying a selection of moves?
    private fun onLineAdded() {
//        val lineSetupMoves = ArrayList<Move>()

//        for (move in setupMoves) {
//            lineSetupMoves += move
//        }
//
//        for (move in lineMoves) {
//            lineSetupMoves += move
//        }

        findFragment<OpeningMovePagerFragment>()?.addLine(selectedLine?.getAllMoves() ?: arrayListOf())
        (getActionBarFragment() as CreateOpeningActionButtonsFragment).showStartRecordingButton()
        requestRender()
    }

    private fun onMoveClicked(move: Move, deleteModeActive: Boolean) {
        Logger.debug("MyTag", "Click! ${game.currentMoveIndex}")
        if (deleteModeActive) {
            game.clearBoardData()
        } else {
            game.goToMove(move)
            openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex)
            evaluateActionButtons()
        }
    }

    private fun onStartRecording() {
        (getActionBarFragment() as CreateOpeningActionButtonsFragment).disableAddLineButton()
//        (getActionBarFragment() as CreateOpeningActionButtonsFragment).enableStopRecordingButton()

        recording = true
//        lineMoves.clear()
        gameState = game.state.toString()
        runOnUiThread {
            if (!openingMovesFragment.getCurrentOpeningFragment().hasHeader(LINE_MOVES_TEXT)) {
                openingMovesFragment.getCurrentOpeningFragment().addHeaderRow(LINE_MOVES_TEXT, true)
            }
        }
    }

    private fun onStopRecording() {
        (getActionBarFragment() as CreateOpeningActionButtonsFragment).showStartRecordingButton()
        (getActionBarFragment() as CreateOpeningActionButtonsFragment).enableAddLineButton()

        recording = false

//        val openingMoves = ArrayList<Move>()

//        for (move in lineMoves) {
//            openingMoves += move
//        }

//        val startMoves = ArrayList<Move>()

//        for (move in setupMoves) {
//            startMoves += move
//        }

//        val newLine = OpeningLine(gameState, startMoves, openingMoves)
//        opening.addLine(newLine)

        hasUnsavedChanges = true
    }

    private fun onHintClicked() {
        hintRequested = true
        giveHint()
    }

    private fun onSolutionClicked() {
        glView.clearHighlightedSquares()
        (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex])
        hintRequested = false
    }

    private fun onRetryClicked() {
        (game as SinglePlayerGame).undoLastMove()
        moveFeedbackIcon.hide()
        if (hintRequested) {
            (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showSolutionButton()
            giveHint()
        } else {
            (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showHintButton()
        }
    }

    private fun onNextClicked() {
        val isFinished = getNextLine()
        if (isFinished) {
            finishedPracticingOpening()
        } else {
            setUpLineState()
        }
    }

    private fun checkMoveCorrectness(move: Move) {
        if (move == currentLine!!.lineMoves[currentMoveIndex]) {
            currentMoveIndex++
            if (currentMoveIndex >= currentLine!!.lineMoves.size) {
                showMoveFeedback(move.getToPosition(openingTeam), true)
                (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showNextButton()
            } else {
                (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
                (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showHintButton()
            }
            hintRequested = false
        } else {
            if (!madeMistakes) {
                madeMistakes = true
                lines.addLast(currentLine)
            }
            showMoveFeedback(move.getToPosition(openingTeam), false)
            (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showRetryButton()
        }
    }

    private fun giveHint() {
        val currentMove = currentLine!!.lineMoves[currentMoveIndex]
        val startSquare = currentMove.getFromPosition(openingTeam)

        glView.highlightSquare(startSquare)
        glView.requestRender()
    }

    private fun showMoveFeedback(square: Vector2, correctMove: Boolean) {
        val x = square.x.roundToInt()
        val flippedY = 7 - square.y.roundToInt()

        val squareWidth = getDisplayWidth() / 8.0f
        val offset = Vector2(0f, 0f)

        if (square.x.roundToInt() == 7) {
            offset.x = -squareWidth * 0.25f
        } else {
            offset.x = squareWidth * 0.75f
        }

        if (square.y.roundToInt() == 7) {
            offset.y = squareWidth * 0.75f
        } else {
            offset.y = -squareWidth * 0.25f
        }

        val transitionedSquare = (Vector2(x, flippedY) / 8.0f) * getDisplayWidth() + offset
        if (correctMove) {
            moveFeedbackIcon.setColor(Color.rgb(0.0f, 0.75f, 0.0f))
            moveFeedbackIcon.setIcon(R.drawable.check_mark_icon)
        } else {
            moveFeedbackIcon.setColor(Color.rgb(0.75f, 0.0f, 0.0f))
            moveFeedbackIcon.setIcon(R.drawable.close_icon)
        }

        moveFeedbackIcon.setPosition(transitionedSquare)
        moveFeedbackIcon.show()
    }

    private fun onStartPracticing() {
//        (openingMovesFragment.getCurrentOpeningFragment().deleteMovesAfter(openingMovesFragment.getCurrentOpeningFragment().currentMoveIndex))

        if (hasUnsavedChanges) {
            saveOpening()
        }

        supportFragmentManager.commit {
            hide(openingMovesFragment)
        }

        practicing = true

        loadPracticeActionButtons()

        for (line in opening.lines) {
            lines += line
        }

        currentLine = lines.pop()

        if (lines.isNotEmpty()) {
            nextLine = lines.pop()
        }

        setUpLineState()
    }

    private fun setUpLineState() {
        currentMoveIndex = 0
        hintRequested = false
        madeMistakes = false
//        game.state = GameState.fromString(currentLine!!.startingState)

        game.resetMoves()

        for (move in currentLine!!.setupMoves) {
            (game as SinglePlayerGame).setMove(move)
        }

        val firstMove = currentLine!!.lineMoves[currentMoveIndex++]
        (game as SinglePlayerGame).move(firstMove)

        moveFeedbackIcon.hide()
        requestRender()
    }

    private fun getNextLine(): Boolean {
        if (madeMistakes) {
            if (nextLine == null) {
                nextLine = currentLine
            } else {
                val temp = currentLine
                currentLine = nextLine
                nextLine = temp
            }
        } else {
            if (currentLine == nextLine) {
                return true
            }

            currentLine = nextLine

            if (currentLine == null) {
                return true
            }

            nextLine = if (lines.isEmpty()) {
                null
            } else {
                lines.pop()
            }
        }
        return false
    }

    private fun onBackClicked() {
        openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex)
    }

    private fun onForwardClicked() {
        openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex)
    }

    private fun finishedPracticingOpening() {
        Toast.makeText(applicationContext, "Done with opening!", Toast.LENGTH_SHORT).show()
    }

    private fun loadCreatingActionButtons() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, CreateOpeningActionButtonsFragment(game, ::onStartRecording, ::onLineAdded, ::onStopRecording, ::onStartPracticing, ::onBackClicked, ::onForwardClicked))
            replace(R.id.lower_fragment_container, openingMovesFragment)
        }
    }

    private fun loadPracticeActionButtons() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeOpeningActionButtonsFragment(game, ::onHintClicked, ::onSolutionClicked, ::onRetryClicked, ::onNextClicked))
        }
    }

    private fun saveOpening() {
        opening.clear()

        val openingFragments = openingMovesFragment.getFragments()

        for (fragment in openingFragments) {
            val line = (fragment as OpeningMovesFragment2).getOpeningLine()
            opening.addLine(line)
        }

        dataManager.setOpening(openingName, openingTeam, opening)
        dataManager.saveOpenings(applicationContext)
        hasUnsavedChanges = false
    }

    private fun getDisplayWidth(): Int {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize.x
    }

}