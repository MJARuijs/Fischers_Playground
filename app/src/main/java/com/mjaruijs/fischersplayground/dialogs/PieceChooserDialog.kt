package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PieceChooserDialog(private val onPieceSelected: (Vector2, PieceType, Team) -> Unit) {

    private lateinit var dialog: Dialog

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.piece_picker_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
    }

    fun setLayout() {
        dialog.window?.apply {
            setLayout(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    fun show(square: Vector2, team: Team) {
        if (team == Team.WHITE) {
            dialog.findViewById<UIButton>(R.id.knight_button)
                .setTexturedDrawable(R.drawable.white_knight)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.KNIGHT, team)
                }

            dialog.findViewById<UIButton>(R.id.bishop_button)
                .setTexturedDrawable(R.drawable.white_bishop)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.BISHOP, team)
                }

            dialog.findViewById<UIButton>(R.id.rook_button)
                .setTexturedDrawable(R.drawable.white_rook)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.ROOK, team)
                }

            dialog.findViewById<UIButton>(R.id.queen_button)
                .setTexturedDrawable(R.drawable.white_queen)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        } else {
            dialog.findViewById<UIButton>(R.id.knight_button)
                .setTexturedDrawable(R.drawable.black_knight)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            dialog.findViewById<UIButton>(R.id.bishop_button)
                .setTexturedDrawable(R.drawable.black_bishop)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            dialog.findViewById<UIButton>(R.id.rook_button)
                .setTexturedDrawable(R.drawable.black_rook)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            dialog.findViewById<UIButton>(R.id.queen_button)
                .setTexturedDrawable(R.drawable.black_queen)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        }

        dialog.show()
    }

}