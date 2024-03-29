package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.get
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BoardOverlay(context: Context, attributes: AttributeSet?) : LinearLayout(context, attributes) {

    private val layout: ConstraintLayout

    private val straightArrows = ArrayList<ArrowData>()
    private val knightArrows = ArrayList<KnightArrowData>()

    private val arrows = ArrayList<MoveArrow>()

    private var arrowCanvas = Canvas()
    private var triangleBitmap: Bitmap? = null

    private var squareHeight = -1f

    private lateinit var knightArrowBitmap: Bitmap
    private lateinit var transparentPaint: Paint
    private lateinit var solidPaint: Paint

    private lateinit var finalBitmap: Bitmap

    init {
        val triangleDrawable = ResourcesCompat.getDrawable(resources, R.drawable.triangle, null)
        LayoutInflater.from(context).inflate(R.layout.board_text_layout, this, true)

        layout = findViewById(R.id.character_layout)
        layout.doOnLayout {
            setCharactersBold(true)
            setTextColor()

            transparentPaint = Paint()
            transparentPaint.isAntiAlias = true
            transparentPaint.color = ResourcesCompat.getColor(resources, R.color.accent_color, null)
            transparentPaint.alpha = (255 * 0.75f).roundToInt()

            solidPaint = Paint()
            solidPaint.isAntiAlias = true
            solidPaint.color = ResourcesCompat.getColor(resources, R.color.accent_color, null)
            solidPaint.alpha = 255

            squareHeight = layout.height / 8f

            triangleBitmap = triangleDrawable!!.toBitmap(squareHeight.toInt() / 2, squareHeight.toInt() / 2, Bitmap.Config.ARGB_8888)

            val knightArrowDrawable = ResourcesCompat.getDrawable(resources, R.drawable.knight_arrow_2, null)
            knightArrowBitmap = knightArrowDrawable!!.toBitmap((squareHeight * 3).toInt(), (squareHeight * 2).toInt(), Bitmap.Config.ARGB_8888)

            finalBitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
            arrowCanvas = Canvas(finalBitmap)

            Logger.debug(TAG, "Arrows: ${arrows.size}")
            if (arrows.isNotEmpty()) {
                drawArrows(arrows)
            }
        }

        setWillNotDraw(false)
    }

    fun checkArrows() {
        if (arrows.isNotEmpty()) {
            for (arrow in arrows) {
                Logger.debug(TAG, "Has arrow from ${arrow.startSquare} to ${arrow.endSquare}")
            }
        } else {
            Logger.debug(TAG, "There are no arrows to be drawn")
        }
    }

    fun swapCharactersForBlack() {
        for (child in layout.children) {
            if (child is TextView) {
                child.text = child.tag.toString()
            }
        }
    }

    private fun setCharactersBold(@Suppress("SameParameterValue") useBold: Boolean) {
        if (useBold) {
            for (child in layout.children) {
                if (child is TextView) {
                    child.typeface = Typeface.DEFAULT_BOLD
                }
            }
        } else {
            for (child in layout.children) {
                if (child is TextView) {
                    child.typeface = Typeface.DEFAULT
                }
            }
        }
    }

    private fun setTextColor() {
        for (i in 0 until 8) {
            val child = layout[i] as TextView
            if (i % 2 == 1) {
                child.setTextColor(whiteColor)
            } else {
                child.setTextColor(darkColor)
            }
        }
        for (i in 8 until 16) {
            val child = layout[i] as TextView
            if (i % 2 == 0) {
                child.setTextColor(whiteColor)
            } else {
                child.setTextColor(darkColor)
            }
        }
    }

    fun drawArrows(arrows: ArrayList<MoveArrow>) {
        if (triangleBitmap == null) {
            this.arrows.addAll(arrows)
            return
        }

        clearArrows()
        for (arrow in arrows) {
            toggleArrow(arrow)
        }

        draw()
    }

    fun hideArrows() {
        arrowCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun clearArrows() {
        arrowCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        straightArrows.clear()
        knightArrows.clear()
        invalidate()
    }

    fun draw() {
        for (data in straightArrows) {
            drawStraightArrowToBitmap(data)
        }

        for (data in knightArrows) {
            drawKnightArrowToBitmap(data)
        }

        arrowCanvas.drawPoint(0f, 0f, transparentPaint)

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            return
        }

        canvas.drawBitmap(finalBitmap, 0f, 0f, transparentPaint)
    }

    private fun drawStraightArrowToBitmap(arrowData: ArrowData) {
        if (triangleBitmap == null) {
            return
        }

        Logger.debug(TAG, "Drawing straight arrow from ${arrowData.fromSquare} to ${arrowData.toSquare}")

        arrowCanvas.save()
        arrowCanvas.translate(arrowData.translationX, arrowData.translationY)
        arrowCanvas.rotate(arrowData.angle, width / 2f, height / 2f)
        arrowCanvas.drawBitmap(triangleBitmap!!, arrowData.arrowHeadMatrix, null)
        for (arrowBase in arrowData.arrowBases) {
            arrowCanvas.drawRect(arrowBase, solidPaint)
        }
        arrowCanvas.restore()
    }

    private fun drawKnightArrowToBitmap(arrowData: KnightArrowData) {
        arrowCanvas.save()
        arrowCanvas.rotate(arrowData.angle, arrowData.pivotX * squareHeight, arrowData.pivotY * squareHeight)
        arrowCanvas.drawBitmap(knightArrowBitmap, arrowData.matrix, null)
        arrowCanvas.restore()
        Logger.debug(TAG, "Drawing knight arrow from ${arrowData.fromSquare} to ${arrowData.toSquare}")
    }

    fun toggleArrow(moveArrow: MoveArrow): Boolean {
        val startSquare = moveArrow.startSquare
        val endSquare = moveArrow.endSquare

        val removedStraightArrow = straightArrows.removeIf { arrow -> arrow.fromSquare == startSquare && arrow.toSquare == endSquare }
        val removedKnightArrow = knightArrows.removeIf { arrow -> arrow.fromSquare == startSquare && arrow.toSquare == endSquare }

        if (removedStraightArrow || removedKnightArrow) {
            arrowCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//            for (arrow in straightArrows) {
//                drawStraightArrow(arrow)
//            }
//            for (arrow in knightArrows) {
//                drawKnightArrow(arrow)
//            }
            draw()
            return true
        }

        val startX = startSquare.x
        val startY = startSquare.y
        val endX = endSquare.x
        val endY = endSquare.y
        val xDif = (endSquare.x - startSquare.x).roundToInt()
        val yDif = (endSquare.y - startSquare.y).roundToInt()

        if (xDif == 0 && yDif == 0) {
            return false
        }

        var angle = 0f
        var baseHeight = 0f

        var translationX = 0f
        var translationY = 0f

        val arrowBases = ArrayList<Rect>()
        val arrowHeadMatrix = Matrix()
        val matrix = Matrix()

        if ((abs(xDif) == 1 && abs(yDif) == 2) || (abs(xDif) == 2 && abs(yDif) == 1)) {
            translationX = (startSquare.x.toInt() - 2f)
            translationY = 7 - startSquare.y.toInt() - 1f

            val pivotX = startX + 0.5f
            val pivotY = 7 - startY + 0.5f

            var scaleX = 1.0f
            var scaleY = 1.0f

            if (xDif == -2 && yDif == 1) {
                angle = 0f
            }
            if (xDif == -1 && yDif == 2) {
                angle = 90f
                scaleY = -1f
            }
            if (xDif == 1 && yDif == 2) {
                angle = 90f
            }
            if (xDif == 2 && yDif == 1) {
                angle = 0f
                scaleX = -1f
            }
            if (xDif == 2 && yDif == -1) {
                angle = 180f
            }
            if (xDif == 1 && yDif == -2) {
                scaleY = -1f
                angle = -90f
            }
            if (xDif == -1 && yDif == -2) {
                angle = -90f
            }

            if (xDif == -2 && yDif == -1) {
                scaleY = -1f
            }

            matrix.setTranslate(translationX * squareHeight, translationY * squareHeight)
            matrix.postScale(scaleX, scaleY, pivotX * squareHeight, pivotY * squareHeight)

            val arrowData = KnightArrowData(startSquare, endSquare, angle, matrix, pivotX, pivotY)
            knightArrows += arrowData

//            drawKnightArrow(arrowData)
        } else {
            val absXDif = abs(xDif)
            val absYDif = abs(yDif)
            if (startX == endX && startY > endY) {
                angle = 180f
                translationX = startSquare.x - 4 + 0.5f
                translationY = 3 - endY + 0.1f
                baseHeight = absYDif - 0.25f
            } else if (startX == endX && startY < endY) {
                angle = 0f
                translationX = startSquare.x - 4 + 0.5f
                translationY = 4 - startY - absYDif - 0.1f
                baseHeight = absYDif - 0.25f
            } else if (startX > endX && startY == endY) {
                angle = 270f
                baseHeight = absXDif - 0.25f
                translationX = startX - 3 - absXDif - 0.1f
                translationY = 3 - startY + 0.5f
            } else if (startX < endX && startY == endY) {
                angle = 90f
                baseHeight = absXDif - 0.25f
                translationX = endX - 4 + 0.1f
                translationY = 3 - startY + 0.5f
            } else {
                if (absXDif != absYDif) {
                    return false
                } else if (startX < endX && startY < endY) {
                    angle = 45f
                    translationX = startSquare.x - 4 + absYDif
                    translationY = 4 - startY - absYDif
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX > endX && startY < endY) {
                    angle = -45f
                    translationX = startSquare.x - 3 - absYDif
                    translationY = 4 - startY - absYDif
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX < endX && startY > endY) {
                    angle = 135f
                    translationX = startX - 4 + absYDif
                    translationY = 3 - endY
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX > endX && startY > endY) {
                    angle = -135f
                    translationX = startX - 3 - absYDif
                    translationY = 3 - endY
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                }
            }

            arrowBases += Rect(
                (3.875f * squareHeight).toInt(),
                (squareHeight * 4).toInt(),
                (4.125f * squareHeight).toInt(),
                (4.5f * squareHeight).toInt() + ((baseHeight - 1) * squareHeight).toInt()
            )

            arrowHeadMatrix.postTranslate(squareHeight * 3.75f, squareHeight * 3.504f)

            val arrowData = ArrowData(startSquare, endSquare, arrowBases, arrowHeadMatrix, angle, translationX * squareHeight, translationY * squareHeight)
            straightArrows += arrowData
//            drawStraightArrow(arrowData)
        }

        return false
    }

    inner class ArrowData(val fromSquare: Vector2, val toSquare: Vector2, val arrowBases: ArrayList<Rect>, val arrowHeadMatrix: Matrix, val angle: Float, val translationX: Float, val translationY: Float)

    inner class KnightArrowData(val fromSquare: Vector2, val toSquare: Vector2, val angle: Float, val matrix: Matrix, val pivotX: Float, val pivotY: Float)

    companion object {
        private const val TAG = "BoardOverlay"

        private val whiteColor = Color.rgb(207, 189, 175)
        private val darkColor = Color.rgb(91, 70, 53)
    }

}