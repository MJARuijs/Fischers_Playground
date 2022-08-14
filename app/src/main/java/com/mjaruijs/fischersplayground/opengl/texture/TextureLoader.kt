package com.mjaruijs.fischersplayground.opengl.texture

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.nio.ByteBuffer

object TextureLoader {

    fun load(resources: Resources, resourceId: Int, name: String? = null): Texture {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeResource(resources, resourceId, bitmapOptions)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()
//        bitmap.recycle()

        return Texture(bitmap, pixelData, name)
    }

    fun loadFromBitmap(resources: Resources, resourceId: Int): Texture {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Bitmap is null")
        val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()

        return Texture(bitmap, pixelData)
    }

}
