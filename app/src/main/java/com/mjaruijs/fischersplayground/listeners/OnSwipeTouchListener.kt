package com.mjaruijs.fischersplayground.listeners

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class OnSwipeTouchListener : View.OnTouchListener {



    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }



        event.action
        TODO("Not yet implemented")
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

    }


}