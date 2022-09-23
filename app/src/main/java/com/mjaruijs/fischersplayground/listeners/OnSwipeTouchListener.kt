package com.mjaruijs.fischersplayground.listeners

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class OnSwipeTouchListener(private val swipeDirection: SwipeDirection, private val onSwipe: () -> Unit, private val onClick: () -> Unit) : View.OnTouchListener, RecyclerView.OnItemTouchListener {

    private var previousX = 0f
    private var previousY = 0f

    private var totalDX = 0f
    private var holdStartTime = 0L

    override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
        processTouch(event)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        val consumed = processTouch(event)

        if (!consumed) {
            v!!.onTouchEvent(event)
        }

        return true
    }

    private fun processTouch(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            totalDX = 0f
            holdStartTime = System.currentTimeMillis()

            previousX = event.x
            previousY = event.y
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            val dx = event.x - previousX

            totalDX += dx

            previousX = event.x
            previousY = event.y
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val currentTime = System.currentTimeMillis()
            val totalHoldTime = currentTime - holdStartTime
            holdStartTime = 0L

            if (swipeDirection == SwipeDirection.RIGHT) {
                if (totalDX > 200f) {
                    onSwipe()
                    return true
                } else if (totalHoldTime < 250L) {
                    onClick()
                    return true
                }
            } else if (swipeDirection == SwipeDirection.LEFT) {
                if (totalDX < -200f) {
                    onSwipe()
                    return true
                } else if (abs(totalDX) < 10f && totalHoldTime < 250L) {
                    onClick()
                    return true
                }
            }
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        return true
    }

    enum class SwipeDirection {
        LEFT,
        RIGHT
    }

}