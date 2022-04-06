package com.mjaruijs.fischersplayground.gamedata

import com.mjaruijs.fischersplayground.gamedata.pieces.Piece
import com.mjaruijs.fischersplayground.gamedata.pieces.PieceTextures
import com.mjaruijs.fischersplayground.gamedata.pieces.PieceType
import com.mjaruijs.fischersplayground.gamedata.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.FloatUtils
import kotlin.math.roundToInt

class GameState {

    private val quad = Quad()
    private val sampler = Sampler(0)

    private val state = ArrayList<ArrayList<Piece?>>()
    private var possibleMoves = ArrayList<Vector2>()
    private var teamToMove = Team.WHITE

    init {
        initBoard()
    }

    fun move(fromPosition: Vector2, toPosition: Vector2) {

        val currentPositionPiece = state[fromPosition.x.roundToInt()][fromPosition.y.roundToInt()] ?: return
        val newPositionPiece = state[toPosition.x.roundToInt()][toPosition.y.roundToInt()]

        state[toPosition.x.roundToInt()][toPosition.y.roundToInt()] = currentPositionPiece

        animateMove(fromPosition, toPosition)

        state[fromPosition.x.roundToInt()][fromPosition.y.roundToInt()] = null

        if (newPositionPiece != null) {
            take(newPositionPiece, !teamToMove)
        }

        teamToMove = if (teamToMove == Team.WHITE) {
            Team.BLACK
        } else {
            Team.WHITE
        }

        possibleMoves.clear()
    }

    fun take(piece: Piece, team: Team) {

    }

    fun animateMove(fromPosition: Vector2, toPosition: Vector2) {

    }

    fun processAction(action: Action): Action {
        println(action)

        if (action.type == ActionType.SQUARE_DESELECTED) {
            return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)
        }

        if (action.previouslySelectedPosition == null || action.previouslySelectedPosition.x == -1.0f || action.previouslySelectedPosition.y == -1.0f) {
            val piece = state[action.clickedPosition.x.roundToInt()][action.clickedPosition.y.roundToInt()] ?: return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)

            if (action.type == ActionType.SQUARE_SELECTED) {
                if (piece.team == teamToMove) {
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }
            }
        } else {
            val previouslySelectedPiece = state[action.previouslySelectedPosition.x.roundToInt()][action.previouslySelectedPosition.y.roundToInt()]

            if (action.type == ActionType.SQUARE_SELECTED) {
                if (previouslySelectedPiece == null) {
                    return Action(action.clickedPosition, ActionType.NO_OP)
                }

                if (possibleMoves.contains(action.clickedPosition)) {
                    move(action.previouslySelectedPosition, action.clickedPosition)
                    return Action(action.clickedPosition, ActionType.PIECE_MOVED)
                }

                val currentlySelectedPiece = state[action.clickedPosition.x.roundToInt()][action.clickedPosition.y.roundToInt()] ?: return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)

                if (currentlySelectedPiece.team == teamToMove) {
                    if (FloatUtils.compare(action.clickedPosition, action.previouslySelectedPosition)) {
                        return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)
                    }
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }

                return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)
            }
        }

        return Action(action.clickedPosition, ActionType.NO_OP)
    }

    fun determinePossibleMoves(square: Vector2): ArrayList<Vector2> {
        val piece = state[square.x.roundToInt()][square.y.roundToInt()] ?: return arrayListOf()
        possibleMoves = PieceType.getPossibleMoves(piece, square, state)
        return possibleMoves
    }

    fun draw(shaderProgram: ShaderProgram, aspectRatio: Float) {
        sampler.bind(PieceTextures.getTextureArray())

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state[row][col] ?: continue

                shaderProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 4.0f)
                shaderProgram.set("textureId", piece.textureId.toFloat())
                shaderProgram.set("textureMaps", sampler.index)
                shaderProgram.set("translation", ((Vector2(row * aspectRatio * 2.0f, col * aspectRatio * 2.0f ) / 8.0f)  + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)) )
                quad.draw()
            }
        }
    }

    private fun initBoard() {
        for (row in 0 until 8) {
            state += ArrayList<Piece?>()

            for (col in 0 until 8) {
                state[row] += null
            }
        }

//        state[0][0] = Piece(PieceType.ROOK, Team.WHITE)
//        state[1][0] = Piece(PieceType.KNIGHT, Team.WHITE)
//        state[2][0] = Piece(PieceType.BISHOP, Team.WHITE)
//        state[3][0] = Piece(PieceType.QUEEN, Team.WHITE)
//        state[4][0] = Piece(PieceType.KING, Team.WHITE)
//        state[5][0] = Piece(PieceType.BISHOP, Team.WHITE)
//        state[6][0] = Piece(PieceType.KNIGHT, Team.WHITE)
//        state[7][0] = Piece(PieceType.ROOK, Team.WHITE)
//        state[0][7] = Piece(PieceType.ROOK, Team.BLACK)
//        state[1][7] = Piece(PieceType.KNIGHT, Team.BLACK)
//        state[2][7] = Piece(PieceType.BISHOP, Team.BLACK)
//        state[3][7] = Piece(PieceType.QUEEN, Team.BLACK)
//        state[4][7] = Piece(PieceType.KING, Team.BLACK)
//        state[5][7] = Piece(PieceType.BISHOP, Team.BLACK)
//        state[6][7] = Piece(PieceType.KNIGHT, Team.BLACK)
//        state[7][7] = Piece(PieceType.ROOK, Team.BLACK)
//
//        for (i in 0 until 4) {
//            state[i][1] = Piece(PieceType.PAWN, Team.WHITE)
//            state[i][6] = Piece(PieceType.PAWN, Team.BLACK)
//        }
//
//        state[2][2] = Piece(PieceType.PAWN, Team.BLACK)
//        state[4][5] = Piece(PieceType.PAWN, Team.WHITE)

        state[4][4] = Piece(PieceType.ROOK, Team.WHITE)
        state[2][4] = Piece(PieceType.PAWN, Team.BLACK)
        state[6][4] = Piece(PieceType.PAWN, Team.BLACK)
    }
}