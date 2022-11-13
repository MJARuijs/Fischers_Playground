package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton2

open class ActionBarFragment : Fragment() {

    private lateinit var layout: LinearLayout
    private val childViews = ArrayList<View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.action_bar_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout = view.findViewById(R.id.action_bar_layout)
    }

    fun addButtons(button: UIButton2) {
        button.layoutParams = BUTTON_LAYOUT
        layout.addView(button, BUTTON_LAYOUT)
        childViews.add(button)
    }

    fun addButtonsAt(button: UIButton2, index: Int) {
        button.layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
        layout.addView(button, index, BUTTON_LAYOUT)
        layout.requestLayout()
        childViews.add(button)
    }

    fun addButtonsToLeft(button: UIButton2) {
        val childCount = 0
        layout.addView(button, childCount, BUTTON_LAYOUT)
        childViews.add(button)
    }

    fun removeButton(button: UIButton2) {
        layout.removeView(button)
        childViews.remove(button)
    }

    fun containsView(view: View) = childViews.contains(view)

    fun invalidate() {
        layout.invalidate()
        layout.requestLayout()
        layout.requestApplyInsets()
    }

    companion object {

        val BUTTON_LAYOUT = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
        val BACKGROUND_COLOR = Color.rgb(0.15f, 0.15f, 0.15f)

        const val TEXT_SIZE = 16f
    }

}