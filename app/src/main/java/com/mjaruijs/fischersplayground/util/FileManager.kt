package com.mjaruijs.fischersplayground.util

import android.content.Context
import android.widget.Toast
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

    fun listFilesInDirectory(context: Context): ArrayList<String> {
        val file = File("${context.filesDir.absoluteFile}")
        val files = file.listFiles()!!
        val fileList = ArrayList<String>()
        for (f in files) {
            fileList += f.name
        }
        return fileList
    }


    fun doesFileExist(context: Context, fileName: String): Boolean {
        val file = File("${context.filesDir.absoluteFile}/$fileName")
        return file.exists()
    }

    fun isFileEmpty(context: Context, fileName: String): Boolean {
        val file = File("${context.filesDir.absoluteFile}/$fileName")
        return file.length() == 0L
    }

    fun write(context: Context, fileName: String, content: String): Boolean {
        return try {
            val writer = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            writer.write(content)
            writer.close()
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

            bufferedReader.readLines()
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun delete(context: Context, fileName: String) {
        File("${context.filesDir.absoluteFile}/$fileName").delete()
    }

}