package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingmovesadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.OpeningMoveView
import com.mjaruijs.fischersplayground.util.Logger

class OpeningMovesFragment : Fragment() {

    private lateinit var typeFace: Typeface

    private lateinit var verticalScrollView: ScrollView

    private lateinit var moveCounterLayout: LinearLayout
    private lateinit var variationsTable: LinearLayout

    private lateinit var game: SinglePlayerGame
    private lateinit var onLastMoveClicked: () -> Unit

    private var moveViewHeight = -1
//    private var counterViewHeight = -1

//    private val mainMoves = ArrayList<Move>()
//    private var mainLineIndex = 0

    private var maxDepth = 0

    private var selectedLineIndex = 0

    private val lines = ArrayList<OpeningLine>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        typeFace = resources.getFont(R.font.anonymous_bold)

        verticalScrollView = view.findViewById(R.id.vertical_scroll_view)
        moveCounterLayout = view.findViewById(R.id.move_numbers_layout)
        variationsTable = view.findViewById(R.id.variations_table)

        addMainLineTable()
        Thread {
            while (moveViewHeight == -1) {
                Thread.sleep(1)
            }

            requireActivity().runOnUiThread {
                addViewsToCounterLayout()
            }
        }.start()

        Logger.mute("Rewrite")
        Logger.mute("debug")
    }

    fun setGame(game: SinglePlayerGame) {
        this.game = game
    }

    fun setOnLastMoveClicked(onLastMoveClicked: () -> Unit) {
        this.onLastMoveClicked = onLastMoveClicked
    }

    fun addMove(move: Move) {
        deselectAll()
        addMoveToLine(move)

        verticalScrollView.post {
//            verticalScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun addMoveToLine(move: Move) {
        val line = lines[selectedLineIndex]
        Logger.debug("fix", "Going to add move to line: lineId=${line.id}, currentIndex=${line.currentIndex} size=${line.moves.size} ")

        if (line.currentIndex != line.getNumberOfMoves()) {
            removeRemainingMoves()
            onLastMoveClicked()
        }

        line.addMove(move)

        Logger.debug("fix", "Adding move: ${move.getSimpleChessNotation()} to line: lineId=${line.id}, currentIndex=${line.currentIndex}, size=${line.moves.size}")

//        if (move.team == Team.WHITE) {
//            addRowToTable()
//        }

//        val moveView = MoveView(line.id, requireContext(), MoveView.MoveViewData(move, ::onMoveClicked, ::onMoveViewInitialized))

        val moveView = OpeningMoveView(requireContext())

        val lastEmptyRow = getLastEmptyRow()
        if (move.team == Team.WHITE) {
            (lastEmptyRow[0] as OpeningMoveView)
                .setLineId(line.id)
                .setMove(move)
                .setOnClick(::onMoveClicked)
                .show()
        } else {
            (lastEmptyRow[1] as OpeningMoveView)
                .setLineId(line.id)
                .setMove(move)
                .setOnClick(::onMoveClicked)
                .show()
        }

        lastEmptyRow.visibility = View.VISIBLE
        addCounterRow(line.getNumberOfMoves())
        selectMove(moveView)
    }

    fun onBackClicked() {
        deselectAll()

        val line = lines[selectedLineIndex]
        line.currentIndex--

        if (line.currentIndex <= 0) {
            return
        }

        selectMove(line.currentIndex - 1)
    }

    fun onForwardClicked() {
        deselectAll()

        val line = lines[selectedLineIndex]
        line.currentIndex++

        Logger.debug("debug", "Trying to go forward: ${line.id} ${line.currentIndex} ${line.moves.size}")

        selectMove(line.currentIndex - 1)
    }

    private fun onMoveClicked(lineId: Int, move: Move) {
        val line = lines[lineId]
        line.jumpToMove(move)

        if (line.currentIndex == line.getNumberOfMoves()) {
            onLastMoveClicked()
        }

        deselectAll()
        if (lineId == selectedLineIndex) {
            game.goToMove(move)
        } else {
            game.swapMoves(line.moves, move)
        }

        game.clearBoardData()

        Logger.debug("fix", "Clicked move: ${move.getSimpleChessNotation()} in line: lineId=$lineId, currentIndex=${line.currentIndex}, size=${line.moves.size}")
        selectedLineIndex = lineId
    }

    private fun deselectAll() {
        for (lineIndex in 0 until variationsTable.childCount) {
            val line = variationsTable[lineIndex] as LinearLayout
            for (rowIndex in 0 until line.childCount) {
                val row = line[rowIndex] as TableRow
                for (i in 0 until row.childCount) {
                    val moveView = row[i]
                    deselectMove(moveView)
                }
            }
        }
    }

    private fun deselectMove(view: View) {
        val card = view.findViewById<CardView>(R.id.opening_move_card)
        card.setBackgroundColor(Color.TRANSPARENT)

        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
        textView.setTypeface(null, Typeface.NORMAL)
    }

    private fun selectMove(i: Int) {
        val rowIndex = i / 2
        val columnIndex = i % 2
        val lineView = (variationsTable[selectedLineIndex] as LinearLayout)
        if (rowIndex >= lineView.childCount) {
            return
        }

        val row = lineView[rowIndex] as TableRow
        val moveView = row[columnIndex]
        selectMove(moveView)
    }

    private fun selectMove(view: View) {
//        val card = view.findViewById<CardView>(R.id.opening_move_card)
//        card.setBackgroundColor(Color.argb(0.25f, 1.0f, 1.0f, 1.0f))

//        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
//        textView.setTypeface(null, Typeface.BOLD)
    }

    private fun removeRemainingMoves() {
        val line = lines[selectedLineIndex]

        for (i in line.getNumberOfMoves() - 1 downTo line.currentIndex) {
            Logger.debug("fix", "Removing remaining: $i : ${line[i].getSimpleChessNotation()}")
            deleteMove(line[i])
            line.deleteMoveAt(i)
        }
    }

    private fun deleteMove(move: Move) {
        deselectAll()

        val line = lines[selectedLineIndex]
        val moveIndex = line.indexOf(move)
        val rowIndex = moveIndex / 2
        val columnIndex = moveIndex % 2

        try {
            val lineColumn = variationsTable[selectedLineIndex] as LinearLayout
            val row = lineColumn[rowIndex] as TableRow
            row.removeViewAt(columnIndex)

            if (row.childCount == 0) {
                lineColumn.removeViewAt(rowIndex)
                // TODO: Also remove the view from moveCounterLayout, if this was the line with the largest depth




            }

//            (variationsTable[rowIndex] as TableRow).removeViewAt(columnIndex)
//            if ((variationsTable[rowIndex] as TableRow).childCount == 0) {
//                Logger.info("MyTag", "Removing row at: $rowIndex")
//                variationsTable.removeViewAt(rowIndex)
//                moveCounterLayout.removeViewAt(rowIndex)
//            }
        } catch (e: Exception) {
            Logger.error("Rewrite", e.stackTraceToString())
//            throw IllegalArgumentException("Failed to delete move at index: $moveIndex. Number of moves was ${mainMoves.size}")
        }
    }

    private fun onMoveViewInitialized(height: Int) {
//        moveViewHeight = height
//        counterViewHeight = moveCounterLayout[0].height

//        val i = moveCounterLayout.childCount - 1
//        if (moveCounterLayout[i].paddingBottom != 0) {
//            return
//        }

//        val textViewHeight = moveCounterLayout[i].height
//        val topPadding = (height - textViewHeight) / 2
//        val bottomPadding = height - topPadding - textViewHeight
//
//        (moveCounterLayout[i] as TextView).setPadding(0, topPadding, 0, bottomPadding)
    }

    private fun addRowToTable(tableLayout: LinearLayout): TableRow {
        val rowParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val newRow = TableRow(requireContext())
        newRow.layoutParams = rowParams
        newRow.orientation = LinearLayout.HORIZONTAL
        tableLayout.addView(newRow)
        return newRow
    }

    private fun addRowToTable(): TableRow {
        val rowParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val newRow = TableRow(requireContext())
        newRow.layoutParams = rowParams
        newRow.orientation = LinearLayout.HORIZONTAL

        val selectedLineTable = getSelectedLineTable()
        val selectedLine = lines[selectedLineIndex]

        selectedLineTable.addView(newRow)

        val lineDepth = selectedLine.totalDepth()
        if (lineDepth < moveCounterLayout.childCount) {
            moveCounterLayout[lineDepth].visibility = View.VISIBLE
        } else {
//            addCounterRow()
        }
//        if (lineDepth > maxDepth) {
//            maxDepth = lineDepth
//            addCounterRow()
//        }
        return newRow
    }

    private fun addCounterRow(n: Int) {


//        val currentRow = maxDepth
//
//        val moveStringPlaceHolder = requireContext().resources.getString(R.string.move_row_string)
//        val moveString = String.format(moveStringPlaceHolder, currentRow)
//
//        val textView = TextView(requireContext())
//        textView.text = moveString
//        textView.textSize = 20.0f
//        textView.typeface = typeFace
//
//        moveCounterLayout.addView(textView)
    }

    private fun getLastRow(lineTable: LinearLayout): TableRow {
        return lineTable[lineTable.childCount - 1] as TableRow
    }

    private fun getLastRow(): TableRow {
        val selectedTable = getSelectedLineTable()

        return selectedTable[selectedTable.childCount - 1] as TableRow
    }

    private fun getLastEmptyRow() = getLastEmptyRow(getSelectedLineTable())

    private fun getLastEmptyRow(lineTable: LinearLayout): TableRow {
        for (i in 0 until lineTable.childCount) {
            val row = lineTable[i] as TableRow
            if (row.childCount == 0) {
                return row
            }

            val isDefaultView = (row[1] as OpeningMoveView).getText() == "0-0-0@"
            if (isDefaultView) {
                Logger.debug("fix", "getLastEmptyRow(): Reusing row at: $i")
                return row
            }
        }
        Logger.debug("fix", "getLastEmptyRow(): Creating new row")

        return addRowToTable(lineTable)
    }

    private fun getSelectedLineTable(): LinearLayout {
        return variationsTable[selectedLineIndex] as LinearLayout
    }

    fun addVariation() {
        val tableParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val variationNumber = variationsTable.childCount

        val newTable = LinearLayout(requireContext())

//        if (variationNumber % 2 == 1) {
        newTable.setBackgroundColor(Color.GRAY)
//        } else {
//            newTable.setBackgroundColor(Color.DKGRAY)
//        }

        newTable.orientation = VERTICAL
        newTable.layoutParams = tableParams

        val parentLine = lines[selectedLineIndex]
        val newLine = OpeningLine(variationNumber)

        for (i in 0 until parentLine.currentIndex) {
            newLine.addMove(parentLine[i])
        }

        for (i in 0 until 15) {
            addRowToTable(newTable)

            val lastRow = getLastRow(newTable)
            val whiteMove = OpeningMoveView(requireContext())
            whiteMove.setLineId(0)
            whiteMove.hide()

            val blackMove = OpeningMoveView(requireContext())
            blackMove.setLineId(0)
            blackMove.hide()

            lastRow.addView(whiteMove)
            lastRow.addView(blackMove)
        }

        variationsTable.addView(newTable)

        lines += newLine
        selectedLineIndex = variationNumber
    }

    private fun addMainLineTable() {
        val tableParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        val table = LinearLayout(requireContext())
//        table.setBackgroundColor(Color.GRAY)
        table.orientation = VERTICAL
        table.layoutParams = tableParams

        for (i in 0 until 15) {
            addRowToTable(table)
            val lastRow = getLastRow(table)

            val whiteMove = OpeningMoveView(requireContext())
            whiteMove.setLineId(0)
            whiteMove.hide()

            if (i == 0) {
                whiteMove.doOnLayout {
                    moveViewHeight = it.height
                    Logger.debug("fix", "MoveView height: $moveViewHeight")
                }
            }

            val blackMove = OpeningMoveView(requireContext())
            blackMove.setLineId(0)
            blackMove.hide()

            lastRow.addView(whiteMove)
            lastRow.addView(blackMove)
        }

        variationsTable.addView(table)

        lines += OpeningLine(0)
    }

    private fun addViewsToCounterLayout() {
        Logger.debug("fix", "Adding Counter Views")
        for (i in 0 until 20) {
            val moveStringPlaceHolder = requireContext().resources.getString(R.string.move_row_string)
            val moveString = String.format(moveStringPlaceHolder, i)

            val textView = TextView(requireContext())
            textView.visibility = View.INVISIBLE
            textView.gravity = Gravity.CENTER_VERTICAL
            textView.text = moveString
            textView.textSize = 20.0f
            textView.typeface = typeFace
            textView.height = moveViewHeight

            moveCounterLayout.addView(textView)
        }
    }
}