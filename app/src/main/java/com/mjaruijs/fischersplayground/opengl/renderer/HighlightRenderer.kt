package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType

class HighlightRenderer(context: Context) {

    private val quad = Quad()

    private val highlight2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.possible_square_highlighter_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.possible_square_highlighter_2d_fragment, ShaderType.FRAGMENT, context)
    )

    private val selectedSquare2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.selected_square_highlighter_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.selected_square_highlighter_2d_fragment, ShaderType.FRAGMENT, context)
    )

    private val selectedSquare3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.selected_square_highlighter_3d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.selected_square_highlighter_3d_fragment, ShaderType.FRAGMENT, context)
    )

    fun renderPossibleSquares2D(board: Board, aspectRatio: Float, displayWidth: Int, displayHeight: Int) {
        highlight2DProgram.start()
        highlight2DProgram.set("aspectRatio", aspectRatio)
        highlight2DProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 8f)
        highlight2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            highlight2DProgram.set("translations[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        quad.drawInstanced(board.getPossibleMoves().size)
        highlight2DProgram.stop()
    }

    fun renderSelectedSquares2D(board: Board, aspectRatio: Float, displayWidth: Int, displayHeight: Int) {
        selectedSquare2DProgram.start()
        selectedSquare2DProgram.set("aspectRatio", aspectRatio)
        selectedSquare2DProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 8.0f)
        selectedSquare2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))

        var i = 0

        if (board.selectedSquare != Vector2(-1, -1)) {
            selectedSquare2DProgram.set("translations[$i]", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare2DProgram.set("colors[$i]", Color(0.0f, 0.0f, 1.0f))
            i++
        }

        if (board.checkedKingSquare != Vector2(-1, -1)) {
            selectedSquare2DProgram.set("translations[$i]", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare2DProgram.set("colors[$i]", Color(1.0f, 0.0f, 0.0f))
            i++
        }

        quad.drawInstanced(i)

        selectedSquare2DProgram.stop()
    }

    fun renderSelectedSquares3D(board: Board, aspectRatio: Float, displayWidth: Int, displayHeight: Int, camera: Camera) {
        selectedSquare3DProgram.start()
        selectedSquare3DProgram.set("aspectRatio", aspectRatio)
        selectedSquare3DProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 8.0f)
        selectedSquare3DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
        selectedSquare3DProgram.set("projection", camera.projectionMatrix)
        selectedSquare3DProgram.set("view", camera.viewMatrix)

        var i = 0

        if (board.selectedSquare != Vector2(-1, -1)) {
            selectedSquare3DProgram.set("translations[$i]", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare3DProgram.set("colors[$i]", Color(0.0f, 0.0f, 1.0f))
            i++
        }

        if (board.checkedKingSquare != Vector2(-1, -1)) {
            selectedSquare3DProgram.set("translations[$i]", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare3DProgram.set("colors[$i]", Color(1.0f, 0.0f, 0.0f))
            i++
        }

        quad.drawInstanced(i)

        selectedSquare3DProgram.stop()
    }

    fun destroy() {
        quad.destroy()
        highlight2DProgram.destroy()
    }
}