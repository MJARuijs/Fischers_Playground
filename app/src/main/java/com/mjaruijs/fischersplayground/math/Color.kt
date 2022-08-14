package com.mjaruijs.fischersplayground.math

import com.mjaruijs.fischersplayground.math.vectors.Vector3

/**
 * A color defined by w red, green, and blue color channels and an alpha value.
 * @param r the red color channel's value (default 0.0f)
 * @param g the green color channel's value (default 0.0f)
 * @param b the blue color channel's value (default 0.0f)
 * @param b the alpha value (default 1.0f)
 * @constructor
 */
data class Color(var r: Float = 0.0f, var g: Float = 0.0f, var b: Float = 0.0f, var a: Float = 1.0f) {

    constructor(color: Color) : this(color.r, color.g, color.b, color.a)

    constructor(r: Int, g: Int, b: Int, a: Int = 255): this(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f)

    constructor(rgb: Vector3, a: Float) : this(rgb[0], rgb[1], rgb[2], a)

    operator fun plus(other: Color) = Color(r + other.r, g + other.g, b + other.b, a + other.b)

    operator fun minus(other: Color) = Color(r - other.r, g - other.g, b - other.b, a - other.b)

    operator fun times(other: Color) = Color(r * other.r, g * other.g, b * other.b, a * other.b)

    operator fun times(factor: Float) = Color(r * factor, g * factor, b * factor, a * factor)

    operator fun div(factor: Float) = Color(r / factor, g / factor, b / factor, a / factor)
    
    operator fun unaryMinus() = Color(-r, -g, -b, a)

    operator fun get(i: Int): Float {
        return when(i) {
            0 -> r
            1 -> g
            2 -> b
            3 -> a
            else -> throw IndexOutOfBoundsException("Tried to access element at position $i, which doesn't exist in a Color..")
        }
    }

    operator fun set(i: Int, value: Float) {
        when(i) {
            0 -> r = value
            1 -> g = value
            2 -> b = value
            3 -> a = value
            else -> throw IndexOutOfBoundsException("Tried to set element at position $i, which doesn't exist in a Color..")
        }
    }

    operator fun plusAssign(other: Color) {
        r += other.r
        g += other.g
        b += other.b
        a += other.a
    }

    operator fun minusAssign(other: Color) {
        r -= other.r
        g -= other.g
        b -= other.b
        a -= other.a
    }

    fun toArray(): FloatArray = floatArrayOf(r, g, b, a)

    fun rgb() = Vector3(r, g, b)
    
    fun copy() = Color(r, g, b, a)

    companion object {

        val LIGHT_WHITE = Color(0.8f, 0.8f, 0.8f)

        val WHITE = Color(1f, 1f, 1f)
        val GREY = Color(0.5f, 0.5f, 0.5f)
        val DARK = Color(0.2f, 0.2f, 0.2f)

        val BLACK = Color(0f, 0f, 0f)


        fun average(first: Color, second: Color): Color = (first + second) / 2.0f

        fun mix(first: Color, second: Color, weight: Float) = (first * (1.0f - weight)) + (second * weight)
        
        fun fromString(string: String): Color {
            val values = string.split(',')
            return Color(values[0].toFloat(), values[1].toFloat(), values[2].toFloat(), values[3].toFloat())
        }

    }

}