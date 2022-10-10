package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team

class MoveView(context: Context, move: Move, var onLayoutChanged: (Int, String) -> Unit = { _, _ -> }) {
//class MoveView(context: Context): View(context) {

    val view = LayoutInflater.from(context).inflate(R.layout.opening_move_layout, null, false)

    init {
        view.addOnLayoutChangeListener { view, i, i2, i3, i4, i5, i6, i7, i8 ->
            onLayoutChanged(view.height, move.getSimpleChessNotation())
        }

        val textView = view.findViewById<TextView>(R.id.opening_move_notation)
        textView.text = move.getSimpleChessNotation().substring(1)

        val pieceIcon = getPieceIcon(context.resources, move.movedPiece, move.team)

        val pieceImage = view.findViewById<ImageView>(R.id.opening_piece_icon)
        pieceImage.setImageDrawable(pieceIcon)
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

        return drawable
    }
}