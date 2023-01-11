package com.mjaruijs.fischersplayground.opengl

import android.content.res.Resources
import com.mjaruijs.fischersplayground.opengl.model.MeshData
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

object OBJLoader {

    private val cache = HashMap<Int, MeshData>()
    private val currentlyLoading = HashMap<Int, AtomicBoolean>()

    fun get(resources: Resources, location: Int): MeshData {
        if (cache.contains(location)) {
            return cache[location]!!
        }

        return if (currentlyLoading.containsKey(location)) {
            try {
                while (currentlyLoading[location]!!.get()) {
                    Thread.sleep(1)
                }
            } catch (e: NullPointerException) {
                throw e
            }

            currentlyLoading.remove(location)
            cache[location]!!
        } else {
            val mesh = load(resources, location)
            cache[location] = mesh
            mesh
        }
    }

    fun preload(resources: Resources, location: Int): MeshData {
        if (cache.containsKey(location)) {
            return cache[location]!!
        }
        currentlyLoading[location] = AtomicBoolean(true)

        val mesh = load(resources, location)
        cache[location] = mesh
        currentlyLoading[location]?.set(false)

        return mesh
    }

    private fun load(resources: Resources, fileLocation: Int): MeshData {
        val inputStream = resources.openRawResource(fileLocation)
        val reader = BufferedInputStream(inputStream)
        val bytes = reader.readBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val numberOfVertices = buffer.int
        val numberOfTextures = buffer.int
        val numberOfNormals = buffer.int

        val vertexData = FloatArray(numberOfVertices)
        val textureData = FloatArray(numberOfTextures)
        val normalData = FloatArray(numberOfNormals)

        for (i in 0 until numberOfVertices) {
            vertexData[i] = buffer.float
        }

        for (i in 0 until numberOfTextures) {
            textureData[i] = buffer.float
        }

        for (i in 0 until numberOfNormals) {
            normalData[i] = buffer.float
        }

        inputStream.close()
        reader.close()

        return MeshData(vertexData, normalData, textureData)
    }
}