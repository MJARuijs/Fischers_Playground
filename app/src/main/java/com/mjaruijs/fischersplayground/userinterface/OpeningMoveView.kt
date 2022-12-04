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
    private var cardView: CardView

    private var lineId = 0
    private var deleteModeActive = false

    private var pieceDrawable: Drawable? = null
    private lateinit var move: Move

    init {
        LayoutInflater.from(context).inflate(R.layout.opening_move_layout, this, true)

        iconView = findViewById(R.id.opening_piece_icon)
        moveView = findViewById(R.id.opening_move_notation)
        cardView = findViewById(R.id.opening_move_card)
        cardView.setCardBackgroundColor(Color.TRANSPARENT)
        cardView.setOnLongClickListener {
//            if (deleteModeActive) {
//                deleteModeActive = false
//                iconView.setImageDrawable(pieceDrawable!!)
//                select()
//            } else {
//                deleteModeActive = true
//                iconView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.delete_icon, null))
//                cardView.setCardBackgroundColor(Color.RED)
//            }
            true
        }
    }

    override fun setBackgroundColor(color: Int) {
        cardView.setCardBackgroundColor(color)
    }

    fun setLineId(lineId: Int): OpeningMoveView {
        this.lineId = lineId
        return this
    }

    fun setMove(move: Move): OpeningMoveView {
        this.move = move
        moveView.text = move.getSimpleChessNotation().substring(1)
        pieceDrawable = getPieceIcon(resources, move.movedPiece, move.team)
        iconView.setImageDrawable(pieceDrawable)
        return this
    }

    fun setOnClick(move: Move, onClick: (Move, Boolean) -> Unit): OpeningMoveView {
        setMove(move)
        cardView.setOnClickListener {
//            if (this::move.isInitialized) {
                onClick(move, deleteModeActive)
//            }
        }
        return this
    }

    fun getText(): String {
        return moveView.text.toString()
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

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Could not find resource with id: $resourceId")
//        val bitmap = drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ALPHA_8)
//        val outlineBitmap = drawable.toBitmap(drawable.intrinsicWidth + pieceOffset * 2, drawable.intrinsicHeight + pieceOffset * 2, Bitmap.Config.ALPHA_8)

        return drawable
    }
}