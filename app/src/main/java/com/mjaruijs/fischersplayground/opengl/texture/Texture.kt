package com.mjaruijs.fischersplayground.opengl.texture

import android.opengl.GLES20.glDeleteTextures
import java.nio.ByteBuffer

class Texture(val handle: Int, val width: Int, val height: Int, val pixelData: ByteBuffer) {

    fun init() {

    }

    fun destroy() {
        glDeleteTextures(1, intArrayOf(handle), 0)
    }

}