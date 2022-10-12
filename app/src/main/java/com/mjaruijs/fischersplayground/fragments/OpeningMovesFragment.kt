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
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.MoveView

class OpeningMovesFragment : Fragment() {

    private lateinit var typeFace: Typeface

    private lateinit var moveNumbersLayout: LinearLayout
    private lateinit var movesLayout: TableLayout

    private lateinit var game: SinglePlayerGame

    private var moveViewHeight = -1
    private var numberViewHeight = -1

    private var mainMoves = ArrayList<Move>()
    private var mainLineIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moveNumbersLayout = view.findViewById(R.id.move_numbers_layout)
        movesLayout = view.findViewById(R.id.moves_table)

        typeFace = resources.getFont(R.font.anonymous_bold)
    }

    fun setGame(game: SinglePlayerGame) {
        this.game = game
    }

    fun addMove(move: Move) {
        deselectAll()
        mainMoves += move
        mainLineIndex++

        if (move.team == Team.WHITE) {
            createRow()
        }

        val moveView = MoveView(requireContext(), move, ::onMoveClicked, ::onMoveViewInitialized)
        (movesLayout[movesLayout.childCount - 1] as TableRow).addView(moveView.view)
        selectMove(moveView.view)
    }

    fun onBackClicked() {
//        val tableRow = (movesLayout[movesLayout.childCount - 1] as TableRow)
//        tableRow.removeViewAt(tableRow.childCount - 1)
//        if (tableRow.childCount == 0) {
//            movesLayout.removeViewAt(movesLayout.childCount - 1)
//        }
    }

    fun onForwardClicked() {

    }

    private fun onMoveClicked(move: Move) {
        game.goToMove(move)
        game.clearBoardData()
        deselectAll()

        val moveIndex = mainMoves.indexOf(move)
        val rowIndex = moveIndex / 2
        val columnIndex = moveIndex % 2

//        selectMove((movesLayout[rowIndex] as TableRow)[columnIndex])
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
//        val card = view.findViewById<CardView>(R.id.opening_move_card)
//        card.setBackgroundColor(Color.TRANSPARENT)

        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
        textView.setTypeface(null, Typeface.NORMAL)
    }

    private fun selectMove(view: View) {
//        val card = view.findViewById<CardView>(R.id.opening_move_card)
//        card.setBackgroundColor(Color.argb(0.25f, 1.0f, 1.0f, 1.0f))
        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
        textView.setTypeface(null, Typeface.BOLD)
    }

    private fun onMoveViewInitialized(height: Int, name: String) {
        moveViewHeight = height
        numberViewHeight = moveNumbersLayout[0].height

        val i = moveNumbersLayout.childCount - 1
        if (moveNumbersLayout[i].paddingBottom != 0) {
            return
        }

        val textViewHeight = moveNumbersLayout[i].height
        val topPadding = (height - textViewHeight) / 2
        val bottomPadding = height - topPadding - textViewHeight

        (moveNumbersLayout[i] as TextView).setPadding(0, topPadding, 0, bottomPadding)
        println("$name Setting padding: $topPadding $bottomPadding $textViewHeight $moveViewHeight")
    }

    private fun createRow() {
        val currentRow = movesLayout.childCount + 1

        val textView = TextView(requireContext())
        textView.text = "$currentRow."
        textView.textSize = 20.0f
        textView.typeface = typeFace

        moveNumbersLayout.addView(textView)

        val row = TableRow(requireContext())
        movesLayout.addView(row)
    }

}