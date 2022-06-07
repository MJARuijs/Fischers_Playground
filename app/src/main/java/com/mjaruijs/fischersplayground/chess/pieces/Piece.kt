package com.mjaruijs.fischersplayground.chess.pieces

class Piece(val type: PieceType, val team: Team) {

    val textureId2D = PieceTextures.get2DTextureId(type, team)

    val textureId3D = PieceTextures.get3DTextureId(type)

}