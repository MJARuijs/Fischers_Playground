package com.mjaruijs.fischersplayground.util

import android.content.Context
import com.mjaruijs.fischersplayground.services.NetworkService
import java.io.*

object FileManager {

    private var filesPath = ""

    fun init(context: Context) {
        filesPath = context.filesDir.absolutePath
    }

    fun append(context: Context, fileName: String, content: String): Boolean {
        return try {
            val currentLines = readLines(context, fileName) ?: return write(context, fileName, content)
            var currentContent = ""
            for (line in currentLines) {
                currentContent += "$line\n"
            }
            val newContent = "$currentContent\n$content"
            return write(context, fileName, newContent)
        } catch (e: Exception) {
            NetworkService.sendCrashReport("crash_file_manager_append.txt", e.stackTraceToString(), context)
            e.printStackTrace()
            false
        }
    }

    fun listFilesInDirectory(dir: String = ""): ArrayList<String> {
        val path = if (dir.isBlank()) filesPath else "$filesPath/$dir"

        val file = File(path)
        val files = file.listFiles()!!
        val fileList = ArrayList<String>()
        for (f in files) {
            fileList += f.name
        }
        return fileList
    }


    fun mkdir(dir: String): Boolean {
        val dirFile = File("$filesPath/$dir")
//        Logger.debug("MyTag", "Can write to dir: ${dirFile.canWrite()} ${dirFile.isDirectory}")
        return dirFile.mkdir()
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
            NetworkService.sendCrashReport("crash_file_manager_write.txt", e.stackTraceToString(), context)
            return false
        }
    }

    fun readLines(context: Context, fileName: String): List<String>? {
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
            write(context, "file_manager_read_crash.txt", e.stackTraceToString())
//            NetworkManager.getInstance().sendCrashReport("file_manager_read_crash.txt", e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    fun readText(context: Context, fileName: String): String? {
        return try {
            val file = File("$filesPath/$fileName")
            if (file.exists()) {
                val inputStream = context.openFileInput(fileName)

                val inputReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputReader)
                bufferedReader.readText()
            } else {
                file.createNewFile()
                ""
            }
        } catch (e: Exception) {
            write(context, "file_manager_read_crash.txt", e.stackTraceToString())
//            NetworkManager.getInstance().sendCrashReport("file_manager_read_crash.txt", e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    fun getFile(fileName: String): File {
        return File("$filesPath/$fileName")
    }
//
//    fun getFiles(dir: String) {
//        val filesDir = File("$filesPath/$dir")
//        filesDir.
//    }

    fun delete(fileName: String) {
        println("Trying to delete: ${filesPath}/$fileName")
        File("$filesPath/$fileName").delete()
    }

}