package com.mjaruijs.fischersplayground.math

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quaternion(var w: Float = 1.0f, var x: Float = 0.0f, var y: Float = 0.0f, var z: Float = 0.0f) {

    constructor(quaternion: Quaternion): this(quaternion.w, quaternion.x, quaternion.y, quaternion.z)

    constructor(angles: Vector2): this(Quaternion(Axis.X, angles.x) * Quaternion(Axis.Y, angles.y))

    constructor(angles: Vector3): this(
        Quaternion(Axis.X, angles.x) * Quaternion(Axis.Y, angles.y) * Quaternion(
        Axis.Z, angles.z)
    )

    constructor(normal: Vector3, angle: Float): this(
            cos(angle / 2.0f),
            normal.x * sin(angle / 2.0f),
            normal.y * sin(angle / 2.0f),
            normal.z * sin(angle / 2.0f)
    )

    constructor(axis: Axis, angle: Float): this(axis.normal, angle)

    fun toMatrix(): Matrix4 {

        val normal = normal()

        val x = normal.x
        val y = normal.y
        val z = normal.z
        val w = normal.w

        return Matrix4(floatArrayOf(
                1.0f - 2.0f * y * y - 2.0f * z * z, 2.0f * x * y - 2.0f * z * w, 2.0f * x * z + 2.0f * y * w, 0.0f,
                2.0f * x * y + 2.0f * z * w, 1.0f - 2.0f * x * x - 2.0f * z * z, 2.0f * y * z - 2.0f * x * w, 0.0f,
                2.0f * x * z - 2.0f * y * w, 2.0f * y * z + 2.0f * x* w, 1.0f - 2.0f * x * x - 2.0f * y * y, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        ))
    }

    operator fun unaryMinus() = Quaternion(-w, -x, -y, -z)

    operator fun plus(other: Quaternion) = Quaternion(w + other.w, x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Quaternion) = Quaternion(w - other.w, x - other.x, y - other.y, z - other.z)

    operator fun times(other: Quaternion) = Quaternion(
            ((w * other.w) - (x * other.x) - (y * other.y) - (z * other.z)),
            ((w * other.x) + (x * other.w) + (y * other.z) - (z * other.y)),
            ((w * other.y) - (x * other.z) + (y * other.w) + (z * other.x)),
            ((w * other.z) + (x * other.y) - (y * other.x) + (z * other.w))
    )

    operator fun times(vector: Vector3): Vector3 {
        val point = Quaternion(0.0f, vector.x, vector.y, vector.z)
        val unit = unit()
        val result = (unit * point) * unit.conjugate()
        return Vector3(result.x, result.y, result.z)
    }

    operator fun times(factor: Float) = Quaternion(w * factor, x * factor, y * factor, z * factor)

    operator fun div(factor: Float) = Quaternion(w / factor, x / factor, y * factor, z / factor)

    fun dot(other: Quaternion) = (w * other.w) + (x * other.x) + (y * other.y) + (z * other.z)

    fun conjugate() = Quaternion(w, -x, -y, -z)

    fun transposition() = conjugate()

    fun norm() = sqrt(dot(this))

    fun length() = norm()

    fun size() = norm()

    fun magnitude() = norm()

    fun absolute() = norm()

    fun modulus() = norm()

    fun versor() = div(norm())

    fun normal() = versor()

    fun unit() = versor()

    fun normalize() {
        val length = length()
        w /= length
        x /= length
        y /= length
        z /= length
    }

    fun reciprocal() = conjugate() / norm()

    fun inverse() = reciprocal()

    /**
     * @return the string representation of the quaternion.
     */
    override fun toString() = "<$w, $x, $y, $z>"

    companion object {

        fun interpolate(a: Quaternion, b: Quaternion, progress: Float): Quaternion {
            val dot = a.dot(b)
            val remaining = 1f - progress
            return if (dot < 0) {
                (a * remaining + b * -progress).normal()
            } else {
                (a * remaining + b * progress).normal()
            }
        }

        fun fromMatrix(matrix: Matrix4): Quaternion {
            val m00 = matrix[0, 0]
            val m11 = matrix[1, 1]
            val m22 = matrix[2, 2]
            val trace = m00 + m11 + m22

            return when {
                trace > 0 -> {
                    val s = sqrt(trace + 1.0f) * 2
                    val w = 0.25f * s
                    val x = (matrix[2, 1] - matrix[1, 2]) / s
                    val y = (matrix[0, 2] - matrix[2, 0]) / s
                    val z = (matrix[1, 0] - matrix[0, 1]) / s
                    Quaternion(w, x, y, z)
                }
                m00 > m11 && m00 > m22 -> {
                    val s = sqrt(1.0f + m00 - m11 - m22) * 2
                    val w = (matrix[2, 1] - matrix[1, 2]) / s
                    val x = 0.25f * s
                    val y = (matrix[0, 1] + matrix[1, 0]) / s
                    val z = (matrix[0, 2] + matrix[2, 0]) / s
                    Quaternion(w, x, y, z)
                }
                m11 > m22 -> {
                    val s = sqrt(1.0f + m11 - m00 - m22) * 2
                    val w = (matrix[0, 2] - matrix[2, 0]) / s
                    val x = (matrix[0, 1] + matrix[1, 0]) / s
                    val y = 0.25f * s
                    val z = (matrix[1, 2] + matrix[2, 1]) / s
                    Quaternion(w, x, y, z)
                }
                else -> {
                    val s = sqrt(1.0f + m22 - m00 - m11) * 2
                    val w = (matrix[1, 0] - matrix[0, 1]) / s
                    val x = (matrix[0, 2] + matrix[2, 0]) / s
                    val y = (matrix[1, 2] + matrix[2, 1]) / s
                    val z = 0.25f * s
                    Quaternion(w, x, y, z)
                }
            }

        }

    }

}