package com.mjaruijs.fischersplayground.util

import android.content.Context
import com.mjaruijs.fischersplayground.networking.NetworkManager
import java.io.*

object FileManager {

    private var filesPath = ""

    fun init(context: Context) {
        filesPath = context.filesDir.absolutePath
    }

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
            NetworkManager.getInstance().sendCrashReport("file_manager_append_crash.txt", e.stackTraceToString())
            e.printStackTrace()
            false
        }
    }

    fun listFilesInDirectory(): ArrayList<String> {
        val file = File(filesPath)
        val files = file.listFiles()!!
        val fileList = ArrayList<String>()
        for (f in files) {
            fileList += f.name
        }
        return fileList
    }


    fun doesFileExist(fileName: String): Boolean {
        val file = File("$filesPath/$fileName")
        return file.exists()
    }

    fun isFileEmpty(fileName: String): Boolean {
        val file = File("$filesPath/$fileName")
        return file.length() == 0L
    }

    fun write(context: Context, fileName: String, content: String): Boolean {
        return try {
            val writer = OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE))
            writer.write(content)
            writer.close()
            true
        } catch (e: IOException) {
            NetworkManager.getInstance().sendCrashReport("file_manager_write_crash.txt", e.stackTraceToString())
            return false
        }
    }

    fun read(context: Context, fileName: String): List<String>? {
        return try {
            val file = File("$filesPath/$fileName")
            if (file.exists()) {
                val inputStream = context.openFileInput(fileName)

                val inputReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputReader)

                bufferedReader.readLines()
            } else {
                file.createNewFile()
                arrayListOf()
            }
        } catch (e: Exception) {
            FileManager.write(context, "file_manager_read_crash.txt", e.stackTraceToString())
//            NetworkManager.getInstance().sendCrashReport("file_manager_read_crash.txt", e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    fun getFile(fileName: String): File {
        return File("$filesPath/$fileName")
    }

    fun delete(fileName: String) {
        println("Trying to delete: ${filesPath}/$fileName")
        File("$filesPath/$fileName").delete()
    }

}