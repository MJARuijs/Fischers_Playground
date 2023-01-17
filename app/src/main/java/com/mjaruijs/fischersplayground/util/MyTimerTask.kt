package com.mjaruijs.fischersplayground.util

import java.util.TimerTask

class MyTimerTask(private val onRun: () -> Unit) : TimerTask() {
    override fun run() {
        onRun()
    }
}