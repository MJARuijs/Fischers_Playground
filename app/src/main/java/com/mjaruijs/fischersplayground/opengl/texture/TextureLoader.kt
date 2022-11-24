package com.mjaruijs.fischersplayground.opengl.texture

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

object TextureLoader {

    private val regularCache = ConcurrentHashMap<Int, Texture>()
    private val bitmapCache = ConcurrentHashMap<Int, Texture>()

    fun load(resources: Resources, resourceId: Int, name: String? = null): Texture {
        if (regularCache.contains(resourceId)) {
            return regularCache[resourceId]!!
        }

        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeResource(resources, resourceId, bitmapOptions)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()

        val texture = Texture(bitmap, pixelData)
        regularCache[resourceId] = texture
        return texture
    }

    fun loadFromBitmap(resources: Resources, resourceId: Int): Texture {
        if (bitmapCache.contains(resourceId)) {
            return bitmapCache[resourceId]!!
        }

        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Bitmap is null")
        val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()
//        bitmap.recycle()

        val texture = Texture(bitmap, pixelData)
        bitmapCache[resourceId] = texture
        return texture
    }

}
