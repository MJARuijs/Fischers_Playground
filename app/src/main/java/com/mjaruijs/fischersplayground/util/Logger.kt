package com.mjaruijs.fischersplayground.util

import android.util.Log

object Logger {

    private val mutedTags = ArrayList<String>()

    fun mute(tag: String) {
        if (!mutedTags.contains(tag)) {
            mutedTags.add(tag)
        }
    }

    fun unMute(tag: String) {
        mutedTags.remove(tag)
    }

    fun info(tag: String, message: String) {
        if (!mutedTags.contains(tag)) {
            Log.i(tag, message)
        }
    }

    fun debug(tag: String, message: String) {
        if (!mutedTags.contains(tag)) {
            Log.d(tag, message)
        }
    }

    fun warn(tag: String, message: String) {
        if (!mutedTags.contains(tag)) {
            Log.w(tag, message)
        }
    }

    fun error(tag: String, message: String) {
        if (!mutedTags.contains(tag)) {
            Log.e(tag, message)
        }
    }
}