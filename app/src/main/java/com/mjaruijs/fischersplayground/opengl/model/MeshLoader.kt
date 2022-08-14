package com.mjaruijs.fischersplayground.opengl.model

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import com.mjaruijs.fischersplayground.opengl.OBJLoader

object MeshLoader {

    private val meshes = HashMap<Int, Pair<Thread, Mesh>>()

    fun preload(resources: Resources, resourceId: Int): Pair<Thread, Mesh> {

        val meshData = OBJLoader.preload(resources, resourceId)
//        println("Mesh created on thread: ${Thread.currentThread().id} | ${meshData.first.id}")

//        context.runOnUiThread {
            val mesh = Mesh(meshData.second)
            meshes[resourceId] = Pair(meshData.first, mesh)
//        }
        return Pair(Thread.currentThread(), mesh)
    }

    fun get(resourceId: Int): Pair<Thread, Mesh> {
        if (meshes.containsKey(resourceId)) {
            return meshes[resourceId]!!
        }

        throw IllegalArgumentException("No mesh was found for resourceId: $resourceId")
//        val meshData = OBJLoader.get(context, resourceId)
//        var mesh: Mesh? = null
//
////        context.runOnUiThread {
//            mesh = Mesh(meshData)
//            meshes[resourceId] = mesh
////        }
//
//        return (mesh as Mesh)
    }

}