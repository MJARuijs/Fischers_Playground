package com.mjaruijs.fischersplayground.util

import com.mjaruijs.fischersplayground.util.Color


/**
 * The Severity class indicates the severity of a log to the console, with a corresponding color.
 * Different options are INFO, DEBUG, WARNING, and ERROR.
 */
enum class Severity(var type: String, var color: Color) {

    INFO("INFO", Color.WHITE),
    DEBUG("DEBUG", Color.BLUE),
    WARNING("WARNING", Color.YELLOW)

}
