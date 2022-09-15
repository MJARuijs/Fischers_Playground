package com.mjaruijs.fischersplayground.util

import java.util.concurrent.LinkedBlockingQueue

class RenderThread : Thread() {

    private val queue = LinkedBlockingQueue<() -> Unit>()

    fun addTask(task: () -> Unit) {
        queue.offer(task)
    }

    override fun run() {
        while (true) {
            if (queue.isNotEmpty()) {
                processTask(queue.take())
            }
        }
    }

    private fun processTask(task: () -> Unit) {
        println("PROCESSING TASK")
        task()
    }
}