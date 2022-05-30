package com.mjaruijs.fischersplayground.math.vectors

import kotlin.math.abs

data class Vector3(var x: Float = 0.0f, var y: Float = 0.0f, var z: Float = 0.0f): Vector<Vector3> {

    constructor(x: Int, y: Int, z: Int): this(x.toFloat(), y.toFloat(), z.toFloat())

    constructor(vector: Vector2, z: Float = 0.0f): this(vector.x, vector.y, z)

    constructor(vector: Vector3): this(vector.x, vector.y, vector.z)

    constructor(vector: Vector4): this(vector.x, vector.y, vector.z)

    constructor(elements: FloatArray): this(elements[0], elements[1], elements[2])

    fun xy() = Vector2(x, y)

    fun yz() = Vector2(y, z)

    fun xz() = Vector2(x, z)

    operator fun minus(factor: Float) = Vector3(x - factor, y - factor, z - factor)

    override operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    override operator fun set(index: Int, value: Float) = when (index) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    override operator fun unaryMinus() = Vector3(-x, -y, -z)

    override operator fun plus (other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)

    override operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)

    override operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)

    override operator fun times(factor: Float) = Vector3(x * factor, y * factor, z * factor)

    override operator fun times(factor: Int) = Vector3(x * factor, y * factor, z * factor)

    override operator fun div(other: Vector3) = Vector3(x / other.x, y / other.y, z / other.z)

    override operator fun div(factor: Float) = times(1.0f / factor)

    override infix fun dot(other: Vector3) = (x * other.x) + (y * other.y) + (z * other.z)

    override fun absolute() = Vector3(abs(x), abs(y), abs(z))


    /**
     * @param other the vector to be multiplied by.
     * @return the cross-product of this vector and the provided vector.
     */
    infix fun cross(other: Vector3) = Vector3(
            (y * other.z) - (z * other.y),
            (z * other.x) - (x * other.z),
            (x * other.y) - (y * other.x)
    )

    override fun normalize() {
        val length = length()
        x /= length
        y /= length
        z /= length
    }

    override fun toString() = "<$x, $y, $z>"

    override fun toArray() = floatArrayOf(x, y, z)

    companion object {

        fun fromString(string: String): Vector3 {
            val values = string.removePrefix("<").removeSuffix(">").replace(",", "").split(' ')
            val x = values[0].toFloat()
            val y = values[1].toFloat()
            val z = values[2].toFloat()
            return Vector3(x, y, z)
        }

    }

}