package com.mjaruijs.fischersplayground.opengl.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import androidx.core.graphics.get
import java.nio.ByteBuffer

object TextureLoader {

    fun load(context: Context, resourceId: Int): Texture {
        val textureHandle = IntArray(1)
        glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inScaled = false
            bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, bitmapOptions)

            glBindTexture(GL_TEXTURE_2D, textureHandle[0])

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

            val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(pixelData)
//            bitmap.recycle()

            pixelData.rewind()

            return Texture(textureHandle[0], bitmap.width, bitmap.height, pixelData)
        } else {
            throw RuntimeException("Failed to load texture with ID: $resourceId")
        }

    }

}