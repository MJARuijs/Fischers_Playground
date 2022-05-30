package com.mjaruijs.fischersplayground.opengl

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import kotlin.math.PI
import kotlin.math.tan

class Camera(
    var fieldOfView: Float = 45.0f,
    private var aspectRatio: Float = 1.0f,
    private var zNear: Float = 0.1f,
    private var zFar: Float = 1000.0f,
    var zoom: Float = 4.0f,
    var rotation: Vector3 = Vector3()
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
            .translate(-Vector3(0f, 0f, zoom))
            .rotate(rotation)

    fun getPosition() = viewMatrix.inverse().getPosition()

    val rotationMatrix: Matrix4
        get() = Matrix4().rotateY(-rotation.y).rotateX(-rotation.x)

    fun zoom(distance: Float) {
        zoom = distance

        if (zoom < 1.0f) {
            zoom = 1.0f
        }

        if (zoom > 10.0f) {
            zoom = 10.0f
        }
    }
}