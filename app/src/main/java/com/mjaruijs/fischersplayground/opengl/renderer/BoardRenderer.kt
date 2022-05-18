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
    private val board2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_2d_fragment, ShaderType.FRAGMENT, context)
    )

    fun render(board: Board, aspectRatio: Float) {
        board2DProgram.start()
        board2DProgram.set("aspectRatio", aspectRatio)
        board2DProgram.set("outColor", Color(0.25f, 0.25f, 1.0f, 1.0f))
        board2DProgram.set("scale", Vector2(aspectRatio, aspectRatio))
        board2DProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
        board2DProgram.set("checkedKingSquare", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            board2DProgram.set("possibleSquares[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        model.draw()
        board2DProgram.stop()
    }

    fun destroy() {
        model.destroy()
        board2DProgram.destroy()
    }

}