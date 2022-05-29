package com.mjaruijs.fischersplayground.listeners

import android.view.ScaleGestureDetector

class ScaleListener(private var scale: Float, private val onScaleChanged: (Float) -> Unit) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        if (detector == null) {
            return false
        }

        println("${detector.currentSpan - detector.previousSpan} ${detector.scaleFactor}")

        val direction = if (detector.currentSpan - detector.previousSpan > 0) 1 else 1

        scale = ((detector.scaleFactor))
//        scale = ((detector.currentSpan - detector.previousSpan) * 0.01f)
        onScaleChanged(scale)

        return super.onScale(detector)
    }

}