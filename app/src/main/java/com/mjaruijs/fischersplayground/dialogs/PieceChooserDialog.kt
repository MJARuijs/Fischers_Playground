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
    private lateinit var knightButton: UIButton
    private lateinit var bishopButton: UIButton
    private lateinit var rookButton: UIButton
    private lateinit var queenButton: UIButton

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
            knightButton = dialog.findViewById(R.id.knight_button)
            knightButton
                .setTexturedDrawable(R.drawable.white_knight)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.KNIGHT, team)
                }

            bishopButton = dialog.findViewById(R.id.bishop_button)
            bishopButton
                .setTexturedDrawable(R.drawable.white_bishop)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.BISHOP, team)
                }

            rookButton = dialog.findViewById(R.id.rook_button)
            rookButton
                .setTexturedDrawable(R.drawable.white_rook)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.ROOK, team)
                }

            queenButton = dialog.findViewById(R.id.queen_button)
            queenButton
                .setTexturedDrawable(R.drawable.white_queen)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        } else {
            knightButton = dialog.findViewById(R.id.knight_button)
            knightButton
                .setTexturedDrawable(R.drawable.black_knight)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            bishopButton = dialog.findViewById(R.id.bishop_button)
            bishopButton
                .setTexturedDrawable(R.drawable.black_bishop)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            rookButton = dialog.findViewById(R.id.rook_button)
            rookButton
                .setTexturedDrawable(R.drawable.black_rook)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }

            queenButton = dialog.findViewById(R.id.queen_button)
            queenButton
                .setTexturedDrawable(R.drawable.black_queen)
                .setColor(Color.rgb(235, 186, 145))
                .setCornerRadius(20f)
                .setOnClick {
                    dialog.dismiss()
                    onPieceSelected(square, PieceType.QUEEN, team)
                }
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun destroy() {
        dismiss()

        if (this::knightButton.isInitialized) {
            knightButton.destroy()
        }

        if (this::bishopButton.isInitialized) {
            bishopButton.destroy()
        }

        if (this::rookButton.isInitialized) {
            rookButton.destroy()
        }

        if (this::queenButton.isInitialized) {
            queenButton.destroy()
        }
    }

}