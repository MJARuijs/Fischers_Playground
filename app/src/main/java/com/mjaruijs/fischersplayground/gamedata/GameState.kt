package com.mjaruijs.fischersplayground.gamedata

import com.mjaruijs.fischersplayground.gamedata.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import kotlin.math.roundToInt

class GameState {

    private val state = ArrayList<ArrayList<PieceType?>>()

    private val quad = Quad()
    private val sampler = Sampler(0)

    init {
        for (row in 0 until 8) {
            state += ArrayList<PieceType?>()

            for (col in 0 until 8) {
                state[row] += null
            }
        }

        state[0][0] = PieceType.WHITE_ROOK
        state[1][0] = PieceType.WHITE_KNIGHT
        state[2][0] = PieceType.WHITE_BISHOP
        state[3][0] = PieceType.WHITE_QUEEN
        state[4][0] = PieceType.WHITE_KING
        state[5][0] = PieceType.WHITE_BISHOP
        state[6][0] = PieceType.WHITE_KNIGHT
        state[7][0] = PieceType.WHITE_ROOK

        state[0][7] = PieceType.BLACK_ROOK
        state[1][7] = PieceType.BLACK_KNIGHT
        state[2][7] = PieceType.BLACK_BISHOP
        state[3][7] = PieceType.BLACK_QUEEN
        state[4][7] = PieceType.BLACK_KING
        state[5][7] = PieceType.BLACK_BISHOP
        state[6][7] = PieceType.BLACK_KNIGHT
        state[7][7] = PieceType.BLACK_ROOK

        for (i in 0 until 8) {
            state[i][1] = PieceType.WHITE_PAWN
            state[i][6] = PieceType.BLACK_PAWN
        }
    }

    fun processAction(action: Action): Action {
        if (action.type == ActionType.SQUARE_SELECTED) {
            return if (state[action.position.x.roundToInt()][action.position.y.roundToInt()] != null) {
                Action(action.position, ActionType.SQUARE_SELECTED)
            } else {
                Action(action.position, ActionType.SQUARE_DESELECTED)
            }
        } else if (action.type == ActionType.SQUARE_DESELECTED) {
            return Action(action.position, ActionType.SQUARE_DESELECTED)
        }

        return Action(action.position, ActionType.NOOP)
    }

    fun draw(shaderProgram: ShaderProgram, aspectRatio: Float) {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state[row][col] ?: continue

                sampler.bind(piece.texture)
                shaderProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 4.0f)
                shaderProgram.set("texture", sampler.index)

                shaderProgram.set("translation", ((Vector2(row * aspectRatio * 2.0f, col * aspectRatio * 2.0f ) / 8.0f)  + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)) )
                quad.draw()
            }
        }
    }

}