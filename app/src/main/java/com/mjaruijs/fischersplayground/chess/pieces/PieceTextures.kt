package com.mjaruijs.fischersplayground.chess.pieces

import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import com.mjaruijs.fischersplayground.opengl.texture.TextureArray
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader
import java.util.concurrent.atomic.AtomicBoolean

class PieceTextures(resources: Resources) {

    private val pieceTextures2D = ArrayList<Triple<PieceType, Team, Texture>>()
    private val pieceTextures3D = ArrayList<Pair<PieceType, Texture>>()
    private var textureArray2D: TextureArray? = null
    private var textureArray3D: TextureArray? = null

//    private val initialized = AtomicBoolean(false)

    init {
//        if (initialized.get()) {
//            return
//        }

        pieceTextures2D += Triple(PieceType.PAWN, Team.WHITE, TextureLoader.load(resources, R.drawable.white_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.WHITE, TextureLoader.load(resources, R.drawable.white_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.WHITE, TextureLoader.load(resources, R.drawable.white_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.WHITE, TextureLoader.load(resources, R.drawable.white_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.WHITE, TextureLoader.load(resources, R.drawable.white_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.WHITE, TextureLoader.load(resources, R.drawable.white_queen))
        pieceTextures2D += Triple(PieceType.PAWN, Team.BLACK, TextureLoader.load(resources, R.drawable.black_pawn))
        pieceTextures2D += Triple(PieceType.KNIGHT, Team.BLACK, TextureLoader.load(resources, R.drawable.black_knight))
        pieceTextures2D += Triple(PieceType.BISHOP, Team.BLACK, TextureLoader.load(resources, R.drawable.black_bishop))
        pieceTextures2D += Triple(PieceType.ROOK, Team.BLACK, TextureLoader.load(resources, R.drawable.black_rook))
        pieceTextures2D += Triple(PieceType.KING, Team.BLACK, TextureLoader.load(resources, R.drawable.black_king))
        pieceTextures2D += Triple(PieceType.QUEEN, Team.BLACK, TextureLoader.load(resources, R.drawable.black_queen))

        pieceTextures3D += Pair(PieceType.PAWN, TextureLoader.load(resources, R.drawable.diffuse_map_pawn))
        pieceTextures3D += Pair(PieceType.KNIGHT, TextureLoader.load(resources, R.drawable.diffuse_map_knight))
        pieceTextures3D += Pair(PieceType.BISHOP, TextureLoader.load(resources, R.drawable.diffuse_map_bishop))
        pieceTextures3D += Pair(PieceType.ROOK, TextureLoader.load(resources, R.drawable.diffuse_map_rook))
        pieceTextures3D += Pair(PieceType.QUEEN, TextureLoader.load(resources, R.drawable.diffuse_map_queen))
        pieceTextures3D += Pair(PieceType.KING, TextureLoader.load(resources, R.drawable.diffuse_map_king))
//        initialized.set(true)

        createTextureArrays()
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

    fun destroy() {
        for (texture in pieceTextures2D) {
            texture.third.destroy()
        }
        for (texture in pieceTextures3D) {
            texture.second.destroy()
        }
        textureArray2D?.destroy()
        textureArray3D?.destroy()
    }
}