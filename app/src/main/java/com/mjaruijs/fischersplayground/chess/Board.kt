package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.math.vectors.Vector2

class Board(private val requestPossibleMoves: (Vector2) -> Unit) {

    val possibleSquaresForMove = ArrayList<Vector2>()

    var selectedSquare = Vector2(-1f, -1f)
        private set

    init {
        for (i in 0 until MAX_NUMBER_OF_POSSIBLE_MOVES) {
            possibleSquaresForMove += Vector2(-1, -1)
        }
    }

    fun updatePossibleMoves(possibleMoves: ArrayList<Vector2>) {
        clearPossibleMoves()

        for (i in 0 until possibleMoves.size) {
            possibleSquaresForMove[i] = possibleMoves[i]
        }
    }

    private fun clearPossibleMoves() {
        for (i in 0 until MAX_NUMBER_OF_POSSIBLE_MOVES) {
            possibleSquaresForMove[i] = Vector2(-1f, -1f)
        }
    }

    fun processAction(action: Action) {
        when (action.type) {
            ActionType.SQUARE_SELECTED -> {
                selectedSquare = action.clickedPosition
                requestPossibleMoves(selectedSquare)
            }
            ActionType.SQUARE_DESELECTED -> {
                selectedSquare = Vector2(-1f, -1f)
                clearPossibleMoves()
            }
            ActionType.PIECE_MOVED -> {
                clearPossibleMoves()
                selectedSquare = Vector2(-1f, -1f)
            }
            ActionType.NO_OP -> {}
        }
    }

    fun onClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Action {
        val selection = determineSelectedSquare(x, y, displayWidth, displayHeight)

        if (selection == Vector2(-1f, -1f)) {
            return Action(selection, ActionType.SQUARE_DESELECTED, selectedSquare)
        }

        return Action(selection, ActionType.SQUARE_SELECTED, selectedSquare)
    }

    private fun determineSelectedSquare(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Vector2 {
        val scaledX = x / displayWidth
        val scaledY = (y / displayHeight) * 2.0f - 1.0f

        val aspectRatio = displayWidth.toFloat() / displayHeight.toFloat()

        val scaleX = 1.0f / 8.0f
        val scaleY = aspectRatio / 4.0f
        var selectedX = -1
        var selectedY = -1

        for (i in 0 until 8) {
            val minX = scaleX * i
            val maxX = scaleX * (i + 1)

            if (scaledX >= minX && scaledX < maxX) {
                selectedX = i
                break
            }
        }

        for (i in -4 until 4) {
            val minY = scaleY * i
            val maxY = scaleY * (i + 1)

            if (scaledY >= minY && scaledY < maxY) {
                selectedY = (i - 3) * -1
                break
            }
        }

        return if (selectedX != -1 && selectedY != -1) {
            Vector2(selectedX, selectedY)
        } else {
            Vector2(-1f, -1f)
        }
    }

    companion object {
        private const val MAX_NUMBER_OF_POSSIBLE_MOVES = 27
    }

}