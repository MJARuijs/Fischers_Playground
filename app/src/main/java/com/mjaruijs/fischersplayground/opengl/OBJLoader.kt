package com.mjaruijs.fischersplayground.opengl

import android.content.Context
import com.mjaruijs.fischersplayground.opengl.model.MeshData
import java.io.BufferedInputStream
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

    fun preload(context: Context, location: Int, name: String) {
        if (cache.containsKey(location)) {
            return
        }

        println("PRELOADING: $location $name")
        currentlyLoading[location] = AtomicBoolean(true)
        val startTime = System.nanoTime()

        val mesh = load(context, location, name)

        val endTime = System.nanoTime()
        cache[location] = mesh

        currentlyLoading[location]?.set(false)
        println("DONE LOADING $location $name ${(endTime - startTime) / 1000000}")
    }

    private fun load(context: Context, fileLocation: Int, name: String = ""): MeshData {
        val vertices: FloatArray
        val normals: FloatArray
        val textures: FloatArray
        val faces: Array<String>

//        var startTime = System.nanoTime()

        val inputStream = context.resources.openRawResource(fileLocation)
        val reader = BufferedInputStream(inputStream)

        val content = ByteArray(reader.available())

        while (reader.available() > 0) {
            reader.read(content)
        }

        val lines = String(content).split('\n')

//        var endTime = System.nanoTime()
//        println("$name Reading file: ${(endTime - startTime) / 1000000}")

//        startTime = System.nanoTime()
        val numberOfVertices = lines.count { line -> line.startsWith("v ") }
        val numberOfNormals = lines.count { line -> line.startsWith("vn") }
        val numberOfTextureCoordinates = lines.count { line -> line.startsWith("vt") }
        val numberOfFaces = lines.count { line -> line.trim().startsWith("f") }

//        endTime = System.nanoTime()
//        println("$name Counting lines: ${(endTime - startTime) / 1000000}")

        vertices = FloatArray(numberOfVertices * 3)
        normals = FloatArray(numberOfNormals * 3)
        textures = FloatArray(numberOfTextureCoordinates * 2)
        faces = Array(numberOfFaces * 3) { "" }

        var vertexIndex = 0
        var normalIndex = 0
        var textureIndex = 0
        var faceIndex = 0

//        var startTime = System.nanoTime()

        val vertexStartIndex = 0
        val vertexEndIndex = numberOfVertices

        val textureStartIndex = vertexEndIndex
        val textureEndIndex = textureStartIndex + numberOfTextureCoordinates

        val normalStartIndex = textureEndIndex
        val normalEndIndex = textureEndIndex + numberOfNormals

        val faceStartIndex = normalEndIndex
        val faceEndIndex = faceStartIndex + numberOfFaces

        val verticesParsed = AtomicBoolean(false)
        val texturesParsed = AtomicBoolean(false)
        val normalsParsed = AtomicBoolean(false)
        val facesParsed = AtomicBoolean(false)

        Thread {
            for (i in vertexStartIndex until vertexEndIndex) {
                val line = lines[i]
                val parts = line.split(' ')

                vertices[vertexIndex++] = parts[1].toFloat()
                vertices[vertexIndex++] = parts[2].toFloat()
                vertices[vertexIndex++] = parts[3].toFloat()
            }
            verticesParsed.set(true)
        }.start()

        Thread {
            for (i in textureStartIndex until textureEndIndex) {
                val line = lines[i]
                val parts = line.split(' ')

                textures[textureIndex++] = parts[1].toFloat()
                textures[textureIndex++] = parts[2].toFloat()
            }
            texturesParsed.set(true)
        }.start()


        Thread {
            for (i in normalStartIndex until normalEndIndex) {
                val line = lines[i]
                val parts = line.split(' ')

                try {
                    normals[normalIndex++] = parts[1].toFloat()
                    normals[normalIndex++] = parts[2].toFloat()
                    normals[normalIndex++] = parts[3].toFloat()
                } catch (e: Exception) {
                    println("COULD NOT PARSE LINE: $line $i")
                    throw e
                }
            }
            normalsParsed.set(true)
        }.start()

        Thread {
            for (i in faceStartIndex until faceEndIndex) {
                val line = lines[i]
                val parts = line.split(' ')

                try {
                    faces[faceIndex++] = parts[1]
                    faces[faceIndex++] = parts[2]
                    faces[faceIndex++] = parts[3]
                } catch (e: Exception) {
                    println("COULD NOT PARSE LINE: $line")
                    throw e
                }
            }

            facesParsed.set(true)
        }.start()


        while (!verticesParsed.get() || !texturesParsed.get() || !normalsParsed.get() || !facesParsed.get()) {
            Thread.sleep(0)
        }


        var vertexIndex2 = 0
        var normalIndex2 = 0
        var textureIndex2 = 0

        val vertexData = FloatArray(faces.size * 3)
        val normalData = FloatArray(faces.size * 3)
        val textureData = FloatArray(faces.size * 2)

//        startTime = System.nanoTime()

        for (face in faces) {
            val parts = face.split("/").toTypedArray()
            var index = 3 * (parts[0].toInt() - 1)

            try {
                val xPosition = vertices[index++]
                val yPosition = vertices[index++]
                val zPosition = vertices[index]

                vertexData[vertexIndex2++] = xPosition
                vertexData[vertexIndex2++] = yPosition
                vertexData[vertexIndex2++] = zPosition

                index = 2 * (parts[1].toInt() - 1)
                val xTexture = textures[index++]
                val yTexture = textures[index]
                textureData[textureIndex2++] += xTexture
                textureData[textureIndex2++] += 1 - yTexture

                index = 3 * (parts[2].toInt() - 1)
                val xNormal = normals[index++]
                val yNormal = normals[index++]
                val zNormal = normals[index]

                normalData[normalIndex2++] = xNormal
                normalData[normalIndex2++] = yNormal
                normalData[normalIndex2++] = zNormal
            } catch (e: Exception) {
                println("Face: $face")
                throw e
            }


        }

//        endTime = System.nanoTime()
//        println("$name parsing faces: ${(endTime - startTime) / 1000000}")
//        var endTime = System.nanoTime()
//        println("parsing $name: ${(endTime - startTime) / 1000000}")

        return MeshData(vertexData, normalData, textureData)
    }
}