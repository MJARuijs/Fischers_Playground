package com.mjaruijs.fischersplayground.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun saveBitmapToStorage(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            val root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val myDir = File("$root/images")
            myDir.mkdirs()

            val file = File(myDir, fileName)
            if (file.exists()) {
                file.delete()
            }

            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}