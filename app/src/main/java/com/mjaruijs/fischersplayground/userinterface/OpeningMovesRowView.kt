package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move

class OpeningMovesRowView(context: Context, attributes: AttributeSet? = null) : LinearLayout(context, attributes) {

    private val layout: ConstraintLayout
    private val moveCounterView: TextView
    private val whiteMoveView: OpeningMoveView
    private val blackMoveView: OpeningMoveView

    private var moveNumber = -1

    init {
        LayoutInflater.from(context).inflate(R.layout.opening_moves_row_layout, this, true)

        layout = findViewById(R.id.layout)
        moveCounterView = findViewById(R.id.move_number_view)
        whiteMoveView = findViewById(R.id.white_move_view)
        blackMoveView = findViewById(R.id.black_move_view)

        moveCounterView.doOnLayout {
            moveCounterView.minimumWidth = moveCounterView.width
            moveCounterView.text = "$moveNumber."
        }

        whiteMoveView.visibility = View.INVISIBLE
        blackMoveView.visibility = View.INVISIBLE
    }

    fun setTypeFace(typeface: Typeface) {
        moveCounterView.typeface = typeface
    }

    fun setMoveNumber(n: Int) {
        moveNumber = n
    }

    fun setWhiteMove(move: Move, onClick: (Move, Boolean) -> Unit) {
        whiteMoveView.setOnClick(move, onClick)
        whiteMoveView.visibility = View.VISIBLE
        selectWhiteMove()
    }

    fun setBlackMove(move: Move, onClick: (Move, Boolean) -> Unit) {
        blackMoveView.setOnClick(move, onClick)
        blackMoveView.visibility = View.VISIBLE
        selectBlackMove()
    }

    fun selectWhiteMove() {
        whiteMoveView.select()
    }

    fun selectBlackMove() {
        blackMoveView.select()
    }

    fun deselectWhiteMove() {
        whiteMoveView.deselect()
    }

    fun deselectBlackMove() {
        blackMoveView.deselect()
    }

    fun areBothMovesHidden(): Boolean {
        return blackMoveView.visibility == View.INVISIBLE && whiteMoveView.visibility == View.INVISIBLE
    }

    fun isBlackMoveHidden(): Boolean {
        return blackMoveView.visibility == View.INVISIBLE
    }

    fun hideBlackMove() {
        blackMoveView.visibility = View.INVISIBLE
    }

    override fun setBackgroundColor(color: Int) {
        layout.setBackgroundColor(color)
    }

}