package com.mjaruijs.fischersplayground.gamedata

import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class Board(private val boardProgram: ShaderProgram) {

    private val model = BoardModel()

    private var selectedSquare = Vector2(-1f, -1f)

    fun processAction(action: Action) {
        if (action.type == ActionType.SQUARE_SELECTED) {
            selectedSquare = action.position
        } else if (action.type == ActionType.SQUARE_DESELECTED) {
            selectedSquare = Vector2(-1f, -1f)
        }
    }

    fun render(aspectRatio: Float) {
        boardProgram.start()
        boardProgram.set("aspectRatio", aspectRatio)
        boardProgram.set("outColor", Color(0.25f, 0.25f, 1.0f, 1.0f))
        boardProgram.set("scale", Vector2(aspectRatio, aspectRatio))
        boardProgram.set("selectedSquareCoordinates", (selectedSquare / 8.0f) * 2.0f - 1.0f)
        model.draw()
        boardProgram.stop()
    }

    fun onClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int): Action {
//        if (selectedSquare == Vector2(-1f, -1f)) {
//            val selection = determineSelectedSquare(x, y, displayWidth, displayHeight)
//
//            if (selection == Vector2(-1f, -1f)) {
//                return Action(selection, ActionType.NOOP)
//            }
//
//            return Action(selection, ActionType.SQUARE_SELECTED)
//        } else {
            val selection = determineSelectedSquare(x, y, displayWidth, displayHeight)

            if (selection == Vector2(-1f, -1f)) {
                return Action(selection, ActionType.SQUARE_DESELECTED)
            }

            return Action(selection, ActionType.SQUARE_SELECTED)
//        }
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

    fun destroy() {
        model.destroy()
    }

}