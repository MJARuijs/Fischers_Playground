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

    private val takenPieces = ArrayList<TakenPieceData>()
    private val pieceOffset = 10
    private var maxPieceWidth = 0
    private var pieceHeight = 0

    private val paint = Paint()
    private lateinit var team: Team

    fun init(team: Team) {
        this.team = team

        paint.isAntiAlias = true
        paint.color = if (team == Team.WHITE) Color.BLACK else Color.WHITE
    }

    fun add(pieceType: PieceType) {
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

        val numberOfPieces = takenPieces.size
        val left = numberOfPieces * maxPieceWidth
        val right = (numberOfPieces + 1) * maxPieceWidth

        val rect = Rect(left, pieceOffset, right, pieceHeight + pieceOffset)
        val outlineRect = Rect(left - pieceOffset, 0, right + pieceOffset, pieceHeight + pieceOffset * 2)

        takenPieces += TakenPieceData(pieceType, team, bitmap, rect, outlineBitmap, outlineRect)
        sort()

        invalidate()
    }

    fun removeTakenPiece(type: PieceType) {
        val lastPieceIndex = takenPieces.indexOfLast { piece -> piece.type == type }
        if (lastPieceIndex == -1) {
            return
        }

        takenPieces.removeAt(lastPieceIndex)
        sort()

        invalidate()
    }

    fun removeAllPieces() {
        takenPieces.clear()
        invalidate()
    }

    private fun sort() {
        takenPieces.sortWith { piece1, piece2 ->
            if (piece1.type.sortingValue > piece2.type.sortingValue) 1 else -1
        }
        recalculateWhiteBorders()
    }

    private fun recalculateWhiteBorders() {
        for ((i, _) in takenPieces.withIndex()) {
            takenPieces[i].rect.left = i * maxPieceWidth
            takenPieces[i].rect.right = (i + 1) * maxPieceWidth
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

        for (piece in takenPieces) {
            canvas.drawBitmap(piece.bitmap, null, piece.rect, paint)
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