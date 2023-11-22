package com.mjaruijs.fischersplayground.opengl.texture

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES30
import com.mjaruijs.fischersplayground.util.Logger
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

class TextureLoader {

    companion object {

        private const val TAG = "TextureLoader"

        private var instance: TextureLoader? = null

        fun getInstance(): TextureLoader {
            if (instance == null) {
                instance = TextureLoader()
                return instance!!
            }

            return instance!!
        }
    }

    private val initializedTextureCache = ConcurrentHashMap<Int, Texture>()
    private val loadedTextureCache = ConcurrentHashMap<Int, TextureData>()
    private val bitmapCache = ConcurrentHashMap<Int, TextureData>()

    fun initTextures() {
        for (textureEntry in loadedTextureCache.entries) {
            val textureId = textureEntry.key
            val textureData = textureEntry.value

            val texture = initTexture(textureData)
            initializedTextureCache[textureId] = texture
        }
    }

    fun get(resources: Resources, resourceId: Int): Texture {
        if (initializedTextureCache.containsKey(resourceId)) {
            return initializedTextureCache[resourceId]!!
        }

        if (!loadedTextureCache.containsKey(resourceId)) {
            Logger.debug(TAG, "Texture was not found.. Loading and initializing now.. $resourceId")
            load(resources, resourceId)
        } else {
            Logger.debug(TAG, "Texture was not initialized.. Initializing now.. $resourceId")
        }

        val texture = initTexture(loadedTextureCache[resourceId]!!)
        initializedTextureCache[resourceId] = texture
        return texture
    }

    fun load(resources: Resources, resourceId: Int) {
        if (loadedTextureCache.containsKey(resourceId)) {
            return
        }

        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inScaled = false
        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeResource(resources, resourceId, bitmapOptions)
        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(pixelData)
        pixelData.rewind()

        Logger.debug(TAG, "Done loading texture: $resourceId")
        loadedTextureCache[resourceId] = TextureData(bitmap, pixelData)
    }

//    fun loadFromBitmap(resources: Resources, resourceId: Int): Texture {
//        if (bitmapCache.contains(resourceId)) {
//            return bitmapCache[resourceId]!!
//        }
//
//        val bitmapOptions = BitmapFactory.Options()
//        bitmapOptions.inScaled = false
//        bitmapOptions.outConfig = Bitmap.Config.ARGB_8888
//
//        val drawable = ResourcesCompat.getDrawable(resources, resourceId, null) ?: throw IllegalArgumentException("Bitmap is null")
//        val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
//        val pixelData = ByteBuffer.allocateDirect(bitmap.byteCount)
//        bitmap.copyPixelsToBuffer(pixelData)
//        pixelData.rewind()
////        bitmap.recycle()
//
//        val texture = Texture(bitmap, pixelData)
//        bitmapCache[resourceId] = texture
//        return texture
//    }

    private fun initTexture(textureData: TextureData): Texture {
        val width = textureData.width
        val height = textureData.height

        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        val handle = textureHandle[0]

        val size = min(width, height).toFloat()
        val levels = max(1, log2(size).toInt())

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle)
        GLES30.glTexStorage2D(GLES20.GL_TEXTURE_2D, levels, GLES30.GL_RGBA8, width, height)
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, textureData.pixelData)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        textureData.bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return Texture(handle, width, height, textureData.pixelData)
    }

}
