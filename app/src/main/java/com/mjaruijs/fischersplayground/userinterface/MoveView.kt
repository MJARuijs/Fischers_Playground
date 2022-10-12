package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team

class MoveView(context: Context, move: Move, onClick: (Move) -> Unit, onLayoutChanged: (Int, String) -> Unit = { _, _ -> }) {

    val view: View = LayoutInflater.from(context).inflate(R.layout.opening_move_layout, null, false)
    val textView: TextView = view.findViewById(R.id.opening_move_notation)

    private val openingMoveCard: CardView = view.findViewById(R.id.opening_move_card)

    init {
        if (move.team == Team.WHITE) {
            view.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                onLayoutChanged(view.height, move.getSimpleChessNotation())
            }
        }

        textView.text = move.getSimpleChessNotation().substring(1)

        val pieceIcon = getPieceIcon(context.resources, move.movedPiece, move.team)

        val pieceImage = view.findViewById<ImageView>(R.id.opening_piece_icon)
        pieceImage.setImageDrawable(pieceIcon)

        openingMoveCard.setOnClickListener {
            onClick(move)
            select()
        }

        select()
    }

    fun select() {
//        Log.d("MyTag","Select")
//        openingMoveCard.setBackgroundColor(Color.argb(0.25f, 1.0f, 1.0f, 1.0f))
        textView.setTypeface(null, Typeface.BOLD)
        openingMoveCard.invalidate()
    }

    fun deselect() {
        openingMoveCard.setBackgroundColor(Color.TRANSPARENT)
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