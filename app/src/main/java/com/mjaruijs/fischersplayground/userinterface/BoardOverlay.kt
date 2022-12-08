package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.isDigitsOnly
import androidx.core.view.children
import androidx.core.view.doOnLayout
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BoardOverlay(context: Context, attributes: AttributeSet?) : LinearLayout(context, attributes) {

    private val layout: ConstraintLayout

    private val arrows = ArrayList<ArrowData>()
    private lateinit var bitmap: Bitmap
    private lateinit var paint: Paint
    private var squareHeight = -1f


    init {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.triangle, null)
        LayoutInflater.from(context).inflate(R.layout.board_text_layout, this, true)

        layout = findViewById(R.id.character_layout)
        layout.doOnLayout {
            setCharactersBold(true)
            setTextColor()
            paint = Paint()
            paint.isAntiAlias = true
            paint.color = ResourcesCompat.getColor(resources, R.color.accent_color, null)
            paint.alpha = (255f * 0.75f).roundToInt()



            squareHeight = layout.height / 8f
            bitmap = drawable!!.toBitmap(squareHeight.toInt() / 2, squareHeight.toInt() / 2, Bitmap.Config.ALPHA_8)

//            arrowBase = Rect(
//                (0.375f * squareHeight).toInt(),
//                (squareHeight * 0.5f).toInt() + (squareHeight * 2).toInt(),
//                (0.625f * squareHeight).toInt(),
//                squareHeight.toInt() + (squareHeight * 2).toInt()
//            )

//            pivot.x = squareHeight / 2f
//            pivot.y = squareHeight * 2f


        }



//        arrowMatrix.preScale(1f, 8f, 12f / 24f * bitmap.width, 9f / 24f * bitmap.height)
//        arrowMatrix.preScale(1f, 8f)
//        arrowMatrix.postRotate(180f)
//        matrix.postScale(1f, 8f, bitmap.width / 2f, bitmap.height / 2f)
//        matrix.preScale(1f, 8f, bitmap.width / 2f, bitmap.height / 2f)

//        arrowMatrix.postTranslate(0f, squareHeight * 4)


        setWillNotDraw(false)
    }

    fun addArrow(arrow: MoveArrow) {
        Logger.debug(TAG, "Adding arrow!")

//        arrows.clear()

        val startSquare = arrow.startSquare
        val endSquare = arrow.endSquare

        val startX = startSquare.x
        val startY = startSquare.y
        val endX = endSquare.x
        val endY = endSquare.y
        val xDif = abs(endSquare.x - startSquare.x).roundToInt()
        val yDif = abs(endSquare.y - startSquare.y).roundToInt()

        if (xDif == 0 && yDif == 0) {
            return
        }

        var angle = 0f
        var baseHeight = 0f

        var translationX = 0f
        var translationY = 0f

        if (startX == endX && startY > endY) {
            angle = 180f
            translationX = startSquare.x - 4 + 0.5f
            translationY = 3 - endY + 0.1f
            baseHeight = yDif.toFloat() - 0.25f
        } else if (startX == endX && startY < endY) {
            angle = 0f
            translationX = startSquare.x - 4 + 0.5f
            translationY = 4 - startY - yDif - 0.1f
            baseHeight = yDif.toFloat() - 0.25f
        } else if (startX > endX && startY == endY) {
            angle = 270f
            baseHeight = xDif.toFloat() - 0.25f
            translationX = startX - 3 - xDif - 0.1f
            translationY = 3 - startY + 0.5f
        } else if (startX < endX && startY == endY) {
            angle = 90f
            baseHeight = xDif.toFloat() - 0.25f
            translationX = endX - 4 + 0.1f
            translationY = 3 - startY + 0.5f
        } else {
            if ((xDif == 1 && yDif == 2) || (xDif == 2 && yDif == 1)) {
                // TODO: Implement knight jumps
            } else {
                if (xDif != yDif) {
                    return
                } else if (startX < endX && startY < endY) {
                    angle = 45f
                    translationX = startSquare.x - 4 + yDif
                    translationY = 4 - startY - yDif
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX > endX && startY < endY) {
                    angle = -45f
                    translationX = startSquare.x - 3 - yDif
                    translationY = 4 - startY - yDif
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX < endX && startY > endY) {
                    angle = 135f
                    translationX = startX - 4 + yDif
                    translationY = 3 - endY
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                } else if (startX > endX && startY > endY) {
                    angle = -135f
                    translationX = startX - 3 - yDif
                    translationY = 3 - endY
                    baseHeight = sqrt((xDif * xDif + yDif * yDif).toFloat()) - 0.5f
                }
            }
        }


//            arrowHeadMatrix.setTranslate(
//                0.25f * squareHeight + endX * 4,
//                squareHeight / 2f + endY * 4
//            )
//            arrowHeadMatrix.setRotate(45f, squareHeight / 4f, squareHeight / 4f)
        val arrowHeadMatrix = Matrix()
        arrowHeadMatrix.postTranslate(squareHeight * 3.75f, squareHeight * 3.505f)

        val arrowBase = Rect(
            (3.875f * squareHeight).toInt(),
            (squareHeight * 4).toInt(),
            (4.125f * squareHeight).toInt(),
            (4.5f * squareHeight).toInt() + ((baseHeight - 1) * squareHeight).toInt()
        )



        arrows += ArrowData(arrayListOf(arrowBase), arrowHeadMatrix, angle, Vector2(translationX * squareHeight, translationY * squareHeight))
//        arrows += arrow
//        invalidate()
    }

    private fun setCharactersBold(useBold: Boolean) {
        if (useBold) {
            for ((i, child) in layout.children.withIndex()) {
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
        for ((i, child) in layout.children.withIndex()) {
            if (child is TextView) {
                if (child.text.isDigitsOnly()) {
                    if (child.text.toString().toInt() % 2 == 1) {
                        child.setTextColor(whiteColor)
                    } else {
                        child.setTextColor(darkColor)
                    }
                } else {
                    if (i % 2 == 0) {
                        child.setTextColor(whiteColor)
                    } else {
                        child.setTextColor(darkColor)
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            Logger.debug(TAG, "Drawing but canvas is null")

            return
        }

        for (arrowData in arrows) {
            canvas.save()
            canvas.translate(arrowData.translation.x, arrowData.translation.y)
            canvas.rotate(arrowData.angle, width / 2f, height / 2f)
            canvas.drawBitmap(bitmap, arrowData.arrowHeadMatrix, paint)
            for (arrowBase in arrowData.arrowBases) {
                canvas.drawRect(arrowBase, paint)
            }
            canvas.restore()
        }
    }

    inner class ArrowData(val arrowBases: ArrayList<Rect>, val arrowHeadMatrix: Matrix, val angle: Float, val translation: Vector2)

    companion object {
        private const val TAG = "BoardOverlay"
        private val whiteColor = Color.rgb(207, 189, 175)
        private val darkColor = Color.rgb(91, 70, 53)

    }

}