package com.mjaruijs.fischersplayground.adapters.openingmovesadapter

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team

class OpeningMovesAdapter(private val resources: Resources, private val moves: ArrayList<OpeningMove> = arrayListOf()) : RecyclerView.Adapter<OpeningMovesAdapter.MoveViewHolder>() {

    operator fun plusAssign(move: OpeningMove) {
        moves += move
//        notifyItemInserted(moves.size)
        Log.d("MoveAdapter", "Move added: ${move.getSimpleNotation()}")
        notifyDataSetChanged()
    }

    override fun getItemCount() = moves.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        return MoveViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.opening_move_layout, parent, false))
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        val openingMove = moves[position]
        val move = openingMove.move
        val drawable = getPieceIcon(move.movedPiece, move.team)

        holder.pieceIcon.setImageDrawable(drawable)
        holder.moveTextView.text = openingMove.getSimpleNotation().substring(1)
    }

    private fun getPieceIcon(pieceType: PieceType, team: Team): Drawable {
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

    inner class MoveViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val pieceIcon: ImageView = view.findViewById(R.id.opening_piece_icon)
        val moveTextView: TextView = view.findViewById(R.id.opening_move_notation)
    }

}