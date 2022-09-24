package com.mjaruijs.fischersplayground.util

import android.content.Context
import android.widget.Toast
import com.mjaruijs.fischersplayground.networking.NetworkManager
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
            NetworkManager.getInstance().sendCrashReport(context, "file_manager_append_crash.txt", e.stackTraceToString())
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
            NetworkManager.getInstance().sendCrashReport(context, "file_manager_write_crash.txt", e.stackTraceToString())
            return false
        }
    }

    fun read(context: Context, fileName: String): List<String>? {
        return try {
            val file = File("${context.filesDir.absolutePath}/$fileName")
            if (file.exists()) {
                val inputStream = context.openFileInput(fileName)

                val inputReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputReader)

                bufferedReader.readLines()
            } else {
                file.createNewFile()
                arrayListOf()
            }
        } catch (e: FileNotFoundException) {
            NetworkManager.getInstance().sendCrashReport(context, "file_manager_read_crash.txt", e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    fun delete(context: Context, fileName: String) {
        File("${context.filesDir.absoluteFile}/$fileName").delete()
    }

}