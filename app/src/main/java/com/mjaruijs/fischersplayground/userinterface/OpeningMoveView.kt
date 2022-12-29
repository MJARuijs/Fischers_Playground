package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team

class OpeningMoveView(context: Context, attributes: AttributeSet? = null) : LinearLayout(context, attributes) {

    private var iconView: ImageView
    private var moveView: TextView
    private var promotedIconView: ImageView
    private var promotedPieceText: TextView
    private var cardView: CardView

    private var pieceDrawable: Drawable? = null
    private lateinit var move: Move

    init {
        LayoutInflater.from(context).inflate(R.layout.opening_move_layout, this, true)

        iconView = findViewById(R.id.opening_piece_icon)
        moveView = findViewById(R.id.opening_move_notation)
        promotedIconView = findViewById(R.id.promoted_piece_icon)
        promotedPieceText = findViewById(R.id.promoted_piece_text)
        cardView = findViewById(R.id.opening_move_card)
        cardView.setCardBackgroundColor(Color.TRANSPARENT)
    }

    override fun setBackgroundColor(color: Int) {
        cardView.setCardBackgroundColor(color)
    }

    private fun setMove(move: Move): OpeningMoveView {
        this.move = move
        pieceDrawable = getPieceIcon(resources, move.movedPiece, move.team)
        iconView.setImageDrawable(pieceDrawable)

        val chessNotation = move.getSimpleChessNotation().substring(1)

        if (move.promotedPiece == null) {
            moveView.text = chessNotation
            promotedIconView.visibility = View.GONE
            promotedPieceText.visibility = View.GONE
        } else {
            promotedIconView.visibility = View.VISIBLE
            promotedIconView.setImageDrawable(getPieceIcon(resources, move.promotedPiece, move.team))

            if (move.isCheck || move.isCheckMate) {
                val checkSymbol = if (move.isCheckMate) "#" else "+"
                moveView.text = "${move.getSimpleChessNotation().substring(1, chessNotation.length)}="
                promotedPieceText.text = checkSymbol
                promotedPieceText.visibility = View.VISIBLE
            } else {
                moveView.text = "${move.getSimpleChessNotation().substring(1)}="
                promotedPieceText.visibility = View.GONE
            }
        }

        return this
    }

    fun setOnClick(move: Move, onClick: (Move) -> Unit): OpeningMoveView {
        setMove(move)
        cardView.setOnClickListener {
            onClick(move)
        }
        return this
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.INVISIBLE
    }

    fun select() {
        cardView.setCardBackgroundColor(Color.argb(0.5f, 0.75f, 0.75f, 0.75f))
    }

    fun deselect() {
        cardView.setCardBackgroundColor(Color.TRANSPARENT)
    }

    private fun getPieceIcon(resources: Resources, pieceType: PieceType, team: Team): Drawable {
        val resourceId = if (team == Team.WHITE) {
            when (pieceType) {
                PieceType.PAWN -> R.drawable.white_pawn
                PieceType.KNIGHT -> R.drawable.white_knight
                PieceType.BISHOP -> R.drawable.white_bishop
                PieceType.ROOK -> R.drawable.white_rook
                PieceType.QUEEN -> R.drawable.white_queen
                PieceType.KING -> R.drawable.white_king
            }
        } else {
            when (pieceType) {
                PieceType.PAWN -> R.drawable.black_pawn
                PieceType.KNIGHT -> R.drawable.black_knight
                PieceType.BISHOP -> R.drawable.black_bishop
                PieceType.ROOK -> R.drawable.black_rook
                PieceType.QUEEN -> R.drawable.black_queen
                PieceType.KING -> R.drawable.black_king
            }
        }

        return ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Could not find resource with id: $resourceId")
    }

    companion object {
        private const val TAG = "OpeningMoveView"
    }
}