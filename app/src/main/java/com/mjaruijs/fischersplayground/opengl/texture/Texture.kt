package com.mjaruijs.fischersplayground.opengl.texture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLUtils
import java.nio.ByteBuffer

class Texture(private val bitmap: Bitmap, val pixelData: ByteBuffer) {

    var handle = -1
        private set

    val width = bitmap.width
    val height = bitmap.height

    fun init() {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        handle = textureHandle[0]

        if (textureHandle[0] != 0) {
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inScaled = false
            bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(pixelData)
//            bitmap.recycle()

            pixelData.rewind()
        }
    }

    fun destroy() {
        glDeleteTextures(1, intArrayOf(handle), 0)
    }

}