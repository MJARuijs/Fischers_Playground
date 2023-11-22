package com.mjaruijs.fischersplayground.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.TakenPiecesView

class PlayerCardFragment : Fragment() {

    private lateinit var statusIcon: ImageView
    private lateinit var takenPieceView: TakenPiecesView
    private lateinit var name: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.player_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        name = requireArguments().getString("player_name") ?: "default_name"
        val hideStatusIcon = requireArguments().getBoolean("hide_status_icon")
        val teamValue = requireArguments().getString("team") ?: throw IllegalArgumentException("Missing essential data in PlayerFragment: team")

        val team = Team.fromString(teamValue)

        takenPieceView = view.findViewById(R.id.taken_pieces_view)
        takenPieceView.init(team)

        val playerNameView = view.findViewById<TextView>(R.id.player_card_name)
        if (playerNameView != null) {
            playerNameView.text = name
        }

        statusIcon = view.findViewById(R.id.status_icon)

        if (hideStatusIcon) {
            statusIcon.visibility = View.INVISIBLE
        }
    }

    fun addTakenPiece(pieceType: PieceType) {
        Log.i(TAG, "Adding $pieceType to $name")
        takenPieceView.add(pieceType)
    }

    fun removeTakenPiece(pieceType: PieceType) {
        Log.i(TAG, "Removing $pieceType from $name")

        takenPieceView.removeTakenPiece(pieceType)
    }

    fun removeAllPieces() {
        Log.i(TAG, "Removing everything from $name")

        takenPieceView.removeAllPieces()
    }

    fun setStatusIcon(status: PlayerStatus) {
        if (this::statusIcon.isInitialized) {
            statusIcon.foregroundTintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(status.color))
        }
    }

    companion object {
        private const val TAG = "PlayerCardFragment"
    }

}