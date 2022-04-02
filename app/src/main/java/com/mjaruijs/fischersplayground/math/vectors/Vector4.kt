package com.mjaruijs.fischersplayground.math.vectors

import kotlin.math.abs

data class Vector4(var x: Float = 0.0f, var y: Float = 0.0f, var z: Float = 0.0f, var w: Float = 0.0f):
    Vector<Vector4> {

    constructor(x: Int, y: Int, z: Int, w: Int): this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    constructor(vector: Vector2, z: Float = 0.0f, w: Float = 1.0f): this(vector.x, vector.y, z, w)

    constructor(vector: Vector3, w: Float = 1.0f): this(vector.x, vector.y, vector.z, w)

    constructor(vector: Vector4): this(vector.x, vector.y, vector.z, vector.w)

    constructor(a: Vector2, b: Vector2): this(a.x, a.y, b.x, b.y)

    fun xy() = Vector2(x, y)

    fun xz() = Vector2(x, z)

    fun xyz() = Vector3(x, y, z)

    fun zw() = Vector2(z, w)

    override fun get(index: Int): Float {
        return when (index) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException("Invalid index for Vector4: $index")
        }
    }

    override fun set(index: Int, value: Float) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IllegalArgumentException("Invalid index for Vector4: $index")
        }
    }

    override fun unaryMinus() = Vector4(-x, -y, -z, -w)

    override fun plus(other: Vector4) = Vector4(x + other.x, y + other.y, z + other.z, w + other.w)

    override fun minus(other: Vector4) = Vector4(x - other.x, y - other.y, z - other.z, w - other.w)

    override fun times(other: Vector4) = Vector4(x * other.x, y * other.y, z * other.z, w * other.w)

    override fun div(other: Vector4) = Vector4(x / other.x, y / other.y, z / other.z, w / other.w)

    override fun times(factor: Float) = Vector4(x * factor, y * factor, z * factor, w * factor)

    override fun times(factor: Int) = Vector4(x * factor, y * factor, z * factor, w * factor)

    override fun div(factor: Float) = Vector4(x / factor, y / factor, z / factor, w / factor)

    override fun dot(other: Vector4) = (x * x) + (y * y) + (z* z) + (w * w)

    override fun absolute() = Vector4(abs(x), abs(y), abs(z), abs(w))

    override fun normalize() {
        val norm = norm()
        x /= norm
        y /= norm
        z /= norm
        w /= norm
    }

    override fun toString() = "<$x, $y, $z, $w>"

    override fun toArray() = floatArrayOf(x, y, z, w)

    fun fromString(string: String, delimiter: String = ","): Vector4 {
        val values = string.split(delimiter)
        return Vector4(values[0].toFloat(), values[1].toFloat(), values[2].toFloat(), values[3].toFloat())
    }

}
