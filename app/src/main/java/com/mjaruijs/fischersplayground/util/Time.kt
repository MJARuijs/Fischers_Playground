package com.mjaruijs.fischersplayground.util

import java.text.SimpleDateFormat
import java.util.*

object Time {

    fun getTimeStamp(): Long {
        val timeString = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault()).format(System.currentTimeMillis())
        var timeWithoutPeriods = ""

        for (char in timeString) {
            if (char != '.') {
                timeWithoutPeriods += char
            }
        }

        return timeWithoutPeriods.toLong()
    }

}