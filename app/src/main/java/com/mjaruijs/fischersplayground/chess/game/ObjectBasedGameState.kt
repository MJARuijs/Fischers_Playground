package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.roundToInt

class ObjectBasedGameState(private val isPlayingWhite: Boolean, private val pieces: ArrayList<Piece> = ArrayList()) {


    init {
        if (pieces.isEmpty()) {
            val whiteIndex = if (isPlayingWhite) 0 else 7
            val blackIndex = if (isPlayingWhite) 7 else 0
            val whitePawnIndex = if (isPlayingWhite) 1 else 6
            val blackPawnIndex = if (isPlayingWhite) 6 else 1
            
            val queenOffset = if (isPlayingWhite) 0 else 1

            pieces += Piece(PieceType.ROOK, Team.WHITE, Vector2(0, whiteIndex))
            pieces += Piece(PieceType.KNIGHT, Team.WHITE, Vector2(1, whiteIndex))
            pieces += Piece(PieceType.BISHOP, Team.WHITE, Vector2(2, whiteIndex))
            pieces += Piece(PieceType.QUEEN, Team.WHITE, Vector2(3 + queenOffset, whiteIndex))
            pieces += Piece(PieceType.KING, Team.WHITE, Vector2(4 - queenOffset, whiteIndex))
            pieces += Piece(PieceType.BISHOP, Team.WHITE, Vector2(5, whiteIndex))
            pieces += Piece(PieceType.KNIGHT, Team.WHITE, Vector2(6, whiteIndex))
            pieces += Piece(PieceType.ROOK, Team.WHITE, Vector2(7, whiteIndex))

            pieces += Piece(PieceType.ROOK, Team.BLACK, Vector2(0, blackIndex))
            pieces += Piece(PieceType.KNIGHT, Team.BLACK, Vector2(1, blackIndex))
            pieces += Piece(PieceType.BISHOP, Team.BLACK, Vector2(2, blackIndex))
            pieces += Piece(PieceType.QUEEN, Team.BLACK, Vector2(3 + queenOffset, blackIndex))
            pieces += Piece(PieceType.KING, Team.BLACK, Vector2(4 - queenOffset, blackIndex))
            pieces += Piece(PieceType.BISHOP, Team.BLACK, Vector2(5, blackIndex))
            pieces += Piece(PieceType.KNIGHT, Team.BLACK, Vector2(6, blackIndex))
            pieces += Piece(PieceType.ROOK, Team.BLACK, Vector2(7, blackIndex))

            for (i in 0 until 8) {
                pieces += Piece(PieceType.PAWN, Team.WHITE, Vector2(i, whitePawnIndex))
                pieces += Piece(PieceType.PAWN, Team.BLACK, Vector2(i, blackPawnIndex))
            }
        }
        
    }

    operator fun get(x: Int, y: Int): Piece? {
        return pieces.find { piece ->
            piece.getBoardX().roundToInt() == x && piece.getBoardY().roundToInt() == y
        }
    }

    operator fun get(x: Float, y: Float): Piece? {
        return get(x.roundToInt(), y.roundToInt())
    }

    operator fun get(square: Vector2): Piece? {
        return get(square.x, square.y)
    }

    operator fun get(x: Int, y: Int, team: Team): Piece? {
        return pieces.find { piece ->
            piece.getBoardX().roundToInt() == x && piece.getBoardY().roundToInt() == y && piece.team == team
        }
    }

    operator fun get(x: Float, y: Float, team: Team): Piece? {
        return get(x.roundToInt(), y.roundToInt(), team)
    }

    operator fun get(square: Vector2, team: Team): Piece? {
        return get(square.x, square.y, team)
    }

    fun getPieces(): ArrayList<Piece> {
        return pieces
    }

    fun add(type: PieceType, team: Team, square: Vector2) {
        add(Piece(type, team, square))
    }

    fun add(piece: Piece) {
        pieces += piece
    }

    fun replace(square: Vector2, type: PieceType, team: Team) {
        remove(square)
        add(Piece(type, team, square))
    }

    fun move(oldSquare: Vector2, newSquare: Vector2, onAnimationFinish: () -> Unit = {}) {
//        val piece = get(oldSquare) ?: return
//        remove(newSquare)
        pieces.find { piece ->
            piece.boardPosition == oldSquare
        }?.move(newSquare) {
            onAnimationFinish()
        }
    }

    fun setPosition(oldSquare: Vector2, newSquare: Vector2) {
        remove(newSquare)

        pieces.find { piece ->
            piece.boardPosition == oldSquare
        }?.setPosition(newSquare)
    }

    /**
     * Remove the first piece that is found at the given square and return it
     */
    fun remove(square: Vector2): Piece? {
        val piece = get(square) ?: return null
        pieces.remove(piece)
        return piece
    }

    /**
     * Remove the piece from a specific team at a given square, and return that piece
     */
    fun remove(square: Vector2, team: Team): Piece? {
        val piece = get(square, team)
        if (piece == null) {
            println("Could not find piece at $square from team $team")
            return null
        }
        println("Removing ${piece.type} on ${piece.team} at $square")
        pieces.remove(piece)
        return piece
    }
    
    fun copy(): ObjectBasedGameState {
        val copiedPieces = ArrayList<Piece>()
        
        for (piece in pieces) {
            copiedPieces += Piece(piece.type, piece.team, Vector2(piece.boardPosition))
        }
        
        return ObjectBasedGameState(isPlayingWhite, copiedPieces)
    }
    
}