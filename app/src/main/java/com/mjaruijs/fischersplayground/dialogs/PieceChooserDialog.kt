package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class PieceChooserDialog(private val onPieceSelected: (Vector2, PieceType, Team) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var knightButton: UIButton2
    private lateinit var bishopButton: UIButton2
    private lateinit var rookButton: UIButton2
    private lateinit var queenButton: UIButton2

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_piece_picker)
        dialog.show()
        dialog.dismiss()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setLayout() {
        dialog.window?.apply {
            setLayout(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    fun show(square: Vector2, team: Team) {
        if (team == Team.WHITE) {
            knightButton = dialog.findViewById(R.id.knight_button)
            knightButton
                .setIcon(R.drawable.white_knight)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.KNIGHT, team)
                }

            bishopButton = dialog.findViewById(R.id.bishop_button)
            bishopButton
                .setIcon(R.drawable.white_bishop)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.BISHOP, team)
                }

            rookButton = dialog.findViewById(R.id.rook_button)
            rookButton
                .setIcon(R.drawable.white_rook)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.ROOK, team)
                }

            queenButton = dialog.findViewById(R.id.queen_button)
            queenButton
                .setIcon(R.drawable.white_queen)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        } else {
            knightButton = dialog.findViewById(R.id.knight_button)
            knightButton
                .setIcon(R.drawable.black_knight)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.KNIGHT, team)
                }

            bishopButton = dialog.findViewById(R.id.bishop_button)
            bishopButton
                .setIcon(R.drawable.black_bishop)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.BISHOP, team)
                }

            rookButton = dialog.findViewById(R.id.rook_button)
            rookButton
                .setIcon(R.drawable.black_rook)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.ROOK, team)
                }

            queenButton = dialog.findViewById(R.id.queen_button)
            queenButton
                .setIcon(R.drawable.black_queen)
                .setColorResource(R.color.accent_color)
                .setCornerRadius(CORNER_RADIUS)
                .setIconPadding(ICON_PADDING, ICON_PADDING, ICON_PADDING, ICON_PADDING)
                .setOnClickListener {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    companion object {

        private const val TAG = "PieceChooserDialog"
        private const val CORNER_RADIUS = 45f
        private const val ICON_PADDING = 16

    }

}