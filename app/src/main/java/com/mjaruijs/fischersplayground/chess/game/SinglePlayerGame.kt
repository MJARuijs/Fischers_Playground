package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class SinglePlayerGame : Game(false) {

    private var teamToMove = Team.WHITE

    override fun getCurrentTeam() = teamToMove

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(if (isPlayingWhite) Team.WHITE else Team.BLACK, piece, square, true, state, moves, lookingForCheck)

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

        val actualFromPosition: Vector2
        val actualToPosition: Vector2

        if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        } else {
//            actualFromPosition = Vector2(7, 7) - fromPosition
//            actualToPosition = Vector2(7, 7) - toPosition
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        }

//        println("fromPosition: $fromPosition, toPosition: $toPosition, actualFromPosition: $actualFromPosition, actualToPosition: $actualToPosition")



        return super.move(team, actualFromPosition, actualToPosition, runInBackground)
    }

    override fun processOnClick(clickedSquare: Vector2): Action {
        if (board.isASquareSelected()) {
            val previouslySelectedSquare = board.selectedSquare

            if (possibleMoves.contains(clickedSquare)) {
                Thread {
                    move(teamToMove, previouslySelectedSquare, clickedSquare, false)
                }.start()

                return Action.PIECE_MOVED
            }

            val pieceAtSquare = state[clickedSquare] ?: return Action.NO_OP

            if (pieceAtSquare.team == teamToMove) {
                return if (FloatUtils.compare(previouslySelectedSquare, clickedSquare)) Action.SQUARE_DESELECTED else Action.SQUARE_SELECTED
            }
        } else {
            val pieceAtSquare = state[clickedSquare] ?: return Action.NO_OP

            if (pieceAtSquare.team == teamToMove) {
                return Action.SQUARE_SELECTED
            }
        }

        return Action.NO_OP
    }
}