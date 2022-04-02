package com.mjaruijs.fischersplayground.gamedata.pieces

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader

enum class PieceType(val value: Int) {

    WHITE_PAWN(1),
    WHITE_KNIGHT(3),
    WHITE_BISHOP(3),
    WHITE_ROOK(5),
    WHITE_QUEEN(9),
    WHITE_KING(900),
    BLACK_PAWN(1),
    BLACK_KNIGHT(3),
    BLACK_BISHOP(3),
    BLACK_ROOK(5),
    BLACK_QUEEN(9),
    BLACK_KING(900);

    lateinit var texture: Texture

    fun getPossibleMoves() {

    }

    companion object {

        fun init(context: Context) {
            for (pieceType in values()) {
                pieceType.texture = when (pieceType) {
                    WHITE_KING -> TextureLoader.load(context, R.drawable.white_king)
                    WHITE_QUEEN -> TextureLoader.load(context, R.drawable.white_queen)
                    WHITE_ROOK -> TextureLoader.load(context, R.drawable.white_rook)
                    WHITE_KNIGHT -> TextureLoader.load(context, R.drawable.white_knight)
                    WHITE_BISHOP -> TextureLoader.load(context, R.drawable.white_bishop)
                    WHITE_PAWN -> TextureLoader.load(context, R.drawable.white_pawn)
                    BLACK_KING -> TextureLoader.load(context, R.drawable.black_king)
                    BLACK_QUEEN -> TextureLoader.load(context, R.drawable.black_queen)
                    BLACK_ROOK -> TextureLoader.load(context, R.drawable.black_rook)
                    BLACK_KNIGHT -> TextureLoader.load(context, R.drawable.black_knight)
                    BLACK_BISHOP -> TextureLoader.load(context, R.drawable.black_bishop)
                    BLACK_PAWN -> TextureLoader.load(context, R.drawable.black_pawn)
                }
            }
        }
    }

}