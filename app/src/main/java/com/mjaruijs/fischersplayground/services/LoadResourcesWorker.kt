package com.mjaruijs.fischersplayground.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader
import com.mjaruijs.fischersplayground.util.Logger

class LoadResourcesWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    override fun doWork(): Result {
        val textureResources = inputData.getIntArray("texture_resources") ?: throw IllegalArgumentException("ResourceWorker is missing texture_resources")
        val modelResources = inputData.getIntArray("model_resources") ?: throw IllegalArgumentException("ResourceWorker is missing model_resources")

        val textureLoader = TextureLoader.getInstance()

        for (textureId in textureResources) {
            textureLoader.load(applicationContext.resources, textureId)
        }

        for (modelId in modelResources) {
            OBJLoader.preload(applicationContext.resources, modelId)
        }

        return Result.success()
    }
}