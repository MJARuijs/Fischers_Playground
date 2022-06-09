package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import com.mjaruijs.fischersplayground.opengl.model.MeshData
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

object OBJLoader {

    private val cache = HashMap<Int, MeshData>()
    private val currentlyLoading = HashMap<Int, AtomicBoolean>()

    fun get(context: Context, location: Int): MeshData {
        if (cache.contains(location)) {
            return cache[location]!!
        }

        return if (currentlyLoading.containsKey(location)) {
//            println("CONTAINS $location")
            try {
                while (currentlyLoading[location]!!.get()) {
                    Thread.sleep(1)
                }
            } catch (e: NullPointerException) {

            }

            currentlyLoading.remove(location)
            cache[location]!!
        } else {
//            println("LOADING MESH AGAIN $location")

            val mesh = load(context, location)
            cache[location] = mesh
            mesh
        }
    }

    fun preload(context: Context, location: Int, name: String) {
        if (cache.containsKey(location)) {
            return
        }

//        println("PRELOADING: $location $name")
        currentlyLoading[location] = AtomicBoolean(true)
        val startTime = System.nanoTime()

        val mesh = load(context, location, name)

        val endTime = System.nanoTime()
        cache[location] = mesh

        currentlyLoading[location]?.set(false)
//        println("DONE LOADING $location $name ${(endTime - startTime) / 1000000}")
    }

    private fun load(context: Context, fileLocation: Int, name: String = ""): MeshData {
        val inputStream = context.resources.openRawResource(fileLocation)
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

        reader.close()

        return MeshData(vertexData, normalData, textureData)
    }
}