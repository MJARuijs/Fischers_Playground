package com.mjaruijs.fischersplayground.util

import android.content.Context
import java.io.*

object FileManager {

    fun write(context: Context, fileName: String, content: String): Boolean {
        return try {
            val writer = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            writer.write(content)
            writer.close()
            println("Wrote: $content")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun read(context: Context, fileName: String): List<String>? {
        return try {
            val inputStream = context.openFileInput(fileName)
            val inputReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputReader)

            val lines = bufferedReader.readLines()

            println("read: ")
            lines.forEach { line -> println(line) }
            println()

            lines
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun delete(context: Context, fileName: String) {
        context.deleteFile(fileName)
    }

}