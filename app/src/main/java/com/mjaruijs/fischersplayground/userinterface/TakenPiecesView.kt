package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import kotlin.math.roundToInt

class TakenPiecesView(context: Context, attributes: AttributeSet?) : View(context, attributes) {

    private val takenWhitePieces = ArrayList<TakenPieceData>()
    private val takenBlackPieces = ArrayList<TakenPieceData>()
    private val pieceOffset = 10
    private var maxPieceWidth = 0
    private var pieceHeight = 0
    private val whitePaint = Paint()
    private val blackPaint = Paint()

    init {
        whitePaint.color = Color.WHITE
        whitePaint.isAntiAlias = true

        blackPaint.color = Color.BLACK
        blackPaint.isAntiAlias = true
//        blackPaint.setShadowLayer(10.0f, 10.0f, 10.0f, Color.WHITE)
    }

    fun add(pieceType: PieceType, team: Team) {
        val resourceId = if (team == Team.WHITE) {
            when (pieceType) {
                PieceType.PAWN -> R.drawable.white_pawn
                PieceType.KNIGHT -> R.drawable.white_knight
                PieceType.BISHOP -> R.drawable.white_bishop
                PieceType.ROOK -> R.drawable.white_rook
                PieceType.QUEEN -> R.drawable.white_queen
                PieceType.KING -> R.drawable.white_king
            }
        } else {
            when (pieceType) {
                PieceType.PAWN -> R.drawable.black_pawn
                PieceType.KNIGHT -> R.drawable.black_knight
                PieceType.BISHOP -> R.drawable.black_bishop
                PieceType.ROOK -> R.drawable.black_rook
                PieceType.QUEEN -> R.drawable.black_queen
                PieceType.KING -> R.drawable.black_king
            }
        }

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Could not find resource with id: $resourceId")
        val bitmap = drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ALPHA_8)
        val outlineBitmap = drawable.toBitmap(drawable.intrinsicWidth + pieceOffset * 2, drawable.intrinsicHeight + pieceOffset * 2, Bitmap.Config.ALPHA_8)

        val numberOfPieces = if (team == Team.WHITE) takenWhitePieces.size else takenBlackPieces.size
        val left = numberOfPieces * maxPieceWidth
        val right = (numberOfPieces + 1) * maxPieceWidth

        val rect = Rect(left, pieceOffset, right, pieceHeight + pieceOffset)
        val outlineRect = Rect(left - pieceOffset, 0, right + pieceOffset, pieceHeight + pieceOffset * 2)

        if (team == Team.WHITE) {
            takenWhitePieces += TakenPieceData(pieceType, team, bitmap, rect, outlineBitmap, outlineRect)
            sortWhitePieces()
        } else {
            takenBlackPieces += TakenPieceData(pieceType, team, bitmap, rect, outlineBitmap, outlineRect)
            sortBlackPieces()
        }

        invalidate()
    }

    fun removeTakenPiece(type: PieceType, team: Team) {
        println("REMOVING $type from $team")
        if (team == Team.WHITE) {
            val lastPieceIndex = takenWhitePieces.indexOfLast { piece -> piece.type == type }
            if (lastPieceIndex == -1) {
                return
            }

            takenWhitePieces.removeAt(lastPieceIndex)
            sortWhitePieces()
        } else {
            val lastPieceIndex = takenBlackPieces.indexOfLast { piece -> piece.type == type }
            if (lastPieceIndex == -1) {
                return
            }

            takenBlackPieces.removeAt(lastPieceIndex)
            sortWhitePieces()
        }

        invalidate()
    }

    private fun sortWhitePieces() {
        takenWhitePieces.sortWith { piece1, piece2 ->
            if (piece1.type.sortingValue > piece2.type.sortingValue) 1 else -1
        }
        recalculateWhiteBorders()
    }

    private fun sortBlackPieces() {
        takenBlackPieces.sortWith { piece1, piece2 ->
            if (piece1.type.sortingValue > piece2.type.sortingValue) 1 else -1
        }
        recalculateBlackBorders()
    }

    private fun recalculateWhiteBorders() {
        for ((i, piece) in takenWhitePieces.withIndex()) {
            takenWhitePieces[i].rect.left = i * maxPieceWidth
            takenWhitePieces[i].rect.right = (i + 1) * maxPieceWidth
        }
    }

    private fun recalculateBlackBorders() {
        for ((i, piece) in takenBlackPieces.withIndex()) {
            takenBlackPieces[i].rect.left = i * maxPieceWidth
            takenBlackPieces[i].rect.right = (i + 1) * maxPieceWidth
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxPieceWidth = (w.toFloat() / 15f).roundToInt()
        pieceHeight = h

        if (pieceHeight > maxPieceWidth) {
            pieceHeight = maxPieceWidth
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        for (piece in takenWhitePieces) {
            canvas.drawBitmap(piece.bitmap, null, piece.rect, whitePaint)
        }

        for (piece in takenBlackPieces) {
            canvas.drawBitmap(piece.bitmap, null, piece.rect, blackPaint)
        }
    }

    private fun addWhiteBorder(bmp: Bitmap, borderSize: Float): Bitmap? {
        val bmpWithBorder = Bitmap.createBitmap(bmp.width + borderSize.roundToInt() * 2, bmp.height + borderSize.roundToInt() * 2, bmp.config)
        val canvas = Canvas(bmpWithBorder)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bmp, borderSize, borderSize, null)
        return bmpWithBorder
    }

    companion object {
        private const val MAX_PIECES = 15
    }

    inner class TakenPieceData(val type: PieceType, val team: Team, val bitmap: Bitmap, val rect: Rect, val outlineBitmap: Bitmap, val outlineRect: Rect)
}