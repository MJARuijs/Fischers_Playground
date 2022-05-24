package com.mjaruijs.fischersplayground.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.TakenPiecesView

class PlayerCardFragment : Fragment(R.layout.player_card) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = requireArguments().getString("player_name")
        val playerNameView = view.findViewById<TextView>(R.id.player_card_name)
        if (playerNameView != null) {
            playerNameView.text = name
        }
    }

    fun addTakenPiece(pieceType: PieceType, team: Team) {
        val takenPieceView = view?.findViewById<TakenPiecesView>(R.id.taken_pieces_view) ?: throw IllegalArgumentException("No view was found with id: taken_pieces_view")
        takenPieceView.add(pieceType, team)
    }
}