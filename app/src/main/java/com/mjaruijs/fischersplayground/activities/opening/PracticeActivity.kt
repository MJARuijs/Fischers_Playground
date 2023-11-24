package com.mjaruijs.fischersplayground.activities.opening

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowMetrics
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.GameActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.fragments.PracticeProgressFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionBarFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeOpeningNavigationBarFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.userinterface.BoardOverlay
import com.mjaruijs.fischersplayground.userinterface.MoveFeedbackIcon
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time
import java.util.LinkedList
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class PracticeActivity : GameActivity() {

    override var isSinglePlayer = true

    override var activityName = "practice_activity"

    private var hintRequested = false
    private var madeMistakes = false
    private var drawingArrows = false

    private var finishedPracticeSession = false

    private val lines = LinkedList<OpeningLine>()
    private var currentLine: OpeningLine? = null
    private var nextLine: OpeningLine? = null
    private var currentMove: Move? = null
    private var currentMoveIndex = 0

    private val variationLines = ArrayList<OpeningLine>()

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var opening: Opening
    private lateinit var session: PracticeSession

    private var resumeSession = false
    private var useShuffle: Boolean? = null
    private var practiceArrows = false
    private var arrowStartSquare = Vector2(-1, -1)
    private var arrows = ArrayList<MoveArrow>()
    private var hintedSquares = ArrayList<Vector2>()

    private lateinit var moveFeedbackIcon: MoveFeedbackIcon
    private lateinit var practiceNavigationButtons: PracticeOpeningNavigationBarFragment
    private lateinit var progressFragment: PracticeProgressFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_practice_opening)
        super.onCreate(savedInstanceState)

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"
        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create $activityName. Missing essential information: opening_team.."))

        if (!intent.hasExtra("resume_session")) {
            throw IllegalArgumentException("Missing essential information for starting PracticeActivity: resume_session")
        }

        opening = dataManager.getOpening(openingName, openingTeam)

        resumeSession = intent.getBooleanExtra("resume_session", false)

        val totalLineCount: Int
        val practiceProgress: Int

        if (resumeSession) {
            try {
                session = dataManager.getPracticeSession(openingName, openingTeam) ?: throw IllegalArgumentException("Tried to resume session with name: $openingName and team $openingTeam, but could not find it..")
                practiceArrows = session.practiceArrows

                if (session.currentLine != null) {
                    lines += session.currentLine!!
                }
                if (session.nextLine != null) {
                    lines += session.nextLine!!
                }
                for (line in session.lines) {
                    lines += line
                }

                totalLineCount = session.totalLineCount
                practiceProgress = session.currentLineIndex
            } catch (e: Exception) {
                throw e
            }
        } else {
            try {
                val variationNames = intent.getStringArrayListExtra("variation_name") ?: ArrayList()
                for (variationName in variationNames) {
                    val variation = opening.getVariation(variationName) ?: throw IllegalArgumentException("Could not find variation with name: $variationName in opening with name: $openingName")
                    for (line in variation.lines) {
                        variationLines += line
                    }
                }

                if (!intent.hasExtra("use_shuffle")) {
                    throw IllegalArgumentException("Missing essential information for starting PracticeActivity: use_shuffle")
                }

                useShuffle = intent.getBooleanExtra("use_shuffle", false)
                practiceArrows = intent.getBooleanExtra("practice_arrows", false)

                totalLineCount = variationLines.size
                practiceProgress = 0
            } catch (e: Exception) {
                throw e
            }
        }

        isPlayingWhite = openingTeam == Team.WHITE
        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp(), false)

        moveFeedbackIcon = findViewById(R.id.move_feedback_icon)
        moveFeedbackIcon.setPosition(Vector2())
        moveFeedbackIcon.doOnLayout {
            Logger.debug(activityName, "MoveFeedBackIcon width: ${it.width}")
            moveFeedbackIcon.scaleToSize((getWindowWidth().toFloat() / 8f / 2f).roundToInt())
            moveFeedbackIcon.hide()
        }

        progressFragment = PracticeProgressFragment.getInstance(practiceProgress, totalLineCount)

        if (!isPlayingWhite) {
            boardOverlay.swapCharactersForBlack()
        }

        loadPracticeActionButtons()

        supportActionBar?.show()
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar_view)
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = openingName
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ActionBarFragment.BACKGROUND_COLOR))
    }

    override fun onContextCreated() {
        super.onContextCreated()
//        runOnUiThread {
//            val constraints = ConstraintSet()
//            constraints.clone(gameLayout)
//            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
//            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)
//            constraints.clear(R.id.lower_fragment_container, ConstraintSet.TOP)
//            constraints.constrainHeight(R.id.lower_fragment_container, -2)
//            constraints.applyTo(gameLayout)
//            gameLayout.invalidate()
//        }

        setGameCallbacks()
        setGameForRenderer()

        if (resumeSession) {
            resumePracticeSession()
        } else {
            startNewPracticeSession()
        }
    }

    override fun setGameCallbacks() {
        super.setGameCallbacks()
        game.onAnimationStarted = ::onMoveAnimationStarted
        game.onAnimationFinished = ::onMoveAnimationFinished
    }

    override fun onClick(x: Float, y: Float) {
        super.onClick(x, y)

        if (drawingArrows) {
            if (arrowStartSquare.x == -1f) {
                arrowStartSquare = game.determineSelectedSquare(x, y, displayWidth, displayHeight)
                glView.highlightSquare(arrowStartSquare)
            } else {
                val arrowEndSquare = game.determineSelectedSquare(x, y, displayWidth, displayHeight)
                onArrowToggled(arrowStartSquare, arrowEndSquare)
                arrowStartSquare = Vector2(-1, -1)
                glView.clearHighlightedSquares()
                for (square in hintedSquares) {
                    glView.highlightSquare(square)
                }
                glView.requestRender()
            }
        } else {
            if (game.board.isASquareSelected()) {
                boardOverlay.hideArrows()
            } else if (!game.pieceMoving) {
                boardOverlay.draw()
            }
        }
    }

    private fun onArrowToggled(startSquare: Vector2, endSquare: Vector2) {
        val arrow = MoveArrow(startSquare, endSquare)
        if (!arrow.isValidArrow()) {
            return
        }

        val deletedArrow = boardOverlay.toggleArrow(arrow)

        if (deletedArrow) {
            arrows.remove(arrow)
        } else {
            arrows += arrow
        }

        boardOverlay.draw()
    }

    private fun onMoveAnimationStarted() {
        boardOverlay.clearArrows()
    }

    private fun onMoveAnimationFinished(moveIndex: Int) {
        val move = game.getCurrentMove() ?: return
        glView.clearHighlightedSquares()

        if (move.team == openingTeam) {
            runOnUiThread {
                checkMoveCorrectness(move, moveIndex)
            }
        } else {
            boardOverlay.drawArrows(currentLine!!.arrows[moveIndex] ?: ArrayList())
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
        if (drawingArrows) {
            val correctArrows = currentLine!!.arrows[currentMoveIndex + currentLine!!.setupMoves.size - 1] ?: ArrayList()
            boardOverlay.drawArrows(correctArrows)
            drawingArrows = false

            hintedSquares.clear()
            arrows.clear()
            glView.clearHighlightedSquares()
            glView.enableLastMoveHighlights()

            if (isLastMoveInLine(currentMove!!)) {
                (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showNextLineButton()
            } else {
                (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showNextMoveButton()
            }
            processLineFailed()
        } else {
            glView.clearHighlightedSquares()
            (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex])
            hintRequested = false
        }
    }

    private fun onRetryClicked() {
        (game as SinglePlayerGame).undoLastMove()
        moveFeedbackIcon.hide()
        if (hintRequested) {
            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showSolutionButton()
            giveHint()
        } else {
            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showHintButton()
        }
    }

    private fun onNextLineClicked() {
        setUpLineState()
    }

    private fun onExitClicked() {
        finish()
    }

    private fun onNextMoveClicked() {
        (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
        boardOverlay.drawArrows(currentLine!!.arrows[currentMoveIndex + currentLine!!.setupMoves.size - 1] ?: ArrayList())
        (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showHintButton()
    }

    private fun onCheckArrowsClicked() {
        val correctArrows = currentLine!!.arrows[currentMoveIndex + currentLine!!.setupMoves.size - 1] ?: ArrayList()
        if (correctArrows.containsAll(arrows) && arrows.containsAll(correctArrows)) {
            drawingArrows = false
            arrows.clear()
            hintedSquares.clear()
            glView.clearHighlightedSquares()
            glView.enableLastMoveHighlights()

            if (isLastMoveInLine(currentMove!!)) {
                progressFragment.incrementCurrent()
                showMoveFeedback(currentMove!!.getToPosition(openingTeam), true)
                processLineFinished()
            } else {
                if (currentLine!!.arrows.containsKey(game.currentMoveIndex)) {
                    onNextMoveClicked()
                } else {
                    (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
                    (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showHintButton()
                }
            }
            hintRequested = false
        } else {
            processLineFailed()
            Toast.makeText(applicationContext, "Try again!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkMoveCorrectness(move: Move, moveIndex: Int) {
        currentMove = move

        if (move == currentLine!!.lineMoves[currentMoveIndex]) {
            currentMoveIndex++
            arrows.clear()

            if (!practiceArrows) {
                boardOverlay.drawArrows(currentLine!!.arrows[moveIndex] ?: ArrayList())

                if (isLastMoveInLine(move)) {
                    progressFragment.incrementCurrent()
                    showMoveFeedback(move.getToPosition(openingTeam), true)
                    processLineFinished()
                } else {
                    if (currentLine!!.arrows.containsKey(game.currentMoveIndex)) {
                        (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showNextMoveButton()
                    } else {
                        (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
                        (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showHintButton()
                    }
                }
                hintRequested = false
            } else {
                if (currentLine!!.arrows[moveIndex] != null && currentLine!!.arrows[moveIndex]!!.isNotEmpty()) {
                    drawingArrows = true
                    glView.disableLastMoveHighlights()
//                    glView.clearHighlightedSquares()
//                    glView.clearHighlightedSquares()
                    (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showButton(PracticeOpeningNavigationBarFragment.CHECK_ARROWS_BUTTON, PracticeOpeningNavigationBarFragment.HINT_BUTTON)
                } else {
                    if (isLastMoveInLine(move)) {
                        progressFragment.incrementCurrent()
                        showMoveFeedback(move.getToPosition(openingTeam), true)
                        processLineFinished()
                    } else {
                        if (currentLine!!.arrows.containsKey(game.currentMoveIndex)) {
                            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showNextMoveButton()
                        } else {
                            (game as SinglePlayerGame).move(currentLine!!.lineMoves[currentMoveIndex++])
                            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showHintButton()
                        }
                    }
                    hintRequested = false
                }
            }
        } else {
            processLineFailed()
            showMoveFeedback(move.getToPosition(openingTeam), false)
            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showRetryButton()
        }
    }

    private fun processLineFailed() {
        if (!madeMistakes) {
            madeMistakes = true
            progressFragment.incrementMax(2)
            if (nextLine == null) {
                nextLine = currentLine
            } else {
                lines.addLast(currentLine)
            }
            saveSession()
        }
    }

    private fun processLineFinished() {
        (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showNextLineButton()
        val isFinished = getNextLine()
        if (isFinished) {
            finishedPracticeSession = true
            finishedPracticingOpening()
        } else {
            saveSession()
        }
    }

    private fun giveHint() {
        if (currentLine == null) {
            return
        }

        processLineFailed()

        if (drawingArrows) {
            val correctArrows = currentLine!!.arrows[currentMoveIndex + currentLine!!.setupMoves.size - 1] ?: ArrayList()

            for (arrow in correctArrows) {
                if (!arrows.contains(arrow)) {
                    if (!hintedSquares.contains(arrow.startSquare)) {
                        glView.highlightSquare(arrow.startSquare)
                        hintedSquares += arrow.startSquare
                        Logger.debug(activityName, "Hinting ${arrow.startSquare} -> ${arrow.endSquare}")
                    }
                }
            }

            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showButton(PracticeOpeningNavigationBarFragment.CHECK_ARROWS_BUTTON, PracticeOpeningNavigationBarFragment.SOLUTION_BUTTON)
            for (arrow in arrows) {
                if (!correctArrows.contains(arrow)) {
                    if (!hintedSquares.contains(arrow.startSquare)) {
                        glView.highlightSquare(arrow.startSquare)
                        hintedSquares += arrow.startSquare
                    }
                }
            }
            glView.requestRender()
        } else {
            (getActionBarFragment() as PracticeOpeningNavigationBarFragment).showSolutionButton()

            val currentMove = currentLine!!.lineMoves[currentMoveIndex]
            val startSquare = currentMove.getFromPosition(openingTeam)

            glView.highlightSquare(startSquare)
            glView.requestRender()
        }
    }

    private fun showMoveFeedback(square: Vector2, correctMove: Boolean) {
        val x = square.x.roundToInt()
        val flippedY = 7 - square.y.roundToInt()

        val squareWidth = getWindowWidth() / 8.0f
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

        val transitionedSquare = (Vector2(x, flippedY) / 8.0f) * getWindowWidth() + offset
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
        if (useShuffle!!) {
            for (line in variationLines.shuffled()) {
                if (line.lineMoves.isNotEmpty()) {
                    lines += line
                }
            }
        } else {
            for (line in variationLines) {
                if (line.lineMoves.isNotEmpty()) {
                    lines += line
                }
            }
        }

        if (lines.isNotEmpty()) {
            currentLine = lines.pop()
        } else {
            Logger.warn(activityName, "Starting new practice session without setting currentLine, because there are no known lines..?")
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

        if (currentLine == null) {
            return
        }

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
        progressFragment.complete()
        practiceNavigationButtons.showExitButton()

        dataManager.removePracticeSession(openingName, openingTeam, applicationContext)
    }

    private fun loadPracticeActionButtons() {
        practiceNavigationButtons = PracticeOpeningNavigationBarFragment.getInstance(game, ::evaluateNavigationButtons, ::onHintClicked, ::onSolutionClicked, ::onRetryClicked, ::onNextLineClicked, ::onExitClicked, ::onNextMoveClicked, ::onCheckArrowsClicked)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, practiceNavigationButtons)
            replace(R.id.lower_fragment_container, progressFragment)
        }
    }

    private fun getWindowWidth(): Int {
        return windowManager.currentWindowMetrics.bounds.width()
    }

    private fun saveSession() {
        val practiceSession = PracticeSession(openingName, openingTeam, practiceArrows, progressFragment.currentValue, progressFragment.maxValue, currentLine, nextLine, lines)
        dataManager.setPracticeSession(openingName, practiceSession, applicationContext)
    }
}