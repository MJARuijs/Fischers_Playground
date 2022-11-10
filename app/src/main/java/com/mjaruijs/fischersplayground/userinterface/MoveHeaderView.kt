package com.mjaruijs.fischersplayground.userinterface

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.mjaruijs.fischersplayground.R

class MoveHeaderView(context: Context, attributes: AttributeSet? = null) : LinearLayout(context, attributes) {

    private val headerTextView: TextView

    private var text: String = ""

    init {
        LayoutInflater.from(context).inflate(R.layout.moves_header_layout, this, true)

        headerTextView = findViewById(R.id.header_text)
    }

    fun setText(text: String) {
        this.text = text
        headerTextView.text = text
    }

    fun getText() = text

}