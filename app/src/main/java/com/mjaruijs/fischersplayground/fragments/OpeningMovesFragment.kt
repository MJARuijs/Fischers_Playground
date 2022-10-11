package com.mjaruijs.fischersplayground.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.MoveView

class OpeningMovesFragment : Fragment() {

    private lateinit var typeFace: Typeface

    private lateinit var moveNumbersLayout: LinearLayout
    private lateinit var movesLayout: TableLayout

    private var moveViewHeight = -1
    private var numberViewHeight = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_moves_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        moveNumbersLayout = view.findViewById(R.id.move_numbers_layout)
        movesLayout = view.findViewById(R.id.moves_table)

        typeFace = resources.getFont(R.font.anonymous_bold)
    }

    fun addMove(move: Move) {
        if (move.team == Team.WHITE) {
            createRow()
        }

        val moveView = MoveView(requireContext(), move)
//        if (movesLayout.childCount == 1) {
//            println("Setting callback!")
//            moveView.onLayoutChanged = ::onMoveViewInitialized
//        } else {
//            println("ChildCount: ${movesLayout.childCount}")
//        }
        if (move.team == Team.WHITE) {
            println("Setting callback")
            moveView.onLayoutChanged = ::onMoveViewInitialized
        }

        (movesLayout[movesLayout.childCount - 1] as TableRow).addView(moveView.view)
    }

    fun onMoveViewInitialized(height: Int, name: String) {
//        if (moveViewHeight != -1) {
//            return
//        }

        moveViewHeight = height
        numberViewHeight = moveNumbersLayout[0].height

//        for (i in 0 until moveNumbersLayout.childCount) {
        val i = moveNumbersLayout.childCount - 1
        if (moveNumbersLayout[i].paddingBottom != 0) {
            return
        }
            val textViewHeight = moveNumbersLayout[i].height
            val topPadding = (height - textViewHeight) / 2
            val bottomPadding = height - topPadding - textViewHeight

            (moveNumbersLayout[i] as TextView).setPadding(0, topPadding, 0, bottomPadding)
            println("$name Setting padding: $topPadding $bottomPadding $textViewHeight $moveViewHeight")
//            moveNumbersLayout[i].minimumHeight = moveViewHeight

//            moveNumbersLayout[i].minimumHeight = height
//        }
    }

    private fun createRow() {

        val currentRow = movesLayout.childCount + 1


        val textView = TextView(requireContext())

//        if (currentRow == 1) {
//        textView.setBackgroundColor(Color.GREEN)
//        } else if (currentRow == 2) {
//            textView.setBackgroundColor(Color.BLUE)
//        } else {
//            textView.setBackgroundColor(Color.RED)
//        }
//        textView.addOnLayoutChangeListener { view, i, i2, i3, i4, i5, i6, i7, i8 ->
//            if (moveViewHeight == -1) {
//                return@addOnLayoutChangeListener
//            }
//
//            if (moveViewHeight == view.height) {
//                return@addOnLayoutChangeListener
//            }
//
//            val textViewHeight = view.height
//            val topPadding = (moveViewHeight - textViewHeight) / 2
//            val bottomPadding = moveViewHeight - topPadding - textViewHeight
////            view.setPadding(0, topPadding, 0, bottomPadding)
////            view.minimumHeight = moveViewHeight
////            println("2. Setting padding: $topPadding $bottomPadding $textViewHeight $moveViewHeight")
//        }

        textView.text = "$currentRow."
        textView.textSize = 20.0f
        textView.typeface = typeFace
//        textView.layoutParams = params

//        if (moveViewHeight != -1) {
//            val textViewHeight = moveNumbersLayout[i].height
//            val topPadding = (height - textViewHeight) / 2
//            val bottomPadding = height - topPadding - textViewHeight
//            (moveNumbersLayout[i] as TextView).setPadding(0, topPadding, 0, bottomPadding)
//        }


        moveNumbersLayout.addView(textView)

        val row = TableRow(requireContext())
        movesLayout.addView(row)

    }

}