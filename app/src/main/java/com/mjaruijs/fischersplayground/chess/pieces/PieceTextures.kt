package com.mjaruijs.fischersplayground.chess.pieces

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import com.mjaruijs.fischersplayground.opengl.texture.TextureArray
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader

object PieceTextures {

    private val pieceTextures2D = ArrayList<Triple<PieceType, Team, Texture>>()
    private val pieceTextures3D = ArrayList<Pair<PieceType, Texture>>()
    private var textureArray2D: TextureArray? = null
    private var textureArray3D: TextureArray? = null

    fun init(context: Context) {
        pieceTextures2D += Triple(PieceType.PAWN, Team.WHITE, TextureLoader.load(context, R.drawable.white_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.WHITE, TextureLoader.load(context, R.drawable.white_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.WHITE, TextureLoader.load(context, R.drawable.white_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.WHITE, TextureLoader.load(context, R.drawable.white_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.WHITE, TextureLoader.load(context, R.drawable.white_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.WHITE, TextureLoader.load(context, R.drawable.white_queen))
        pieceTextures2D += Triple(PieceType.PAWN, Team.BLACK, TextureLoader.load(context, R.drawable.black_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.BLACK, TextureLoader.load(context, R.drawable.black_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.BLACK, TextureLoader.load(context, R.drawable.black_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.BLACK, TextureLoader.load(context, R.drawable.black_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.BLACK, TextureLoader.load(context, R.drawable.black_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.BLACK, TextureLoader.load(context, R.drawable.black_queen))

        pieceTextures3D += Pair(PieceType.PAWN, TextureLoader.load(context, R.drawable.pawn_texture))
        pieceTextures3D += Pair(PieceType.KNIGHT, TextureLoader.load(context, R.drawable.knight_texture))
        pieceTextures3D += Pair(PieceType.BISHOP, TextureLoader.load(context, R.drawable.bishop_texture))
        pieceTextures3D += Pair(PieceType.ROOK, TextureLoader.load(context, R.drawable.rook_texture))
        pieceTextures3D += Pair(PieceType.QUEEN, TextureLoader.load(context, R.drawable.queen_texture))
        pieceTextures3D += Pair(PieceType.KING, TextureLoader.load(context, R.drawable.king_texture))


    }

    fun createTextureArrays() {
        for (texture in pieceTextures2D) {
            texture.third.init()
        }

        textureArray2D = TextureArray(pieceTextures2D.map { pieceTexture -> pieceTexture.third })

        for (texture in pieceTextures3D) {
            texture.second.init()
        }

        textureArray3D = TextureArray(pieceTextures3D.map { pieceTextures -> pieceTextures.second })
    }

    fun get2DTextureArray() = textureArray2D!!

    fun get3DTextureArray() = textureArray3D!!

    fun get2DTextureId(pieceType: PieceType, team: Team): Int {
        for ((i, pieceTexture) in pieceTextures2D.withIndex()) {
            if (pieceTexture.first == pieceType && pieceTexture.second == team) {
                return i
            }
        }

        throw IllegalArgumentException("2D Texture not found for piece: $pieceType, team: $team")
    }

    fun get3DTextureId(pieceType: PieceType): Int {
        for ((i, pieceTexture) in pieceTextures3D.withIndex()) {
            if (pieceTexture.first == pieceType) {
                return i
            }
        }

        throw IllegalArgumentException("3D Texture not found for piece: $pieceType")
    }
}