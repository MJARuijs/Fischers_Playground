package com.mjaruijs.fischersplayground.opengl.texture

import android.opengl.GLES30.*
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

class TextureArray(textures: List<Texture>) {

    val handle: Int

    init {
        val buffer = IntArray(1)
        glGenTextures(1, buffer, 0)
        handle = buffer[0]

        val width = textures[0].width
        val height = textures[0].height

        val size = min(width, height).toFloat()
        val levels = max(1, log2(size).toInt())

        glBindTexture(GL_TEXTURE_2D_ARRAY, handle)
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, levels, GL_RGBA8, width, height, textures.size)

        for (i in textures.indices) {
            val texture = textures[i]
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, texture.width, texture.height, 1, GL_RGBA, GL_UNSIGNED_BYTE, texture.pixelData)
        }

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0)
    }

}