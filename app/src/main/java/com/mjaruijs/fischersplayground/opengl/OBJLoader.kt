package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import android.content.res.Resources
import com.mjaruijs.fischersplayground.opengl.model.Mesh
import com.mjaruijs.fischersplayground.opengl.model.MeshData
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

object OBJLoader {

    private val cache = HashMap<Int, Pair<Thread, MeshData>>()
    private val currentlyLoading = HashMap<Int, AtomicBoolean>()

    fun get(resources: Resources, location: Int): Pair<Thread, MeshData> {
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

            val mesh = load(resources, location)
            cache[location] = Pair(Thread.currentThread(), mesh)
            Pair(Thread.currentThread(), mesh)
        }
    }

    fun preload(resources: Resources, location: Int): Pair<Thread, MeshData> {
        if (cache.containsKey(location)) {
            return cache[location]!!
        }

//        println("PRELOADING: $location $name")
        currentlyLoading[location] = AtomicBoolean(true)
//        val startTime = System.nanoTime()

        val mesh = load(resources, location)

//        val endTime = System.nanoTime()
        cache[location] = Pair(Thread.currentThread(), mesh)

        currentlyLoading[location]?.set(false)

        return Pair(Thread.currentThread(), mesh)
//        println("DONE LOADING $location $name ${(endTime - startTime) / 1000000}")
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

        reader.close()

        return MeshData(vertexData, normalData, textureData)
    }
}