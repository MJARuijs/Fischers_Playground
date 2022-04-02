package com.mjaruijs.fischersplayground.opengl.texture

import android.opengl.GLES20.glDeleteTextures

class Texture(val handle: Int) {

    fun destroy() {
        glDeleteTextures(1, intArrayOf(handle), 0)
    }

}