package com.mjaruijs.fischersplayground.opengl.renderer

import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class AnimationData(val pieceType: PieceType, val fromPosition: Vector2, val toPosition: Vector2, val onAnimationFinished: () -> Unit = {})