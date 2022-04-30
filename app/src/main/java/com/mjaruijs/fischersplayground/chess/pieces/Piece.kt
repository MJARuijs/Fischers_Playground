package com.mjaruijs.fischersplayground.chess.pieces

class Piece(val type: PieceType, val team: Team) {

    val textureId = PieceTextures.getTextureId(type, team)

}