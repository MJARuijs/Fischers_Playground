package com.mjaruijs.fischersplayground.opengl.texture

import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team

class PieceTextures(resources: Resources) {

    private val pieceTextures2D = ArrayList<Triple<PieceType, Team, Texture>>()
    private val pieceTextures3D = ArrayList<Pair<PieceType, Texture>>()
    private var textureArray2D: TextureArray? = null
    private var textureArray3D: TextureArray? = null

    init {
        val textureLoader = TextureLoader.getInstance()

        pieceTextures2D += Triple(PieceType.PAWN, Team.WHITE, textureLoader.get(resources, R.drawable.white_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.WHITE, textureLoader.get(resources, R.drawable.white_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.WHITE, textureLoader.get(resources, R.drawable.white_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.WHITE, textureLoader.get(resources, R.drawable.white_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.WHITE, textureLoader.get(resources, R.drawable.white_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.WHITE, textureLoader.get(resources, R.drawable.white_queen))
        pieceTextures2D += Triple(PieceType.PAWN, Team.BLACK, textureLoader.get(resources, R.drawable.black_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.BLACK, textureLoader.get(resources, R.drawable.black_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.BLACK, textureLoader.get(resources, R.drawable.black_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.BLACK, textureLoader.get(resources, R.drawable.black_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.BLACK, textureLoader.get(resources, R.drawable.black_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.BLACK, textureLoader.get(resources, R.drawable.black_queen))

        pieceTextures3D += Pair(PieceType.PAWN, textureLoader.get(resources, R.drawable.diffuse_map_pawn))
        pieceTextures3D += Pair(PieceType.KNIGHT, textureLoader.get(resources, R.drawable.diffuse_map_knight))
        pieceTextures3D += Pair(PieceType.BISHOP, textureLoader.get(resources, R.drawable.diffuse_map_bishop))
        pieceTextures3D += Pair(PieceType.ROOK, textureLoader.get(resources, R.drawable.diffuse_map_rook))
        pieceTextures3D += Pair(PieceType.QUEEN, textureLoader.get(resources, R.drawable.diffuse_map_queen))
        pieceTextures3D += Pair(PieceType.KING, textureLoader.get(resources, R.drawable.diffuse_map_king))

        createTextureArrays()
    }

    private fun createTextureArrays() {
        textureArray2D = TextureArray(pieceTextures2D.map { pieceTexture -> pieceTexture.third })
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

    fun destroy() {
        for (texture in pieceTextures2D) {
//            texture.third.destroy()
        }
        for (texture in pieceTextures3D) {
//            texture.second.destroy()
        }
        textureArray2D?.destroy()
        textureArray3D?.destroy()
    }
}