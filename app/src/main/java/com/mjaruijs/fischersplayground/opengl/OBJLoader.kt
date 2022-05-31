package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import com.mjaruijs.fischersplayground.opengl.model.MeshData
import java.io.BufferedInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

object OBJLoader {

    private val cache = HashMap<Int, MeshData>()
    private val currentlyLoading = HashMap<Int, AtomicBoolean>()

    fun get(context: Context, location: Int): MeshData {
        if (cache.contains(location)) {
            return cache[location]!!
        }

        return if (currentlyLoading.containsKey(location)) {
            println("CONTAINS $location")
            try {
                while (currentlyLoading[location]!!.get()) {
                    Thread.sleep(1)
                }
            } catch (e: NullPointerException) {

            }

            currentlyLoading.remove(location)
            cache[location]!!
        } else {
            println("LOADING MESH AGAIN $location")

            val mesh = load(context, location)
            cache[location] = mesh
            mesh
        }
    }

    fun preload(context: Context, location: Int) {
        if (cache.containsKey(location)) {
            return
        }

        println("PRELOADING: $location")
        currentlyLoading[location] = AtomicBoolean(true)

        val mesh = load(context, location)
        cache[location] = mesh

        currentlyLoading[location]?.set(false)
        println("DONE LOADING $location")
    }

    private fun load(context: Context, fileLocation: Int): MeshData {
        val vertices: ArrayList<Float> = ArrayList()
        val normals: ArrayList<Float> = ArrayList()
        val textures: ArrayList<Float> = ArrayList()
        val faces: ArrayList<String> = ArrayList()

        try {
            val inputStream = context.resources.openRawResource(fileLocation)
            val reader = BufferedInputStream(inputStream)

            val content = ByteArray(reader.available())

            while (reader.available() > 0) {
                reader.read(content)
            }

            val lines = String(content).split('\n')

            for (line in lines) {

                if (line.isBlank()) {
                    continue
                }

                val parts = line.replace("  ", " ").split(' ').toList()

                when (parts[0]) {
                    "v" -> {
                        // vertices
                        vertices.add(java.lang.Float.valueOf(parts[1]))
                        vertices.add(java.lang.Float.valueOf(parts[2]))
                        vertices.add(java.lang.Float.valueOf(parts[3]))
                    }
                    "vt" -> {
                        // textures
                        textures.add(java.lang.Float.valueOf(parts[1]))
                        textures.add(java.lang.Float.valueOf(parts[2]))
                    }
                    "vn" -> {
                        // normals
                        normals.add(java.lang.Float.valueOf(parts[1]))
                        normals.add(java.lang.Float.valueOf(parts[2]))
                        normals.add(java.lang.Float.valueOf(parts[3]))
                    }
                    "f" -> {
                        // faces: vertex/texture/normal
                        faces.add(parts[1])
                        faces.add(parts[2])
                        faces.add(parts[3])
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

//        val numFaces = faces.size

//        val normalsArray = FloatArray(numFaces * 3)
//        val textureCoordinates = FloatArray(numFaces * 2)
//        val positions = FloatArray(numFaces * 3)

        var positionIndex = 0
        var normalIndex = 0
//        var textureIndex = 0

        var vertexData = FloatArray(faces.size * 3)
        var normalData = FloatArray(faces.size * 3)

        var maxX = -Float.MAX_VALUE
        var minX = Float.MAX_VALUE

        var indices = IntArray(faces.size)
//        var i = 0f

//        indices.fill(i++, 0, faces.size)

        for ((i, face) in faces.withIndex()) {
//            println("Face: $face")
            val parts = face.split("/").toTypedArray()
            var index = 3 * (parts[0].toInt() - 1)

            val xPosition = vertices[index++]
            val yPosition = vertices[index++]
            val zPosition = vertices[index]

            if (xPosition > maxX) {
                maxX = xPosition
            }

            if (xPosition < minX){
                minX = xPosition
            }

//            vertexData += xPosition
//            vertexData += yPosition
//            vertexData += zPosition

            vertexData[positionIndex++] = xPosition
            vertexData[positionIndex++] = yPosition
            vertexData[positionIndex++] = zPosition

//            i++
//            i++
//            i++

            index = 2 * (parts[1].toInt() - 1)
//            textureCoordinates[normalIndex++] = textures.get(index++)
            // NOTE: Bitmap gets y-inverted
//            textureCoordinates[normalIndex++] = 1 - textures.get(index)

//            val xTexture = textures[index++]
//            val yTexture = textures[index]
//            vertexData += xTexture
//            vertexData += yTexture

            index = 3 * (parts[2].trim().toInt() - 1)
            val xNormal = normals[index++]
            val yNormal = normals[index++]
            val zNormal = normals[index]

            normalData[normalIndex++] = xNormal
            normalData[normalIndex++] = yNormal
            normalData[normalIndex++] = zNormal

            indices[i] = i
        }

        for (f in indices) {
//            println(f)
        }

//        println("DONE LOADING FILE $i")
        return MeshData(vertexData, normalData, indices)
    }
}