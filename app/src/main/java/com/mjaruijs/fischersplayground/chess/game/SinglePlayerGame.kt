package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class SinglePlayerGame(lastUpdated: Long) : Game(true, lastUpdated) {

    private var teamToMove = Team.WHITE

    override fun getCurrentTeam() = teamToMove

    override fun getPieceMoves(piece: Piece, square: Vector2, state: ArrayBasedGameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(if (isPlayingWhite) Team.WHITE else Team.BLACK, piece, square, true, state, moves, lookingForCheck)

    override fun showPreviousMove(runInBackground: Boolean, animationSpeed: Long): Pair<Boolean, Boolean> {
        if (currentMoveIndex != -1) {
            teamToMove = !teamToMove
        }
        return super.showPreviousMove(runInBackground, animationSpeed)
    }

    override fun showNextMove(runInBackground: Boolean, animationSpeed: Long): Pair<Boolean, Boolean> {
        if (!isShowingCurrentMove()) {
            teamToMove = !teamToMove
        }
        return super.showNextMove(runInBackground, animationSpeed)
    }

    fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
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
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        }

        move(team, actualFromPosition, actualToPosition, false, animationSpeed)
    }

    override fun processOnClick(clickedSquare: Vector2): Action {
        if (board.isASquareSelected()) {
            val previouslySelectedSquare = board.selectedSquare

            if (possibleMoves.contains(clickedSquare)) {
                Thread {
                    move(teamToMove, previouslySelectedSquare, clickedSquare)
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