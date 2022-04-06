package com.mjaruijs.fischersplayground.gamedata.pieces

import com.mjaruijs.fischersplayground.opengl.Quad

class Piece(val type: PieceType, val team: Team) {

    private val quad = Quad()

    val textureId = PieceTextures.getTextureId(type, team)

    fun destroy() {
        quad.destroy()
    }

}