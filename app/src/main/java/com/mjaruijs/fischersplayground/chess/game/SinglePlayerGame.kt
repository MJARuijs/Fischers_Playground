package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class SinglePlayerGame(isPlayingWhite: Boolean, lastUpdated: Long) : Game(isPlayingWhite, lastUpdated) {

    private var teamToMove = Team.WHITE

    override fun getCurrentTeam() = teamToMove

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(if (isPlayingWhite) Team.WHITE else Team.BLACK, piece, square, true, state, moves.subList(0, currentMoveIndex + 1), lookingForCheck)

    override fun showPreviousMove(runInBackground: Boolean, animationSpeed: Long) {
        if (currentMoveIndex != -1) {
            teamToMove = !teamToMove
        }
        super.showPreviousMove(runInBackground, animationSpeed)
    }

    override fun showNextMove(runInBackground: Boolean, animationSpeed: Long){
        if (!isShowingCurrentMove()) {
            teamToMove = !teamToMove
        }
        super.showNextMove(runInBackground, animationSpeed)
    }

    fun move(move: Move, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        move(move.team, move.getFromPosition(team), move.getToPosition(team), animationSpeed)
    }

    override fun resetMoves() {
        super.resetMoves()
        teamToMove = Team.WHITE
    }

    override fun swapMoves(newMoves: ArrayList<Move>, selectedMoveIndex: Int) {
        super.swapMoves(newMoves, selectedMoveIndex)
        val currentMove = getCurrentMove()
        teamToMove = if (currentMove == null) {
            Team.WHITE
        } else {
            !currentMove.team
        }
    }

    override fun setMove(move: Move) {
        super.setMove(move)
        teamToMove = !move.team
    }

    fun undoLastMove() {
        teamToMove = moves.last().team
        undoMove(moves.removeLast(), false)
    }

    override fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, animationSpeed: Long) {
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
        teamToMove = !team

        val actualFromPosition: Vector2
        val actualToPosition: Vector2

        if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        } else {
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        }

        super.move(team, actualFromPosition, actualToPosition, animationSpeed)
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