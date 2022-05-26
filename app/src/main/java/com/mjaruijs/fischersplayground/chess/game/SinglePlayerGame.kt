package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class SinglePlayerGame : Game(true) {

    private var teamToMove = Team.WHITE

    override fun getCurrentTeam() = teamToMove

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(piece.team, piece, square, true, state, moves, lookingForCheck)

    override fun showPreviousMove(): Pair<Boolean, Boolean> {
        if (currentMoveIndex != -1) {
            teamToMove = !teamToMove
        }
        return super.showPreviousMove()
    }

    override fun showNextMove(): Pair<Boolean, Boolean> {
        if (!isShowingCurrentMove()) {
            teamToMove = !teamToMove
        }
        return super.showNextMove()
    }

    override fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean): Move {
        if (!isShowingCurrentMove()) {
            val moveCount = moves.size
            for (i in currentMoveIndex + 1 until moveCount) {
                if (i == -1) {
                    continue
                }
                moves.removeLast()
            }
            currentMoveIndex = moves.size - 1
        }
        teamToMove = !teamToMove

        return super.move(team, fromPosition, toPosition, runInBackground)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean) {
        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        move(team, fromPosition, toPosition, runInBackground)

//        val pieceAtNewPosition = state[toPosition]
//        val move = finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)

//        move.movedPiece = state[toPosition]?.type ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
    }

    override fun processOnClick(square: Vector2): Action {
        if (board.isASquareSelected()) {
            val selectedSquare = board.selectedSquare

            if (possibleMoves.contains(square)) {

                val pieceAtSelectedSquare = state[selectedSquare] ?: return Action.NO_OP

//                if (pieceAtSelectedSquare.type == PieceType.PAWN && (square.y == 0f || square.y == 7f)) {

//                } else {
//                println("TEAM TO MOVE: $teamToMove")
                Thread {
                    move(teamToMove, selectedSquare, square, false)
//                    movePlayer(selectedSquare, square, false)
                }.start()
//                }

                return Action.PIECE_MOVED
            }

            val pieceAtSquare = state[square] ?: return Action.NO_OP

            if (pieceAtSquare.team == teamToMove) {
                return if (FloatUtils.compare(selectedSquare, square)) Action.SQUARE_DESELECTED else Action.SQUARE_SELECTED
            }
        } else {
            val pieceAtSquare = state[square] ?: return Action.NO_OP

            if (pieceAtSquare.team == teamToMove) {
                return Action.SQUARE_SELECTED
            }
        }

        return Action.NO_OP
    }
}