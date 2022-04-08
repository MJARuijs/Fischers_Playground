package com.mjaruijs.fischersplayground.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Logger class, which handles both normal logs, as well as error messages.
 * Both types of logs can be written to the terminal or to a file.
 */
object Logger {

    private val TIME_FORMAT = SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.ENGLISH)

    private var errOutStream = System.err
    private var fileOutStream = System.out
    private var loggerOutStream = System.out

    private val errors = ArrayList<String>()

    private var printTag = true
    private var printColored = true
    private var printTimeStamp = true

    /**
     * Log a text with certain severity.
     * @param message the text to be logged.
     * @param severity the severity of the text.
     */
    private fun log(message: String, severity: Severity) {
        var prefix = ""

        if (printColored) {
            loggerOutStream.print(severity.color.code)
        }

        if (printTimeStamp) {
            prefix += createTimeStamp()
            loggerOutStream.print(createTimeStamp())
        }

        if (printTag) {
            prefix += createTag(severity)
            loggerOutStream.print(createTag(severity))
        }

        loggerOutStream.print(message + "\n" + Severity.INFO.color.code)

//        val logFile = File("log.txt")
//        val fileWriter = FileWriter(logFile, true)
//        fileWriter.appendln(prefix + message)
//        fileWriter.close()
    }

    /**
     * Helper method that creates a tag for the given severity.
     * @param severity the severity for which a tag must be created.
     * @return the string representation of the given severity.
     */
    private fun createTag(severity: Severity): String {
        return "[" + severity.type + "] "
    }

    /**
     * Helper method that creates a timestamp.
     * @return the string representation of the timestamp.
     */
    private fun createTimeStamp(): String {
        val date = Date()

        synchronized(TIME_FORMAT) {
            val timeStamp = TIME_FORMAT.format(date)
            return "[$timeStamp] "
        }
    }

    /**
     * Overload log method. Automatically prints with INFO severity.
     * @param message the text to be logged.
     */
    fun info(message: String) {
        log(message, Severity.INFO)
    }

    /**
     * Overload log method. Automatically prints with WARNING severity.
     * @param message the text to be logged.
     */
    fun warn(message: String) {
        log(message, Severity.WARNING)
    }

    /**
     * Overload log method. Automatically prints with DEBUG severity.
     * @param message the text to be logged.
     */
    fun debug(message: String) {
        log(message, Severity.DEBUG)
    }

    /**
     * Prints an error text to either the terminal, or to a file, depending on the writeErrorToFile parameter.
     * If the text should be written to a file, create a crash report file, with the title of the file being
     * the current date and time.
     * @param message the error text to be printed to either the terminal, or to a file.
     */
    fun err(message: String?) {
        if (message == null) {
            err("Cannot print a null string!")
            return
        }

        errors.add(message)
    }

    /**
     * First flushes the output stream, then closes it.
     */
    fun close() {
        fileOutStream.flush()
        if (fileOutStream !== System.out) {
            fileOutStream.close()
        }

        loggerOutStream.flush()
        if (loggerOutStream !== System.out) {
            loggerOutStream.close()
        }

        errors.clear()
        errOutStream.flush()
        errOutStream.close()
    }

}
/**
 * Private constructor, since the Logger should never be instantiated.
 */
