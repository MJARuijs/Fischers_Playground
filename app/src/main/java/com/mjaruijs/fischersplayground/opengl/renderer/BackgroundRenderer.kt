package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader

class BackgroundRenderer(resources: Resources) {

    private val quad = Quad()
    private val sampler = Sampler(0)
    private val texture = TextureLoader.load(resources, R.drawable.chess_background)

    private val background2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.background_2d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.background_2d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val background3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.background_3d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.background_3d_fragment, ShaderType.FRAGMENT, resources)
    )

    init {
        texture.init()
    }

    fun render2D(aspectRatio: Float) {
        background2DProgram.start()
        background2DProgram.set("aspectRatio", aspectRatio)
        background2DProgram.set("textureMap", sampler.index)
        sampler.bind(texture)
        quad.draw()
        background2DProgram.stop()
    }

    fun render3D(camera: Camera, aspectRatio: Float) {
        background3DProgram.start()
        background3DProgram.set("projection", camera.projectionMatrix)
        background3DProgram.set("view", camera.viewMatrix)

        background3DProgram.stop()
    }

    fun destroy() {
        background3DProgram.destroy()
        background2DProgram.destroy()
    }
}