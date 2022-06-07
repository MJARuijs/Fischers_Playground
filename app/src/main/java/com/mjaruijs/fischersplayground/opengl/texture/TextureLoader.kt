package com.mjaruijs.fischersplayground.opengl.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer

object TextureLoader {

    fun load(context: Context, resourceId: Int): Texture {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, bitmapOptions)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()

        //        texture.init()

        return Texture(bitmap, pixelData)
    }

}
