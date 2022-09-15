package com.mjaruijs.fischersplayground.opengl.model

import android.content.res.Resources
import com.mjaruijs.fischersplayground.opengl.OBJLoader

object MeshLoader {

    private val meshes = HashMap<Int, Mesh>()

    fun preload(resources: Resources, resourceId: Int): Mesh {
//        if (meshes.containsKey(resourceId)) {
//            return meshes[resourceId]!!
//        }

        val meshData = OBJLoader.get(resources, resourceId)
        val mesh = Mesh(meshData)
        meshes[resourceId] = mesh

        return mesh
    }

    fun get(resourceId: Int): Mesh {
        if (meshes.containsKey(resourceId)) {
            return meshes[resourceId]!!
        }

        throw IllegalArgumentException("No mesh was found for resourceId: $resourceId")
    }

}