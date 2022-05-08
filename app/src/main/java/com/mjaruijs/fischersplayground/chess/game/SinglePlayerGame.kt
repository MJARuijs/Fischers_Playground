package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.ActionType
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class SinglePlayerGame : Game(true) {

    private var teamToMove = Team.WHITE

    override fun getCurrentTeam() = teamToMove

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState) = PieceType.getPossibleMoves(piece.team, piece, square, true, state, moves)

    override fun showPreviousMove(): Pair<Boolean, Boolean> {
        teamToMove = !teamToMove
        return super.showPreviousMove()
    }

    override fun showNextMove(): Pair<Boolean, Boolean> {
        teamToMove = !teamToMove
       return super.showNextMove()
    }

    override fun processAction(action: Action): Action {
        if (action.type == ActionType.SQUARE_DESELECTED) {
            return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED) // Square deselected
        }

        if (action.type == ActionType.SQUARE_SELECTED) {
            // No piece has been selected yet
            if (action.previouslySelectedPosition == null || action.previouslySelectedPosition.x == -1.0f || action.previouslySelectedPosition.y == -1.0f) {
                val piece = state[action.clickedPosition] ?: return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)

                // Select a piece now, if the piece belongs to the team who's turn it is
                if (piece.team == teamToMove) {
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }
            } else { // A piece is already selected

                // If the newly selected square belongs to the possible moves of the selected piece, we can move to that new square
                if (possibleMoves.contains(action.clickedPosition)) {
                    move(teamToMove, action.previouslySelectedPosition, action.clickedPosition, true)
                    teamToMove = !teamToMove
                    return Action(action.clickedPosition, ActionType.PIECE_MOVED)
                }

                val currentlySelectedPiece = state[action.clickedPosition] ?: return Action(action.clickedPosition, ActionType.NO_OP)

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
}