package com.mjaruijs.fischersplayground.opengl.shaders

import android.content.Context
import java.io.BufferedInputStream

object ShaderLoader {

    fun load(fileLocation: Int, type: ShaderType, context: Context): Shader {
        val inputStream = context.resources.openRawResource(fileLocation)
        val reader = BufferedInputStream(inputStream)
        val content = ByteArray(reader.available())

        while (reader.available() > 0) {
            reader.read(content)
        }

        return Shader(type, String(content))
    }
}