package com.mjaruijs.fischersplayground.opengl.texture

import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLES30.GL_RGBA8
import android.opengl.GLES30.glTexStorage2D
import android.opengl.GLUtils
import java.nio.ByteBuffer
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

class Texture(private val bitmap: Bitmap, val pixelData: ByteBuffer, private val name: String? = null) {

    var handle = -1
        private set

    val width = bitmap.width
    val height = bitmap.height

    fun init() {
        val textureHandle = IntArray(1)
        glGenTextures(1, textureHandle, 0)
        handle = textureHandle[0]

//        if (name != null) {
//            println("LOADING $name: $handle")
//        }

        val size = min(width, height).toFloat()
        val levels = max(1, log2(size).toInt())

        glBindTexture(GL_TEXTURE_2D, handle)
        glTexStorage2D(GL_TEXTURE_2D, levels, GL_RGBA8, width, height)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelData)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

//        if (loadResource) {
//            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
//        }
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun destroy() {
        glDeleteTextures(1, intArrayOf(handle), 0)
    }

}