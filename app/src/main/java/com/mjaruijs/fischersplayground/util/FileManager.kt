package com.mjaruijs.fischersplayground.util

import android.content.Context
import java.io.*

object FileManager {

    fun append(context: Context, fileName: String, content: String): Boolean {
        return try {
            val currentLines = read(context, fileName) ?: return write(context, fileName, content)
            var currentContent = ""
            for (line in currentLines) {
                currentContent += "$line\n"
            }
            val newContent = "$currentContent\n$content"
            return write(context, fileName, newContent)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun write(context: Context, fileName: String, content: String): Boolean {
        return try {
            val writer = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            writer.write(content)
            writer.close()
//            println("Wrote: $content")
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

//            println("read: ")
//            lines.forEach { line -> println(line) }
//            println()

            lines
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun delete(context: Context, fileName: String) {
        context.deleteFile(fileName)
    }

}