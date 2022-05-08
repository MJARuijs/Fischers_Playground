package com.mjaruijs.fischersplayground.chess.pieces

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import com.mjaruijs.fischersplayground.opengl.texture.TextureArray
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader

object PieceTextures {

    private val pieceTextures = ArrayList<Triple<PieceType, Team, Texture>>()
    private var textureArray: TextureArray? = null

    fun init(context: Context) {
        pieceTextures += Triple(PieceType.PAWN, Team.WHITE, TextureLoader.load(context, R.drawable.white_pawn))
        pieceTextures += Triple(PieceType.KNIGHT, Team.WHITE, TextureLoader.load(context, R.drawable.white_knight))
        pieceTextures += Triple(PieceType.BISHOP, Team.WHITE, TextureLoader.load(context, R.drawable.white_bishop))
        pieceTextures += Triple(PieceType.ROOK, Team.WHITE, TextureLoader.load(context, R.drawable.white_rook))
        pieceTextures += Triple(PieceType.KING, Team.WHITE, TextureLoader.load(context, R.drawable.white_king))
        pieceTextures += Triple(PieceType.QUEEN, Team.WHITE, TextureLoader.load(context, R.drawable.white_queen))
        pieceTextures += Triple(PieceType.PAWN, Team.BLACK, TextureLoader.load(context, R.drawable.black_pawn))
        pieceTextures += Triple(PieceType.KNIGHT, Team.BLACK, TextureLoader.load(context, R.drawable.black_knight))
        pieceTextures += Triple(PieceType.BISHOP, Team.BLACK, TextureLoader.load(context, R.drawable.black_bishop))
        pieceTextures += Triple(PieceType.ROOK, Team.BLACK, TextureLoader.load(context, R.drawable.black_rook))
        pieceTextures += Triple(PieceType.KING, Team.BLACK, TextureLoader.load(context, R.drawable.black_king))
        pieceTextures += Triple(PieceType.QUEEN, Team.BLACK, TextureLoader.load(context, R.drawable.black_queen))
    }

    fun createTextureArray() {
        for (texture in pieceTextures) {
            texture.third.init()
        }

        textureArray = TextureArray(pieceTextures.map { pieceTexture -> pieceTexture.third })
    }

    fun getTextureArray() = textureArray!!

    fun getTextureId(pieceType: PieceType, team: Team): Int {
        for ((i, pieceTexture) in pieceTextures.withIndex()) {
            if (pieceTexture.first == pieceType && pieceTexture.second == team) {
                return i
            }
        }

        throw IllegalArgumentException("Texture not found for piece: $pieceType, team: $team")
    }

}