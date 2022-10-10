package com.mjaruijs.fischersplayground.activities

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.MoveView

class TableTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_test)

        val moveNumbersLayout = findViewById<LinearLayout>(R.id.move_numbers_layout)
        val movesLayout = findViewById<TableLayout>(R.id.moves_table)

        for (i in 0 until 50) {
            val textView = TextView(applicationContext)
            textView.setBackgroundColor(Color.GREEN)
            textView.setPadding(0, 50, 0, 50)
            textView.text = "$i."
            textView.textSize = 20.0f
            moveNumbersLayout.addView(textView)
        }

//        for (x in 1 until 40) {
//            val tableRow = TableRow(applicationContext)
//
//            for (i in 0 until 8) {
//                val moveView = MoveView(applicationContext, "d${x * i}")
//                tableRow.addView(moveView.view)
//            }
//
//            movesLayout.addView(tableRow)
//        }

    }
}