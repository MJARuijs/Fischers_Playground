package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingmovesadapter.OpeningVariation
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.MoveView
import com.mjaruijs.fischersplayground.util.Logger

class OpeningMovesFragment : Fragment() {

    private lateinit var typeFace: Typeface

    private lateinit var moveCounterLayout: LinearLayout
    private lateinit var movesLayout: TableLayout

    private lateinit var game: SinglePlayerGame
    private lateinit var onLastMoveClicked: () -> Unit

    private var moveViewHeight = -1
    private var counterViewHeight = -1

    private val mainMoves = ArrayList<Move>()
    private var mainLineIndex = 0

    private val variations = ArrayList<OpeningVariation>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moveCounterLayout = view.findViewById(R.id.move_numbers_layout)
        movesLayout = view.findViewById(R.id.moves_table)

        typeFace = resources.getFont(R.font.anonymous_bold)
    }

    fun setGame(game: SinglePlayerGame) {
        this.game = game
    }

    fun setOnLastMoveClicked(onLastMoveClicked: () -> Unit) {
        this.onLastMoveClicked = onLastMoveClicked
    }

    fun addMove(move: Move) {
        deselectAll()
        Logger.info("MyTag", "Adding move. Current index: $mainLineIndex. Number of moves: ${mainMoves.size}")

        if (mainLineIndex != mainMoves.size) {
            removeRemainingMoves()
            onLastMoveClicked()
        }

        mainMoves += move
        mainLineIndex++

        if (move.team == Team.WHITE) {
            createRow()
        }

        val moveView = MoveView(requireContext(), move, ::onMoveClicked, ::onMoveViewInitialized)
        (movesLayout[movesLayout.childCount - 1] as TableRow).addView(moveView.view)
        selectMove(moveView.view)
    }

    fun addVariation() {

    }

    fun onBackClicked() {
        mainLineIndex--
        deselectAll()
        if (mainLineIndex <= 0) {
            return
        }
        selectMove(mainLineIndex - 1)
    }

    fun onForwardClicked() {
        mainLineIndex++
        deselectAll()
        selectMove(mainLineIndex - 1)
    }

    private fun onMoveClicked(move: Move) {
        game.goToMove(move)
        game.clearBoardData()
        mainLineIndex = mainMoves.indexOf(move) + 1

        Logger.info("MyTag", "Clicked move: $mainLineIndex")
        deselectAll()

        if (mainLineIndex == mainMoves.size) {
            onLastMoveClicked()
        }
    }

    private fun deselectAll() {
        for (rowIndex in 0 until movesLayout.childCount) {
            val row = movesLayout[rowIndex] as TableRow
            for (i in 0 until row.childCount) {
                deselectMove(row[i])
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
        val view = (movesLayout[rowIndex] as TableRow)[columnIndex]
        selectMove(view)
    }

    private fun selectMove(view: View) {
        val card = view.findViewById<CardView>(R.id.opening_move_card)
        card.setBackgroundColor(Color.argb(0.25f, 1.0f, 1.0f, 1.0f))

        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
        textView.setTypeface(null, Typeface.BOLD)
    }

    private fun removeRemainingMoves() {
        Logger.info("MyTag", "Having to remove remaining moves: ${mainMoves.size - 1}, $mainLineIndex")
        for (i in mainMoves.size - 1 downTo mainLineIndex) {
            Logger.info("MyTag", "Attempting to delete move at index: $i")

            deleteMove(mainMoves[i])
            mainMoves.removeAt(i)
        }
    }

    private fun deleteMove(move: Move) {
        deselectAll()
        val moveIndex = mainMoves.indexOf(move)
        val rowIndex = moveIndex / 2
        val columnIndex = moveIndex % 2

        try {
            (movesLayout[rowIndex] as TableRow).removeViewAt(columnIndex)
            if ((movesLayout[rowIndex] as TableRow).childCount == 0) {
                Logger.info("MyTag", "Removing row at: $rowIndex")
                movesLayout.removeViewAt(rowIndex)
                moveCounterLayout.removeViewAt(rowIndex)
            }
        } catch (e: Exception) {
            Logger.error("MyTag", e.stackTraceToString())
//            throw IllegalArgumentException("Failed to delete move at index: $moveIndex. Number of moves was ${mainMoves.size}")
        }
    }

    private fun onMoveViewInitialized(height: Int, name: String) {
        moveViewHeight = height
        counterViewHeight = moveCounterLayout[0].height

        val i = moveCounterLayout.childCount - 1
        if (moveCounterLayout[i].paddingBottom != 0) {
            return
        }

        val textViewHeight = moveCounterLayout[i].height
        val topPadding = (height - textViewHeight) / 2
        val bottomPadding = height - topPadding - textViewHeight

        (moveCounterLayout[i] as TextView).setPadding(0, topPadding, 0, bottomPadding)
        println("$name Setting padding: $topPadding $bottomPadding $textViewHeight $moveViewHeight")
    }

    private fun createRow() {
        val currentRow = movesLayout.childCount + 1

        val moveStringPlaceHolder = requireContext().resources.getString(R.string.move_row_string)
        val moveString = String.format(moveStringPlaceHolder, currentRow)

        val textView = TextView(requireContext())
        textView.text = moveString
        textView.textSize = 20.0f
        textView.typeface = typeFace

        moveCounterLayout.addView(textView)

        val row = TableRow(requireContext())
        movesLayout.addView(row)
    }

}