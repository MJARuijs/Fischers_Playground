package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2

class Piece(val type: PieceType, val team: Team, val boardPosition: Vector2) {

//    var shouldAnimate = false

//    var newSquare: Vector2? = null
    var onAnimationFinish: () -> Unit = {}

//    var translation = Vector2()

    var animatedPosition = Vector2(boardPosition)

    fun getBoardX() = boardPosition.x

    fun getBoardY() = boardPosition.y

    fun getAnimatedX() = animatedPosition.x

    fun getAnimatedY() = animatedPosition.y

    fun move(newSquare: Vector2, onAnimationFinish: () -> Unit) {
//        shouldAnimate = true
        println("Moving $type $team from $animatedPosition to $newSquare")

        boardPosition.x = newSquare.x
        boardPosition.y = newSquare.y


//        this.newSquare = newSquare
        this.onAnimationFinish = onAnimationFinish
    }

    fun setPosition(newSquare: Vector2) {
        boardPosition.x = newSquare.x
        boardPosition.y = newSquare.y
    }

}