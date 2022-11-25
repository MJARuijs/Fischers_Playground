package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.MoveHeaderView
import com.mjaruijs.fischersplayground.userinterface.OpeningMovesRowView
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

class OpeningMovesFragment2 : Fragment() {

    private lateinit var onMoveClick: (Move, Boolean) -> Unit
    private val setupMoves = ArrayList<Move>()
    private val lineMoves = ArrayList<Move>()

    private lateinit var typeFace: Typeface
    private lateinit var scrollView: ScrollView
    private lateinit var moveTable: TableLayout

//    private var rowOffset = 0
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
//            rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0

            addHeaderRow(LINE_MOVES_TEXT, true)
            addLineMoves()
        }

        moveTable.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
//            selectLastMove()
        }
    }

    fun getOpeningLine() = OpeningLine(setupMoves, lineMoves)

    private fun selectLastMove() {
        if (setupMoves.isEmpty() && lineMoves.isEmpty()) {
            Logger.debug("MyTag", "MOVES ARE EMPTY")
            return
        }

        selectMove(setupMoves.size + lineMoves.size - 1)
    }

    fun selectMove(index: Int) {
        deselectAllMoves()

        Logger.debug(TAG, "Clicked on index: $index")

        currentMoveIndex = index

        if (index == -1) {
            scrollView.post {
                scrollView.smoothScrollTo(0, 0)
            }
            return
        }

        val rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0
        val headerOffset = if (index < setupMoves.size) 1 else 2 + rowOffset

        val rowIndex = index / 2 + headerOffset

        val rowView = moveTable[rowIndex] as OpeningMovesRowView
        if (index % 2 == 0) {
            rowView.selectWhiteMove()
        } else {
            rowView.selectBlackMove()
        }

        val tableHeight = scrollView.measuredHeight
        val selectedRowBottom = moveTable[rowIndex].y + moveTable[rowIndex].height
        val selectedRowTop = moveTable[rowIndex].y

        val scrollingDown = if (selectedRowBottom > tableHeight + scrollView.scrollY) {
            true
        } else if (selectedRowTop < scrollView.scrollY) {
            false
        } else {
            null
        }

        val dY = if (scrollingDown == null) {
            -1
        } else if (scrollingDown) {
            val difference = selectedRowBottom - tableHeight
            difference.roundToInt()
        } else {
            if (rowIndex <= 1) {
                0
            } else {
                selectedRowTop.roundToInt()
            }
        }

        if (dY != -1) {
            scrollView.post {
                scrollView.smoothScrollTo(0, dY)
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

    fun addMove(move: Move) {
        if (hasHeader(LINE_MOVES_TEXT)) {
            addLineMove(move)
        } else {
            addSetupMove(move)
        }
    }

    private fun addSetupMove(move: Move) {
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

    private fun addLineMove(move: Move) {
        deselectAllMoves()

        if (!isShowingLastMove()) {
            Logger.debug(TAG, "Not showing latest move! Deleting moves after $currentMoveIndex")
            deleteMovesAfter(currentMoveIndex)
        }

        if (lineMoves.isEmpty()) {
//            rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0

//            if (!hasHeader(LINE_MOVES_TEXT)) {
//                Logger.debug(TAG, "Does not have header")
//
//                addHeaderRow(LINE_MOVES_TEXT, true)
//            }

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

    fun clear() {
        deleteMovesAfter(-1)
    }

    fun test() {
        deleteMovesAfter(currentMoveIndex)
    }

    fun addHeaderRow(text: String, scrollDown: Boolean) {
        if (currentMoveIndex != -1) {
            val currentMoveIndexCopy = currentMoveIndex

            if (currentMoveIndex < setupMoves.size) {
//                Logger.debug(TAG, "if")
                val newLineMoves = ArrayList<Move>()

                for (i in currentMoveIndex + 1 until setupMoves.size + lineMoves.size) {
                    newLineMoves += if (i < setupMoves.size) {
                        setupMoves[i]
                    } else {
                        lineMoves[i - setupMoves.size]
                    }
                }

                if (!isShowingLastMove()) {
                    deleteMovesAfter(currentMoveIndex)
                }

                val headerView = MoveHeaderView(requireContext())
                headerView.setText(text)

                moveTable.addView(headerView)

                for (move in newLineMoves) {
                    addLineMove(move)
                }

//                selectMove(currentMoveIndexCopy)
            } else {
//                Logger.debug(TAG, "else")

                val newSetupMoves = ArrayList<Move>()
                val newLineMoves = ArrayList<Move>()

                for (move in setupMoves) {
                    newSetupMoves += move
                }

                for (i in 0 until currentMoveIndex - setupMoves.size + 1) {
                    newSetupMoves += lineMoves[i]
                }

                for (i in currentMoveIndex - setupMoves.size + 1 until lineMoves.size) {
                    newLineMoves += lineMoves[i]
                }

                deleteMovesAfter(-1)

                for (move in newSetupMoves) {
                    addSetupMove(move)
                }

                val headerView = MoveHeaderView(requireContext())
                headerView.setText(LINE_MOVES_TEXT)

                moveTable.addView(headerView)

                for (move in newLineMoves) {
                    addLineMove(move)
                }
            }

//            selectMove(currentMoveIndexCopy)
        } else {
            val headerView = MoveHeaderView(requireContext())
            headerView.setText(text)

            moveTable.addView(headerView)

            if (scrollDown) {
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    // a3 e7 d3 g7 e2

    private fun deleteMovesAfter(index: Int) {
        if (index > setupMoves.size + lineMoves.size) {
            return
        }

        if (index == -1) {
            while (moveTable.childCount > 1) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            currentMoveIndex = -1
            setupMoves.clear()
            lineMoves.clear()
            return
        }

//        if (index < setupMoves.size) {
//            Logger.debug(TAG, "if")
//
//            while (setupMoves.size > index + 1) {
//                setupMoves.removeLast()
//            }
//
//            lineMoves.clear()
//        } else {
//            Logger.debug(TAG, "else")
//
//            while (lineMoves.size > index) {
//                lineMoves.removeLast()
//            }
//        }
//
//        currentMoveIndex = index
//
//        moveTable.removeAllViews()
//
//        Logger.debug(TAG, "Current move index: $currentMoveIndex, ${setupMoves.size} + ${lineMoves.size}")
//
//        addHeaderRow(SETUP_MOVES_TEXT, false)
//        addSetupMoves()
////
//        if (lineMoves.isNotEmpty()) {
//            Logger.debug(TAG, "LineMoves is not empty")
//////            rowOffset = if (setupMoves.last().team == Team.WHITE) 1 else 0
////
////            addHeaderRow(LINE_MOVES_TEXT, true)
////            addLineMoves()
//            val headerView = MoveHeaderView(requireContext())
//            headerView.setText(LINE_MOVES_TEXT)
//
//            moveTable.addView(headerView)
//        } else {
//            Logger.debug(TAG, "Linemoves is empty")
//        }

        if (index < setupMoves.size) {
            val rowIndex = index / 2 + 2

            while (moveTable.childCount > rowIndex) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            while (setupMoves.size > index + 1) {
                setupMoves.removeLast()
            }

            if (setupMoves.isNotEmpty()) {
                if (setupMoves.last().team == Team.WHITE) {
                    (moveTable[moveTable.childCount - 1] as OpeningMovesRowView).hideBlackMove()
                }
            }

            lineMoves.clear()
        } else {
            val rowIndex = index / 2 + 4

            while (moveTable.childCount > rowIndex) {
                moveTable.removeViewAt(moveTable.childCount - 1)
            }

            while (lineMoves.size > index) {
                lineMoves.removeLast()
            }

            if (lineMoves.isNotEmpty()) {
                if (lineMoves.last().team == Team.WHITE) {
                    (moveTable[moveTable.childCount - 1] as OpeningMovesRowView).hideBlackMove()
                }
            }
        }
    }

    private fun isShowingLastMove(): Boolean {
        return currentMoveIndex == setupMoves.size + lineMoves.size - 1
    }

    private fun hasHeader(text: String): Boolean {
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
        selectMove(currentMoveIndex)
    }

    companion object {

        private const val TAG = "OpeningMovesFragment"

        const val SETUP_MOVES_TEXT = "Setup Moves"
        const val LINE_MOVES_TEXT = "Line Moves"

        fun getInstance(onMoveClick: (Move, Boolean) -> Unit, setupMoves: ArrayList<Move> = ArrayList(), lineMoves: ArrayList<Move> = ArrayList()): OpeningMovesFragment2 {
            val fragment = OpeningMovesFragment2()

            fragment.onMoveClick = onMoveClick
            for (move in setupMoves) {
                fragment.setupMoves += move
            }

            for (move in lineMoves) {
                fragment.lineMoves += move
            }

            return fragment
        }
    }
}