package com.mjaruijs.fischersplayground.math.vectors

import kotlin.math.abs

data class IVector3(var x: Int = 0, var y: Int = 0, var z: Int = 0) {

//
//    constructor(vector: Vector2, z: Float = 0.0f): this(vector.x, vector.y, z)
//
//    constructor(vector: IVector3): this(vector.x, vector.y, vector.z)
//
//    constructor(vector: Vector4): this(vector.x, vector.y, vector.z)
//
//    constructor(elements: FloatArray): this(elements[0], elements[1], elements[2])
//
    constructor(intList: List<Int>) : this(intList[0], intList[1], intList[2])

    fun xy() = Vector2(x, y)

    fun yz() = Vector2(y, z)

    fun xz() = Vector2(x, z)

    operator fun minus(factor: Int) = IVector3(x - factor, y - factor, z - factor)

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    operator fun set(index: Int, value: Int) = when (index) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IndexOutOfBoundsException("Vector component's index was out of bounds: $index")
    }

    operator fun unaryMinus() = IVector3(-x, -y, -z)

    operator fun plus (other: IVector3) = IVector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: IVector3) = IVector3(x - other.x, y - other.y, z - other.z)

    operator fun times(other: IVector3) = IVector3(x * other.x, y * other.y, z * other.z)

//    operator fun times(factor: Float) = IVector3(x * factor, y * factor, z * factor)

    operator fun times(factor: Int) = IVector3(x * factor, y * factor, z * factor)

    operator fun div(other: IVector3) = IVector3(x / other.x, y / other.y, z / other.z)

//    operator fun div(factor: Float) = times(1.0f / factor)

    infix fun dot(other: IVector3) = (x * other.x) + (y * other.y) + (z * other.z)

    fun absolute() = IVector3(abs(x), abs(y), abs(z))


    /**
     * @param other the vector to be multiplied by.
     * @return the cross-product of this vector and the provided vector.
     */
    infix fun cross(other: IVector3) = IVector3(
            (y * other.z) - (z * other.y),
            (z * other.x) - (x * other.z),
            (x * other.y) - (y * other.x)
    )

//    override fun normalize() {
//        val length = length()
//        x /= length
//        y /= length
//        z /= length
//    }

    override fun toString() = "<$x, $y, $z>"

    fun toArray() = intArrayOf(x, y, z)

    companion object {

        fun fromString(string: String): IVector3 {
            val values = string.removePrefix("<").removeSuffix(">").replace(",", "").split(' ')
            val x = values[0].toInt()
            val y = values[1].toInt()
            val z = values[2].toInt()
            return IVector3(x, y, z)
        }

    }

}