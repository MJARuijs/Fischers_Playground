package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.Logger

class HighlightRenderer(resources: Resources) {

    private val quad = Quad()
    private val circleSampler = Sampler(0)
    private val effectSampler = Sampler(1)

    private val highlight2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.possible_square_highlighter_2d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.possible_square_highlighter_2d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val highlight3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.possible_square_highlighter_3d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.possible_square_highlighter_3d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val selectedSquare2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.selected_square_highlighter_2d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.selected_square_highlighter_2d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val selectedSquare3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.selected_square_highlighter_3d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.selected_square_highlighter_3d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val highlightedSquares = ArrayList<Vector2>()

    private var highlightLastMove = true

    fun enableLastMoveHighlights() {
        highlightLastMove = true
    }

    fun disableLastMoveHighlights() {
        highlightLastMove = false
    }

    fun addHighlightedSquare(square: Vector2) {
        highlightedSquares += square
    }

    fun removeHighlightedSquare(square: Vector2) {
        highlightedSquares.remove(square)
    }

    fun clearHighlightedSquares() {
        Logger.debug(TAG, "Clearing highlighted squares")
        highlightedSquares.clear()
    }

    fun renderPossibleSquares2D(board: Board, displayWidth: Int, displayHeight: Int, aspectRatio: Float) {
        highlight2DProgram.start()
        highlight2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
        highlight2DProgram.set("aspectRatio", aspectRatio)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            highlight2DProgram.set("translations[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        quad.drawInstanced(board.getPossibleMoves().size)
        highlight2DProgram.stop()
    }

    fun renderSelectedSquares2D(board: Board, displayWidth: Int, displayHeight: Int, aspectRatio: Float) {
        selectedSquare2DProgram.start()
        selectedSquare2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
        selectedSquare2DProgram.set("aspectRatio", aspectRatio)
        selectedSquare2DProgram.set("hasGradient", true)

        var i = 0

        if (board.selectedSquare != Vector2(-1, -1)) {
            selectedSquare2DProgram.set("translations[0]", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare2DProgram.set("colors[0]", Color(0.0f, 0.0f, 1.0f))
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

    fun renderLastMoveHighlights(game: Game, displayWidth: Int, displayHeight: Int) {
        val lastMove = game.getCurrentMove() ?: return
        val fromPosition = lastMove.getFromPosition(game.team)
        val toPosition = lastMove.getToPosition(game.team)

        selectedSquare2DProgram.start()
        selectedSquare2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
        selectedSquare2DProgram.set("hasGradient", false)

        selectedSquare2DProgram.set("translations[0]", (fromPosition / 8.0f) * 2.0f - 1.0f)
        selectedSquare2DProgram.set("colors[0]", Color(235f / 255f, 186f / 255f, 145f / 255f))

        selectedSquare2DProgram.set("translations[1]", (toPosition / 8.0f) * 2.0f - 1.0f)
        selectedSquare2DProgram.set("colors[1]", Color(235f / 255f, 186f / 255f, 145f / 255f))

        quad.drawInstanced(2)

        selectedSquare2DProgram.stop()
    }

    fun renderHighlightedSquares(game: Game, displayWidth: Int, displayHeight: Int) {
        selectedSquare2DProgram.start()
        selectedSquare2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
        selectedSquare2DProgram.set("hasGradient", false)

        var instances = 0

        val color = Color(251f / 255f, 159f / 255f, 110f / 255f)

        if (highlightedSquares.isNotEmpty()) {
            for ((i, square) in highlightedSquares.withIndex()) {
                Logger.debug(TAG, "Drawing highlight: $square")
                selectedSquare2DProgram.set("translations[${i}]", (square / 8.0f) * 2.0f - 1.0f)
                selectedSquare2DProgram.set("colors[${i}]", color)
                instances++
            }
        } else if (highlightLastMove) {
            val lastMove = game.getCurrentMove()

            if (lastMove != null) {
                instances += 2
                val fromPosition = lastMove.getFromPosition(game.team)
                val toPosition = lastMove.getToPosition(game.team)

                selectedSquare2DProgram.set("translations[0]", (fromPosition / 8.0f) * 2.0f - 1.0f)
                selectedSquare2DProgram.set("colors[0]", color)

                selectedSquare2DProgram.set("translations[1]", (toPosition / 8.0f) * 2.0f - 1.0f)
                selectedSquare2DProgram.set("colors[1]", color)
            }
        }

        quad.drawInstanced(instances)
        selectedSquare2DProgram.stop()
    }

    fun renderPossibleSquares3D(board: Board, camera: Camera) {
        highlight3DProgram.start()
        highlight3DProgram.set("projection", camera.projectionMatrix)
        highlight3DProgram.set("view", camera.viewMatrix)
        highlight3DProgram.set("circleTexture", circleSampler.index)
//        circleSampler.bind(circleText ure)

        for ((i, possibleSquare) in board.getPossibleMoves().withIndex()) {
            highlight3DProgram.set("translations[$i]", (possibleSquare / 8.0f) * 2.0f - 1.0f)
        }

        quad.drawInstanced(board.getPossibleMoves().size)
        highlight3DProgram.stop()
    }

    fun renderSelectedSquares3D(board: Board, camera: Camera) {
        selectedSquare3DProgram.start()
        selectedSquare3DProgram.set("projection", camera.projectionMatrix)
        selectedSquare3DProgram.set("view", camera.viewMatrix)
        selectedSquare3DProgram.set("effectSampler", effectSampler.index)
//        effectSampler.bind(textureEffects)

        var i = 0

        if (board.selectedSquare != Vector2(-1, -1)) {
            selectedSquare3DProgram.set("translations[0]", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare3DProgram.set("effects[0]", 0f)
            i++
        }

        if (board.checkedKingSquare != Vector2(-1, -1)) {
            selectedSquare3DProgram.set("translations[$i]", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)
            selectedSquare3DProgram.set("effects[$i]", 1f)
            i++
        }

        quad.drawInstanced(i)
        selectedSquare3DProgram.stop()
    }

    fun destroy() {
        quad.destroy()
        highlight2DProgram.destroy()
        highlight3DProgram.destroy()
        selectedSquare2DProgram.destroy()
        selectedSquare3DProgram.destroy()
    }

    companion object {

        private const val TAG = "HighlightRenderer"
    }
}