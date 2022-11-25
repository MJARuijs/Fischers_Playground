package com.mjaruijs.fischersplayground.opengl.texture

import android.graphics.Bitmap
import java.nio.ByteBuffer

class TextureData(val bitmap: Bitmap, val pixelData: ByteBuffer) {

    val width = bitmap.width
    val height = bitmap.height

}