package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.BoardModel
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType

class BoardRenderer(context: Context) {

    private val model = BoardModel()
    private val boardProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_fragment, ShaderType.FRAGMENT, context)
    )

    fun render(board: Board, aspectRatio: Float) {
        boardProgram.start()
        boardProgram.set("aspectRatio", aspectRatio)
        boardProgram.set("outColor", Color(0.25f, 0.25f, 1.0f, 1.0f))
        boardProgram.set("scale", Vector2(aspectRatio, aspectRatio))
        boardProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            boardProgram.set("possibleSquares[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        model.draw()
        boardProgram.stop()
    }

    fun destroy() {
        model.destroy()
        boardProgram.destroy()
    }

}