package com.mjaruijs.fischersplayground.listeners

import android.widget.SearchView
import java.util.concurrent.atomic.AtomicBoolean

class OnSearchViewChangedListener(private val onStoppedTyping: () -> Unit) : SearchView.OnQueryTextListener {

    private val maxDelay = 1000L
    private var lastTextEdit = 0L

    private var threadStarted = AtomicBoolean(false)
    private var stopThread = AtomicBoolean(false)

    private var thread = Thread()

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (thread.state != Thread.State.TERMINATED) {
            onStoppedTyping()
            thread.interrupt()
            stopThread.set(true)
        }

        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText == null) {
            return false
        }

        if (newText.isNotEmpty()) {
            if (!threadStarted.get()) {
                thread = Thread {
                    while (true) {
                        if (stopThread.get()) {
                            break
                        }

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTextEdit > maxDelay) {
                            onStoppedTyping()
                            break
                        }
                    }
                    threadStarted.set(false)
                    stopThread.set(false)
                }

                thread.start()
                threadStarted.set(true)
            }

            lastTextEdit = System.currentTimeMillis()
        }

        return false
    }

}