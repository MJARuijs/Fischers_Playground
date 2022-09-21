package com.mjaruijs.fischersplayground.opengl.texture

import android.opengl.GLES20.*
import android.opengl.GLES30.GL_TEXTURE_2D_ARRAY

class Sampler(val index: Int) {

    fun bind(texture: Texture) {
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, texture.handle)
        glActiveTexture(GL_TEXTURE0)
    }

    fun bind(textureArray: TextureArray) {
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArray.handle)
        glActiveTexture(GL_TEXTURE0)
    }

    fun unbind() {
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE0)
    }

}