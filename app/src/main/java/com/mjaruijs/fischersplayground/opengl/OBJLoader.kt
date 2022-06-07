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

        val vertices: FloatArray
        val normals: FloatArray
        val textures: FloatArray
//        val faces: IntArray
        val faces: Array<String>

        val inputStream = context.resources.openRawResource(fileLocation)
        val reader = BufferedInputStream(inputStream)

        val content = ByteArray(reader.available())

        while (reader.available() > 0) {
            reader.read(content)
        }

        val lines = String(content).split('\n')

        val numberOfVertices = lines.count { line -> line.startsWith("v") }
        val numberOfNormals = lines.count { line -> line.startsWith("vn") }
        val numberOfTextureCoordinates = lines.count { line -> line.startsWith("vt") }
        val numberOfFaces = lines.count { line -> line.trim().startsWith("f") }

        vertices = FloatArray(numberOfVertices * 3)
        normals = FloatArray(numberOfNormals * 3)
        textures = FloatArray(numberOfTextureCoordinates * 2)
        faces = Array(numberOfFaces * 3) { "" }

        var vertexIndex = 0
        var normalIndex = 0
        var textureIndex = 0
        var faceIndex = 0

        for (line in lines) {

            if (line.isBlank()) {
                continue
            }

            val parts = line.replace("  ", " ").split(' ').toList()
            when (parts[0]) {
                "v" -> {
                    // vertices
                    vertices[vertexIndex++] = parts[1].toFloat()
                    vertices[vertexIndex++] = parts[2].toFloat()
                    vertices[vertexIndex++] = parts[3].toFloat()
                }
                "vt" -> {
                    // textures
                    textures[textureIndex++] = parts[1].toFloat()
                    textures[textureIndex++] = parts[2].toFloat()
                }
                "vn" -> {
                    // normals
                    normals[normalIndex++] = parts[1].toFloat()
                    normals[normalIndex++] = parts[2].toFloat()
                    normals[normalIndex++] = parts[3].toFloat()
                }
                "f" -> {
                    try {
                        // faces: vertex/texture/normal
                        faces[faceIndex++] = parts[1]
                        faces[faceIndex++] = parts[2]
                        faces[faceIndex++] = parts[3]
                    } catch (e: Exception) {
                        println("FAILED TO PARSE LINE: $line $numberOfFaces ${parts.size} ${faces.size} $faceIndex")
                        throw e
                    }

                }
            }
        }

        vertexIndex = 0
        normalIndex = 0
        textureIndex = 0

        val vertexData = FloatArray(faces.size * 3)
        val normalData = FloatArray(faces.size * 3)
        val textureData = FloatArray(faces.size * 2)

        val indices = IntArray(faces.size)

        for ((i, face) in faces.withIndex()) {
            val parts = face.split("/").toTypedArray()
            var index = 3 * (parts[0].toInt() - 1)

            val xPosition = vertices[index++]
            val yPosition = vertices[index++]
            val zPosition = vertices[index]

            vertexData[vertexIndex++] = xPosition
            vertexData[vertexIndex++] = yPosition
            vertexData[vertexIndex++] = zPosition

            index = 2 * (parts[1].toInt() - 1)
            val xTexture = textures[index++]
            val yTexture = textures[index]
            textureData[textureIndex++] += xTexture
            textureData[textureIndex++] += 1 - yTexture

            index = 3 * (parts[2].toInt() - 1)
            val xNormal = normals[index++]
            val yNormal = normals[index++]
            val zNormal = normals[index]

            normalData[normalIndex++] = xNormal
            normalData[normalIndex++] = yNormal
            normalData[normalIndex++] = zNormal

            indices[i] = i
        }

        return MeshData(vertexData, normalData, textureData, indices)
    }
}