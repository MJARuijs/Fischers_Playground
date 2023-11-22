package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.core.view.contains
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton2

open class ActionBarFragment : Fragment() {

    protected lateinit var runOnUIThread: (() -> Unit) -> Unit

    protected lateinit var layout: LinearLayout
    private val childViews = ArrayList<View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.action_bar_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout = view.findViewById(R.id.action_bar_layout)
    }

    fun addButton(button: UIButton2, tag: String) {
        button.layoutParams = BUTTON_LAYOUT
        button.tag = tag
        layout.addView(button, BUTTON_LAYOUT)
        childViews.add(button)
    }

    fun addButtonToLeft(button: UIButton2) {
        val childCount = 0
        layout.addView(button, childCount, BUTTON_LAYOUT)
        childViews.add(button)
    }

    fun removeButton(button: UIButton2) {
        layout.removeView(button)
        childViews.remove(button)
    }

    fun showButton(vararg tags: String, hideOtherButtons: Boolean = true) {
        for (view in childViews) {
            if (view !is UIButton2) {
                continue
            }

            if (!tags.contains(view.tag) && hideOtherButtons) {
                view.hide()
            } else {
                view.show()
            }
        }
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