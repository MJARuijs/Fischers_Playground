package com.mjaruijs.fischersplayground.util

/**
 * The Color enum is used to output colored text to the terminal, using the Logger.
 */
enum class Color(var code: String) {

    WHITE("\u001b[037m"),
    BLUE("\u001b[034m"),
    YELLOW("\u001b[033m"),
    RED("\u001b[031m"),
    DEFAULT("\u001b[0m")
}
