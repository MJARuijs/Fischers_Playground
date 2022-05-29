package com.mjaruijs.fischersplayground.opengl

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import kotlin.math.PI
import kotlin.math.tan

class Camera(
    var fieldOfView: Float = 45.0f,
    var aspectRatio: Float = 1.0f,
    var zNear: Float = 0.01f,
    var zFar: Float = 1000.0f,

    var position: Vector3 = Vector3(),
    var rotation: Vector3 = Vector3(),
    var zoom: Vector3 = Vector3()
) {

    val projectionMatrix: Matrix4
        get() = Matrix4(floatArrayOf(
            1.0f / (aspectRatio * tan((PI.toFloat() / 180.0f) * fieldOfView / 2.0f)), 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f / tan((PI.toFloat() / 180.0f) * fieldOfView / 2.0f), 0.0f, 0.0f,
            0.0f, 0.0f, -(zFar + zNear) / (zFar - zNear), -(2.0f * zFar * zNear) / (zFar - zNear),
            0.0f, 0.0f, -1.0f, 0.0f
        ))

    val viewMatrix: Matrix4
        get() = Matrix4()
            .rotate(rotation)
            .translate(-position - zoom)

    val rotationMatrix: Matrix4
        get() = Matrix4().rotateY(-rotation.y).rotateX(-rotation.x)

    fun translate(translation: Vector3) {
        val rotationMatrix = Matrix4().rotateY(-rotation.y)
        position += rotationMatrix.dot(-translation.unit() * 0.00001f)

        if (position.z < 0.0f) {
            position.z = 0.0f
        }

    }
}