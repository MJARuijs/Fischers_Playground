package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.BoardModel
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType

class BoardRenderer(context: Context) {

    private val model2D = BoardModel(false)
    private val model3D = BoardModel(true)

    private val board2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_2d_fragment, ShaderType.FRAGMENT, context)
    )

    private val board3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_3d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_3d_fragment, ShaderType.FRAGMENT, context)
    )

    fun render3D(board: Board, camera: Camera) {
        board3DProgram.start()
        board3DProgram.set("projection", camera.projectionMatrix)
        board3DProgram.set("view", camera.viewMatrix)
        board3DProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
        board3DProgram.set("checkedKingSquare", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            board3DProgram.set("possibleSquares[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        model3D.draw()
        board3DProgram.stop()
    }

    fun render(board: Board, aspectRatio: Float) {
        board2DProgram.start()
        board2DProgram.set("aspectRatio", aspectRatio)
        board2DProgram.set("scale", Vector2(aspectRatio, aspectRatio))
        board2DProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
        board2DProgram.set("checkedKingSquare", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            board2DProgram.set("possibleSquares[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        model2D.draw()
        board2DProgram.stop()
    }

    fun destroy() {
        model2D.destroy()
        model3D.destroy()
        board2DProgram.destroy()
    }

}