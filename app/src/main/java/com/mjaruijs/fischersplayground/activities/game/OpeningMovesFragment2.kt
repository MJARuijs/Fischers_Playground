package com.mjaruijs.fischersplayground.activities.game

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import androidx.core.view.children
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.MoveHeaderView
import com.mjaruijs.fischersplayground.userinterface.OpeningMovesRowView
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

class OpeningMovesFragment2(private val onMoveClick: (Move, Boolean) -> Unit, private val setupMoves: ArrayList<Move> = arrayListOf(), private val lineMoves: ArrayList<Move> = arrayListOf()) : Fragment() {

    private lateinit var typeFace: Typeface

    private lateinit var scrollView: ScrollView

    private lateinit var moveTable: TableLayout

    private var rowOffset = 0
    var currentMoveIndex = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        typeFace = resources.getFont(R.font.anonymous_bold)

        scrollView = view.findViewById(R.id.scroll_view)
        moveTable = view.findViewById(R.id.move_table)

        addHeaderRow(SETUP_MOVES_TEXT, false)
        addSetupMoves()

        if (lineMoves.isNotEmpty()) {
            rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0

            addHeaderRow(LINE_MOVES_TEXT, false)
            addLineMoves()
        }
    }

    fun getOpeningLine() = OpeningLine("", setupMoves, lineMoves)

    fun selectMove(index: Int) {
        deselectAllMoves()

        val scrollingDown = index > currentMoveIndex

        currentMoveIndex = index

        if (index == -1) {
            return
        }

        val headerOffset = if (index < setupMoves.size) 1 else 2 + rowOffset

        val rowIndex = index / 2 + headerOffset

        val rowView = moveTable[rowIndex] as OpeningMovesRowView
        if (index % 2 == 0) {
            rowView.selectWhiteMove()
        } else {
            rowView.selectBlackMove()
        }

        val numberOfHeaders = if (index < setupMoves.size) 1 else 2
//        val numberOfMoveRows =

        val isSetupMove = index < setupMoves.size



        val tableHeight = scrollView.height
        val selectedRowBottom = moveTable[rowIndex].y + moveTable[rowIndex].height

        Logger.debug("MyTag", "Current row y: $selectedRowBottom. TableHeight: $tableHeight. yScroll: ${scrollView.scrollY}. Height + Scroll: ${tableHeight + scrollView.scrollY}")

        val dY = if (scrollingDown) {
            if (selectedRowBottom > tableHeight + scrollView.scrollY) {
                val difference = selectedRowBottom - tableHeight
//                (moveTable[rowIndex].y - tableHeight).roundToInt()
//                scrollView.scrollY + moveTable[rowIndex].height
                difference.roundToInt()
            } else {
                -1
            }
        } else {
            -1
        }

        Logger.debug("MyTag", "dy: $dY ${scrollView.scrollY}")

        if (dY != -1) {
            scrollView.post {
//                scrollView.smoothScrollTo(0, dY)
                scrollView.scrollTo(0, dY)


                Logger.debug("MyTag", "New scrollY: ${scrollView.scrollY}")
            }
        }
    }

    private fun deselectAllMoves() {
        for (child in moveTable.children) {
            if (child is OpeningMovesRowView) {
                child.deselectWhiteMove()
                child.deselectBlackMove()
            }
        }
        moveTable.invalidate()
    }

    fun addSetupMove(move: Move) {
        deselectAllMoves()

        deleteMovesAfter(currentMoveIndex)

        if (move.team == Team.WHITE) {
            val moveNumber = setupMoves.size / 2 + 1
            val movesView = OpeningMovesRowView(requireContext())
            movesView.setMoveNumber(moveNumber)
            movesView.setTypeFace(typeFace)
            movesView.setWhiteMove(move, onMoveClick)

            if (moveNumber % 2 == 1) {
                movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
            } else {
                movesView.setBackgroundColor(Color.DKGRAY)
            }

            moveTable.addView(movesView)
        } else {
            val movesView = moveTable[moveTable.childCount - 1] as OpeningMovesRowView
            movesView.setBlackMove(move, onMoveClick)
        }

        setupMoves += move
        currentMoveIndex++

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun addLineMove(move: Move) {
        deselectAllMoves()

        deleteMovesAfter(currentMoveIndex)

        if (lineMoves.isEmpty()) {
            rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0

            if (!hasHeader(LINE_MOVES_TEXT)) {
                addHeaderRow(LINE_MOVES_TEXT, true)
            }

            val moveNumber = setupMoves.size / 2 + 1
            val movesView = OpeningMovesRowView(requireContext())

            movesView.setTypeFace(typeFace)
            movesView.setMoveNumber(moveNumber)

            if (move.team == Team.WHITE) {
                movesView.setWhiteMove(move, onMoveClick)
            } else {
                movesView.setBlackMove(move, onMoveClick)
            }

            if (moveNumber % 2 == 1) {
                movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
            } else {
                movesView.setBackgroundColor(Color.DKGRAY)
            }

            moveTable.addView(movesView)
        } else {
            if (move.team == Team.WHITE) {
                val moveNumber = setupMoves.size / 2 + 1 + lineMoves.size / 2 + 1
                val movesView = OpeningMovesRowView(requireContext())
                movesView.setTypeFace(typeFace)
                movesView.setMoveNumber(moveNumber)
                movesView.setWhiteMove(move, onMoveClick)

                if (moveNumber % 2 == 1) {
                    movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
                } else {
                    movesView.setBackgroundColor(Color.DKGRAY)
                }

                moveTable.addView(movesView)
            } else {
                val lastRow = moveTable[moveTable.childCount - 1] as OpeningMovesRowView
                lastRow.setBlackMove(move, onMoveClick)
            }
        }

        lineMoves += move
        currentMoveIndex++

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun removeLastMove() {
        if (moveTable.childCount == 0) {
            return
        }

        if (lineMoves.isNotEmpty()) {
            lineMoves.removeLast()
        } else if (setupMoves.isNotEmpty()) {
            setupMoves.removeLast()
        }

        val lastRow = moveTable.children.last()
        if (lastRow is OpeningMovesRowView) {
            if (lastRow.isBlackMoveHidden()) {
                moveTable.removeView(lastRow)
            } else {
                lastRow.hideBlackMove()
                if (lastRow.areBothMovesHidden()) {
                    moveTable.removeView(lastRow)
                }
            }

            if (moveTable.childCount != 0) {
                val newLastRow = moveTable.children.last()
                if (newLastRow is MoveHeaderView) {
                    if (newLastRow.getText() != SETUP_MOVES_TEXT) {
                        moveTable.removeView(newLastRow)
                    }
                }
            }
        }

        currentMoveIndex--
    }

    fun addHeaderRow(text: String, scrollDown: Boolean) {
        val headerView = MoveHeaderView(requireContext())
        headerView.setText(text)
        moveTable.addView(headerView)

        if (scrollDown) {
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun deleteMovesAfter(index: Int) {
        if (index > setupMoves.size + lineMoves.size) {
            return
        }

        if (index == -1) {
            while (moveTable.childCount > 1) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            setupMoves.clear()
            lineMoves.clear()
            return
        }

        if (index < setupMoves.size) {
            val rowIndex = index / 2 + 2

            while (moveTable.childCount > rowIndex) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            while (setupMoves.size > index + 1) {
                setupMoves.removeLast()
            }

            lineMoves.clear()
        } else {
            val rowIndex = index / 2 + 4

            while (moveTable.childCount > rowIndex) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            while (lineMoves.size > index + 1) {
                lineMoves.removeLast()
            }
        }
    }

    fun hasHeader(text: String): Boolean {
        if (moveTable.childCount == 0) {
            return false
        }

        for (row in moveTable.children) {
            if (row !is MoveHeaderView) {
                continue
            }

            if (row.getText() == text) {
                return true
            }
        }

        return false
    }

    private fun getHeaderHeight(): Int {
        return moveTable[0].height
    }

    private fun addSetupMoves() {
        for (i in 0 until setupMoves.size step 2) {
            val movesView = OpeningMovesRowView(requireContext())
            val moveNumber = i / 2 + 1
            val whiteMove = setupMoves[i]
            movesView.setTypeFace(typeFace)
            movesView.setMoveNumber(moveNumber)
            movesView.setWhiteMove(whiteMove, onMoveClick)

            if (moveNumber % 2 == 1) {
                movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
            } else {
                movesView.setBackgroundColor(Color.DKGRAY)
            }

            if (setupMoves.size > i + 1) {
                movesView.setBlackMove(setupMoves[i + 1], onMoveClick)
            }

            moveTable.addView(movesView)
        }

        deselectAllMoves()
    }

    private fun addLineMoves() {
        var lineMovesStartIndex = 0

        if (lineMoves.first().team == Team.BLACK) {
            val moveNumber = setupMoves.size / 2 + 1
            val movesView = OpeningMovesRowView(requireContext())
            val move = lineMoves.first()

            movesView.setTypeFace(typeFace)
            movesView.setMoveNumber(moveNumber)
            movesView.setBlackMove(move, onMoveClick)

            if (moveNumber % 2 == 1) {
                movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
            } else {
                movesView.setBackgroundColor(Color.DKGRAY)
            }

            moveTable.addView(movesView)
            lineMovesStartIndex = 1
        }

        for (i in lineMovesStartIndex until lineMoves.size step 2) {
            val moveNumber = setupMoves.size / 2 + i / 2 + 1 + lineMovesStartIndex
            val movesView = OpeningMovesRowView(requireContext())
            val whiteMove = lineMoves[i]
            movesView.setTypeFace(typeFace)
            movesView.setMoveNumber(moveNumber)
            movesView.setWhiteMove(whiteMove, onMoveClick)

            if (moveNumber % 2 == 1) {
                movesView.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
            } else {
                movesView.setBackgroundColor(Color.DKGRAY)
            }

            if (lineMoves.size > i + 1) {
                movesView.setBlackMove(lineMoves[i + 1], onMoveClick)
            }

            moveTable.addView(movesView)
        }

        deselectAllMoves()
    }

    companion object {

        const val SETUP_MOVES_TEXT = "Setup Moves"
        const val LINE_MOVES_TEXT = "Line Moves"

    }
}