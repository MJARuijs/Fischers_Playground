package com.mjaruijs.fischersplayground.activities.game

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.get
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.OpeningMoveView
import com.mjaruijs.fischersplayground.util.Logger
import kotlin.math.roundToInt

class OpeningMovesFragment2(private val moves: ArrayList<Move>) : Fragment() {

    private lateinit var typeFace: Typeface

    lateinit var scrollView: ScrollView

    lateinit var moveTable: TableLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        typeFace = resources.getFont(R.font.anonymous_bold)

        scrollView = view.findViewById(R.id.scroll_view)
        moveTable = view.findViewById(R.id.move_table)

        for (move in moves) {
            if (moveTable.childCount == 0 || move.team == Team.WHITE) {
                addRow(moveTable.childCount)
            }

            Logger.debug("MyTag", "Adding move: ${move.toChessNotation()}, ${moveTable.childCount}")

            val row = moveTable[moveTable.childCount - 1] as TableRow
//            addMoveCounter(moveTable.childCount, row)
            addMoveToRow(move, row)
        }
    }

    private fun addRow(moveNumber: Int) {
        val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val tableRow = TableRow(requireContext())

        if (moveNumber % 2 == 0) {
            tableRow.setBackgroundColor(Color.rgb(0.3f, 0.3f, 0.3f))
        } else {
            tableRow.setBackgroundColor(Color.DKGRAY)
        }

        tableRow.layoutParams = rowParams
        tableRow.orientation = LinearLayout.HORIZONTAL

        addMoveCounter(moveNumber + 1, tableRow)
        moveTable.addView(tableRow)
    }

    private fun addMoveCounter(moveNumber: Int, tableRow: TableRow) {
        val moveStringPlaceHolder = resources.getString(R.string.move_row_string)
        val moveString = String.format(moveStringPlaceHolder, moveNumber)

        val textView = TextView(requireContext())
        textView.gravity = Gravity.CENTER
        textView.setPadding(dpToPx(resources, 8), 0, 0, 0)
        textView.text = moveString
        textView.textSize = 20.0f
        textView.typeface = typeFace

        tableRow.addView(textView)
    }

    private fun addMoveToRow(move: Move, tableRow: TableRow) {
        val moveView = OpeningMoveView(requireContext())
        moveView.setMove(move)
        moveView.doOnLayout {
            tableRow[0].minimumHeight = moveView.height
        }
        tableRow.addView(moveView)
    }

    private fun dpToPx(resources: Resources, dp: Int): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}