package com.mjaruijs.fischersplayground.math.vectors

import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.util.FloatUtils
import kotlin.math.abs

data class Vector2(var x: Float = 0.0f, var y: Float = 0.0f): Vector<Vector2> {

    constructor(vector2: Vector2) : this(vector2.x, vector2.y)
    
    constructor(x: Int, y: Int): this(x.toFloat(), y.toFloat())

    override operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    override operator fun set(index: Int, value: Float) = when (index) {
        0 -> x = value
        1 -> y = value
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    override operator fun unaryMinus() = Vector2(-x, -y)

    override operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)

    override operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)

    override operator fun times(other: Vector2) = Vector2(x * other.x, y * other.y)

    override operator fun div(other: Vector2) = Vector2(x / other.x, y / other.y)

    override operator fun times(factor: Float) = Vector2(x * factor, y * factor)

    override operator fun times(factor: Int) = Vector2(x * factor, y * factor)

    override operator fun div(factor: Float) = Vector2(x / factor, y / factor)

    operator fun minus(factor: Float) = Vector2(x - factor, y - factor)

    override infix fun dot(other: Vector2) = (x * other.x) + (y * other.y)

    /**
     * @return the conjugate (orthogonal/perpendicular) of the vector.
     */
    fun conjugate() = Vector2(y, -x)

    override fun absolute() = Vector2(abs(x), abs(y))

    override fun normalize() {
        val norm = norm()
        if (x != 0.0f) {
            x /= norm
        }
        if (y != 0.0f) {
            y /= norm
        }
    }
    
    fun roundToDecimal(n: Int): Vector2 {
        x = FloatUtils.roundToDecimal(x, n)
        y = FloatUtils.roundToDecimal(y, n)
        return this
    }

    override fun toString() = "<$x, $y>"

    override fun toArray() = floatArrayOf(x, y)

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (this === other) {
            return true
        }

        if (other !is Vector2) {
            return false
        }

        if (!FloatUtils.compare(x, other.x)) {
            return false
        }

        if (!FloatUtils.compare(y, other.y)) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {

        fun fromString(content: String): Vector2 {
            try {
                val startIndex = content.indexOf('<') + 1
                val separatorIndex = content.indexOf(',')
                val endIndex = content.indexOf('>')

                val x = content.substring(startIndex, separatorIndex).toFloat()
                val y = content.substring(separatorIndex + 2, endIndex).toFloat()
                return Vector2(x, y)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_vec_from_string.txt", e.stackTraceToString())
                throw IllegalArgumentException("Failed to parse the following string into a Vector2: $content")
            }

        }
    }

}
