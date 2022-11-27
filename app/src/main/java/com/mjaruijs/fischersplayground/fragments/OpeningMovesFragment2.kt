package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnScrollChangeListener
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import androidx.core.view.children
import androidx.core.view.doOnLayout
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

class OpeningMovesFragment2 : Fragment() {

    private lateinit var onMoveClick: (Move, Boolean) -> Unit
    private val setupMoves = ArrayList<Move>()
    private val lineMoves = ArrayList<Move>()

    private lateinit var typeFace: Typeface
    private lateinit var scrollView: ScrollView
    private lateinit var moveTable: TableLayout

    private lateinit var layoutChangeListener: LayoutChangeListener

    var currentMoveIndex = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        typeFace = resources.getFont(R.font.anonymous_bold)

        scrollView = view.findViewById(R.id.scroll_view)
        moveTable = view.findViewById(R.id.move_table)

        addSetupHeader()
        addSetupMoves()

        if (lineMoves.isNotEmpty()) {
            addLineHeader()
            addLineMoves()
        }

        layoutChangeListener = LayoutChangeListener()

        if (moveTable.childCount > 0) {

            scrollView.addOnLayoutChangeListener(layoutChangeListener)
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (scrollY + scrollView.height == moveTable.height) {
                    scrollView.removeOnLayoutChangeListener(layoutChangeListener)
                }
            }
        }
    }

    private fun addSetupHeader() {
        val headerView = MoveHeaderView(requireContext())
        headerView.setText(SETUP_MOVES_TEXT)

        moveTable.addView(headerView)
    }

    private fun addLineHeader() {
        val headerView = MoveHeaderView(requireContext())
        headerView.setText(LINE_MOVES_TEXT)

        moveTable.addView(headerView)
    }

    fun getOpeningLine() = OpeningLine(setupMoves, lineMoves)

    fun selectLastMove() {
        if (setupMoves.isEmpty() && lineMoves.isEmpty()) {
            return
        }

        selectMove(setupMoves.size + lineMoves.size - 1, false)
    }

    fun selectMove(index: Int, scrollToMove: Boolean) {
        deselectAllMoves()

        currentMoveIndex = index

        if (index == -1) {
            if (scrollToMove) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, 0)
                }
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

        if (scrollToMove) {
//            rowView.doOnLayout {
                scrollToView(rowView)
//            }
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
        deselectAllMoves()

        if (!isShowingLastMove()) {
            Logger.debug(TAG, "Not showing latest move! Deleting moves after $currentMoveIndex")
            deleteMovesAfter(currentMoveIndex)
        }

        if (hasLineHeader()) {
            addLineMove(move, true)
        } else {
            addSetupMove(move, true)
        }
    }

    private fun addSetupMove(move: Move, scrollToMove: Boolean) {
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

            if (scrollToMove) {
                movesView.doOnLayout {
                    scrollToView(movesView)
                }
            }
        } else {
            val movesView = moveTable[moveTable.childCount - 1] as OpeningMovesRowView
            movesView.setBlackMove(move, onMoveClick)

            if (scrollToMove) {
                scrollToView(movesView)
            }
        }

        setupMoves += move
        currentMoveIndex++

//        scrollView.post {
//            Logger.debug(TAG, "Scrolling down after SetupMove")
//            scrollView.fullScroll(View.FOCUS_DOWN)
//        }
    }

    private fun addLineMove(move: Move, scrollToMove: Boolean) {

        if (lineMoves.isEmpty()) {
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

            if (scrollToMove) {
                movesView.doOnLayout {
                    scrollToView(movesView)
                }
            }
        } else {
            if (move.team == Team.WHITE) {
                val headerOffset = if (setupMoves.last().team == Team.WHITE) 2 else 1
                val moveNumber = moveTable.childCount - headerOffset
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

                if (scrollToMove) {
                    movesView.doOnLayout {
                        scrollToView(movesView)
                    }
                }
            } else {
                val lastRow = moveTable[moveTable.childCount - 1] as OpeningMovesRowView
                lastRow.setBlackMove(move, onMoveClick)

                if (scrollToMove) {
                    scrollToView(lastRow)
                }
            }
        }

        lineMoves += move
        currentMoveIndex++
    }

    fun clear() {
        deleteMovesAfter(-1)
    }

    fun setLineHeader() {
        if (currentMoveIndex != -1) {
            val currentMoveIndexCopy = currentMoveIndex

            val headerView = MoveHeaderView(requireContext())
            headerView.setText(LINE_MOVES_TEXT)

            if (currentMoveIndex < setupMoves.size) {
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

                if (!hasLineHeader()) {
                    moveTable.addView(headerView)
                }

                for (move in newLineMoves) {
                    addLineMove(move, false)
                }
            } else {
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
                    addSetupMove(move, false)
                }

                moveTable.addView(headerView)

                for (move in newLineMoves) {
                    addLineMove(move, false)
                }
            }

            selectMove(currentMoveIndexCopy, false)
            headerView.doOnLayout {
                scrollToView(headerView)
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

            currentMoveIndex = -1
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

            while (lineMoves.size > index - setupMoves.size + 1) {
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

    private fun hasLineHeader(): Boolean {
        if (moveTable.childCount == 0) {
            return false
        }

        for (row in moveTable.children) {
            if (row !is MoveHeaderView) {
                continue
            }

            if (row.getText() == LINE_MOVES_TEXT) {
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

        currentMoveIndex = setupMoves.size - 1
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
        selectMove(currentMoveIndex, false)
    }

    private fun scrollToView(view: View) {
        val rowIndex = moveTable.indexOfChild(view)

        if (rowIndex == -1) {
            return
        }

        val tableHeight = scrollView.measuredHeight
        val selectedRowBottom = moveTable[rowIndex].y + moveTable[rowIndex].height
        val selectedRowTop = moveTable[rowIndex].y

//        Logger.debug(TAG, "Scrolling to view: $rowIndex ::: $selectedRowBottom > $tableHeight + ${scrollView.scrollY} || $selectedRowTop < ${scrollView.scrollY}")

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
//                Logger.debug(TAG, "Scrolling view to: $dY")
                scrollView.smoothScrollTo(0, dY)
            }
        }
    }

    inner class LayoutChangeListener : OnLayoutChangeListener {
        override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            selectLastMove()
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

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