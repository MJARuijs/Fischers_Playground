package com.mjaruijs.fischersplayground.activities.opening

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.GameActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.MoveFeedbackIcon
import com.mjaruijs.fischersplayground.util.Time
import java.util.*
import kotlin.math.roundToInt

class PracticeActivity : GameActivity() {

    override var isSinglePlayer = true

    override var activityName = "practice_activity"

    private var hintRequested = false
    private var madeMistakes = false
    private var finishedPracticeSession = false

    private val lines = LinkedList<OpeningLine>()
    private var currentLine: OpeningLine? = null
    private var nextLine: OpeningLine? = null
    private var currentMoveIndex = 0

    private val variationLines = ArrayList<OpeningLine>()

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var opening: Opening
    private lateinit var session: PracticeSession

    private var resumeSession = false

    private lateinit var moveFeedbackIcon: MoveFeedbackIcon
    private lateinit var practiceNavigationButtons: PracticeOpeningActionButtonsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        findViewById<FragmentContainerView>(R.id.upper_fragment_container).visibility = View.GONE

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"
        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create $activityName. Missing essential information: opening_team.."))

        if (!intent.hasExtra("resume_session")) {
            throw IllegalArgumentException("Missing essential information for starting PracticeActivity: resume_session")
        }

        opening = dataManager.getOpening(openingName, openingTeam)

        resumeSession = intent.getBooleanExtra("resume_session", false)
        if (resumeSession) {
            session = dataManager.getPracticeSession(openingName, openingTeam) ?: throw IllegalArgumentException("Tried to resume session with name: $openingName and team $openingTeam, but could not find it..")
            if (session.currentLine != null) {
                lines += session.currentLine!!
            }
            if (session.nextLine != null) {
                lines += session.nextLine!!
            }
            for (line in session.lines) {
                lines += line
            }
        } else {
            val variationNames = intent.getStringArrayListExtra("variation_name") ?: ArrayList()
            for (variationName in variationNames) {
                val variation = opening.getVariation(variationName) ?: throw IllegalArgumentException("Could not find variation with name: $variationName in opening with name: $openingName")
                for (line in variation.lines) {
                    variationLines += line
                }
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

        loadPracticeActionButtons()
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

        if (resumeSession) {
            resumePracticeSession()
        } else {
            startNewPracticeSession()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!finishedPracticeSession) {
//            saveSession()
        }
    }

    override fun onMoveMade(move: Move) {
        super.onMoveMade(move)

        glView.clearHighlightedSquares()

        if (move.team == openingTeam) {
            runOnUiThread {
                checkMoveCorrectness(move)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (!finishedPracticeSession) {
            saveSession()
        }
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
        setUpLineState()
    }

    private fun checkMoveCorrectness(move: Move) {
        if (move == currentLine!!.lineMoves[currentMoveIndex]) {
            currentMoveIndex++
            if (isLastMoveInLine(move)) {
                showMoveFeedback(move.getToPosition(openingTeam), true)
                processLineFinished()
            } else {
                (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
                (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showHintButton()
            }
            hintRequested = false
        } else {
            if (!madeMistakes) {
                madeMistakes = true
                if (nextLine == null) {
                    nextLine = currentLine
                } else {
                    lines.addLast(currentLine)
                }
                saveSession()
            }
            showMoveFeedback(move.getToPosition(openingTeam), false)
            (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showRetryButton()
        }
    }

    private fun processLineFinished() {
        (getActionBarFragment() as PracticeOpeningActionButtonsFragment).showNextButton()
        val isFinished = getNextLine()
        if (isFinished) {
            finishedPracticeSession = true
            finishedPracticingOpening()
        } else {
            saveSession()
        }
//        saveSession()
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

    private fun startNewPracticeSession() {
        for (line in variationLines.shuffled()) {
            if (line.lineMoves.isNotEmpty()) {
                lines += line
            }
        }

        if (lines.isNotEmpty()) {
            currentLine = lines.pop()
        } else {
            return
        }

        if (lines.isNotEmpty()) {
            nextLine = lines.pop()
        }

        saveSession()
        setUpLineState()
    }

    private fun resumePracticeSession() {
        if (lines.isEmpty()) {
            return
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

        game.resetMoves()

        for (move in currentLine!!.setupMoves) {
            (game as SinglePlayerGame).setMove(move)
        }

        val firstMove = currentLine!!.lineMoves[currentMoveIndex]
        if (firstMove.team != openingTeam) {
            (game as SinglePlayerGame).move(firstMove)
            currentMoveIndex++
        }

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

    private fun isLastMoveInLine(move: Move): Boolean {
        if (currentLine == null) {
            return true
        }

        return currentLine!!.lineMoves.indexOf(move) >= currentLine!!.lineMoves.size - 2
    }

    private fun finishedPracticingOpening() {
        Toast.makeText(applicationContext, "Done with opening!", Toast.LENGTH_SHORT).show()
        supportFragmentManager.commit {
            hide(practiceNavigationButtons)
        }
        dataManager.removePracticeSession(openingName, openingTeam)
        dataManager.savePracticeSessions(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.DELETE_PRACTICE_SESSION, "$userId|$openingName|$openingTeam"))
    }

    private fun loadPracticeActionButtons() {
        practiceNavigationButtons = PracticeOpeningActionButtonsFragment.getInstance(game, ::evaluateNavigationButtons, ::onHintClicked, ::onSolutionClicked, ::onRetryClicked, ::onNextClicked)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, practiceNavigationButtons)
        }
    }

    private fun getDisplayWidth(): Int {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize.x
    }

    private fun saveSession() {
        val practiceSession = PracticeSession(openingName, openingTeam, currentLine, nextLine, lines)
        dataManager.setPracticeSession(openingName, practiceSession)
        dataManager.savePracticeSessions(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.NEW_PRACTICE_SESSION, "$userId|$openingName|$openingTeam|$practiceSession"))
    }
}