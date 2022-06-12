package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector4
import com.mjaruijs.fischersplayground.opengl.Camera

class Board(var requestPossibleMoves: (Vector2) -> Unit = {}) {

    private val possibleSquaresForMove = ArrayList<Vector2>()

    var checkedKingSquare = Vector2(-1, -1)

    var selectedSquare = Vector2(-1, -1)
        private set

    private var camera = Camera()

    var is3D = false

    init {
        for (i in 0 until MAX_NUMBER_OF_POSSIBLE_MOVES) {
            possibleSquaresForMove += Vector2(-1, -1)
        }
    }

    fun updateCamera(camera: Camera) {
        this.camera = camera
    }

    fun getPossibleMoves() = possibleSquaresForMove

    fun isASquareSelected(): Boolean {
        return selectedSquare.x != -1f
    }

    fun deselectSquare() {
        selectedSquare = Vector2(-1, -1)
        clearPossibleMoves()
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

    fun updateSelectedSquare(square: Vector2) {
        if (square == Vector2(-1, -1)) {
            deselectSquare()
        } else {
            selectedSquare = square
            requestPossibleMoves(selectedSquare)
        }
    }

    fun determineSelectedSquare(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Vector2 {
        return if (is3D) {
            getSelectedSquares3D(x, y, displayWidth, displayHeight)
        } else {
            getSelectedSquares2D(x, y, displayWidth, displayHeight)
        }
    }

    private fun getSelectedSquares2D(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Vector2 {
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

    private fun getSelectedSquares3D(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Vector2 {

        val mouseX = (2.0f * x) / displayWidth.toFloat() - 1.0f
        val mouseY = 1.0f - (2.0f * y) / displayHeight.toFloat()

        val clipCoords = Vector4(mouseX, mouseY, -1f, 1f)
        val eyeSpace = camera.projectionMatrix.inverse().dot(clipCoords)
        eyeSpace.z = -1f
        eyeSpace.w = 0f

        val rayDirection = camera.viewMatrix.inverse().dot(eyeSpace).xyz().normal()

        val position = camera.getPosition()
        val t = -position.z / rayDirection.z

        val squareX = position.x + rayDirection.x * t
        val squareY = position.y + rayDirection.y * t

        var selectedX = -1
        var selectedY = -1

        val scaleX = 1.0f / 4.0f
        val scaleY = 1.0f / 4.0f

        for (i in -4 until 4) {
            val minX = scaleX * i
            val maxX = scaleX * (i + 1)

            if (squareX >= minX && squareX < maxX) {
                selectedX = i + 4
                break
            }
        }

        for (i in -4 until 4) {
            val minY = scaleY * i
            val maxY = scaleY * (i + 1)

            if (squareY >= minY && squareY < maxY) {
                selectedY = i + 4
                break
            }
        }

        return Vector2(selectedX, selectedY)
    }

    companion object {
        private const val MAX_NUMBER_OF_POSSIBLE_MOVES = 27
    }

}