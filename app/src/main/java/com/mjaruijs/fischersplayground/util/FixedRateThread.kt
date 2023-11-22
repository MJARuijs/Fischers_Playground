package com.mjaruijs.fischersplayground.util

import java.util.concurrent.atomic.AtomicBoolean

class FixedRateThread(tps: Float, private val runnable: () -> Unit) {

    private var runTime = (1.0f / tps) * 1000000000L

    private val shouldStop = AtomicBoolean(false)

    fun setTps(tps: Float) {
        runTime = (1.0f / tps) * 1000000000L
    }

    fun run() {
        shouldStop.set(false)

        Thread {
            while (!shouldStop.get()) {
                val startTime = System.nanoTime()
                runnable()
                val endTime = System.nanoTime()

                var timeDifference = endTime - startTime

                while (timeDifference <= runTime.toLong()) {
                    timeDifference = System.nanoTime() - startTime
                }
            }
        }.start()
    }

    fun stop() {
        shouldStop.set(true)
    }

}