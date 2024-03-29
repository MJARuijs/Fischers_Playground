package com.mjaruijs.fischersplayground.opengl

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import kotlin.math.PI
import kotlin.math.tan

class Camera(
    var fieldOfView: Float = 45.0f,
    var aspectRatio: Float = 1.0f,
    private var zNear: Float = 1f,
    private var zFar: Float = 1000.0f,
    private var zoom: Float = DEFAULT_ZOOM,
    var rotation: Vector3 = Vector3()
) {

    val projectionMatrix: Matrix4
        get() = Matrix4(floatArrayOf(
            1.0f / (aspectRatio * tan((PI.toFloat() / 180.0f) * fieldOfView / 2.0f)), 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f / tan((PI.toFloat() / 180.0f) * fieldOfView / 2.0f), 0.0f, 0.0f,
            0.0f, 0.0f, -(zFar + zNear) / (zFar - zNear), -(2.0f * zFar * zNear) / (zFar - zNear),
            0.0f, 0.0f, -1.0f, 0.0f
        ))

    fun getViewMatrix(useLargeZoom: Boolean): Matrix4 {
//        return if (useLargeZoom) Matrix4()
//            .translate(-Vector3(0f, 0f, zoom * 2f))
//            .rotate(rotation)
//        else {
//            Matrix4()
//                .translate(-Vector3(0f, 0f, zoom))
//                .rotate(rotation)
//        }
        return Matrix4()
                .translate(-Vector3(0f, 0f, zoom))
                .rotate(rotation)
    }

    val viewMatrix: Matrix4
        get() = Matrix4()
            .translate(-Vector3(0f, 0f, zoom))
            .rotate(rotation)

    fun getPosition() = viewMatrix.inverse().getPosition()

    val rotationMatrix: Matrix4
        get() = Matrix4().rotateY(-rotation.y).rotateX(-rotation.x)

    fun getZoom() = zoom

    fun setZoom(distance: Float) {
        zoom = distance

        if (zoom < 1.0f) {
            zoom = 1.0f
        }

        if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM
        }
    }

    fun incrementZoom(distance: Float) {
        zoom += distance

        if (zoom < 1.0f) {
            zoom = 1.0f
        }

        if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM
        }
    }

    companion object {
        const val DEFAULT_ZOOM = 3f
        const val MAX_ZOOM = 30f
    }
}