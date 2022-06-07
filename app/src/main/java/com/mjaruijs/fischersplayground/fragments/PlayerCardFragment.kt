package com.mjaruijs.fischersplayground.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.TakenPiecesView

class PlayerCardFragment : Fragment(R.layout.player_card) {

    private lateinit var statusIcon: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = requireArguments().getString("player_name")
        val hideStatusIcon = requireArguments().getBoolean("hide_status_icon")

        val playerNameView = view.findViewById<TextView>(R.id.player_card_name)
        if (playerNameView != null) {
            playerNameView.text = name
        }

        statusIcon = view.findViewById(R.id.status_icon)

        if (hideStatusIcon) {
            statusIcon.visibility = View.INVISIBLE
        }
    }

    fun addTakenPiece(pieceType: PieceType, team: Team) {
        val takenPieceView = view?.findViewById<TakenPiecesView>(R.id.taken_pieces_view) ?: throw IllegalArgumentException("No view was found with id: taken_pieces_view")
        takenPieceView.add(pieceType, team)
    }

    fun setStatusIcon(status: PlayerStatus) {
        println("SETTING STATUS $status")
        if (this::statusIcon.isInitialized) {
            statusIcon.foregroundTintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(status.color))
        }
    }

}