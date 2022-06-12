package com.mjaruijs.fischersplayground.opengl.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.nio.ByteBuffer

object TextureLoader {

    fun load(context: Context, resourceId: Int, name: String? = null): Texture {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, bitmapOptions)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()
//        bitmap.recycle()

        return Texture(bitmap, pixelData, name)
    }

    fun loadFromBitmap(context: Context, resourceId: Int): Texture {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val drawable = ResourcesCompat.getDrawable(context.resources, resourceId, null) ?: throw IllegalArgumentException("Bitmap is null")
        val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()

        return Texture(bitmap, pixelData)
    }

}
