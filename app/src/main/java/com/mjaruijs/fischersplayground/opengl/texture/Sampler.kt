package com.mjaruijs.fischersplayground.opengl.texture

import android.opengl.GLES20.*

class Sampler(val index: Int) {

    fun bind(texture: Texture) {
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, texture.handle)
        glActiveTexture(GL_TEXTURE0)
    }

    fun unbind() {
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE0)
    }

}