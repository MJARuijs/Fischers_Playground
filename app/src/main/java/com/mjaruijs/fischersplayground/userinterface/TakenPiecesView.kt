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

    private fun sort() {
        takenPieces.sortBy { pieceType -> pieceType.type.value }
    }

    fun add(pieceType: PieceType, team: Team) {
//        println("PIECE ADDED: $pieceType, $team")
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

        val offset = 10

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Could not find resource with id: $resourceId")
        val bitmap = drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ALPHA_8)
        val outlineBitmap = drawable.toBitmap(drawable.intrinsicWidth + offset * 2, drawable.intrinsicHeight + offset * 2, Bitmap.Config.ALPHA_8)

        val numberOfPieces = takenPieces.filter { piece -> piece.team == team }.size
        val left = numberOfPieces * maxPieceWidth
        val right = (numberOfPieces + 1) * maxPieceWidth

        val rect = Rect(left, offset, right, pieceHeight + offset)
        val outlineRect = Rect(left - offset, 0, right + offset, pieceHeight + offset * 2)

        takenPieces += TakenPieceData(pieceType, team, bitmap, rect, outlineBitmap, outlineRect)
        sort()

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxPieceWidth = (w.toFloat() / 15f).roundToInt()
        pieceHeight = h

        if (pieceHeight > maxPieceWidth) {
            pieceHeight = maxPieceWidth
        }

//        println("Max piece width: $maxPieceWidth. Height: $h")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) {
            return
        }

        for (piece in takenPieces) {
//            println("Drawing piece")
            if (piece.team == Team.WHITE) {
                canvas.drawBitmap(piece.bitmap, null, piece.rect, whitePaint)
            } else {
//                canvas.drawBitmap(piece.outlineBitmap, null, piece.outlineRect, whitePaint)
                canvas.drawBitmap(piece.bitmap, null, piece.rect, blackPaint)
//                addWhiteBorder(piece.bitmap, 10.0f)
            }
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