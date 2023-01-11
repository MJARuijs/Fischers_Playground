@file:Suppress("UNCHECKED_CAST")

package com.mjaruijs.fischersplayground.math.vectors

import android.os.Parcelable
import kotlin.math.sqrt

internal interface Vector<T: Vector<T>> : Parcelable {

    /**
     * Get the value from the component at provided index in the vector.
     * @param index the component's index.
     * @return the component's value.
     * @throws IndexOutOfBoundsException the component's index was out of bounds.
     */
    operator fun get(index: Int): Float

    /**
     * Set the value of the component at the provided index in the vector.
     * @param index the component's index.
     * @param value the component's value.
     * @throws IndexOutOfBoundsException the component's index was out of bounds.
     */
    operator fun set(index: Int, value: Float)

    /**
     * @return the vector multiplied by -1.
     */
    operator fun unaryMinus(): T

    /**
     * @param other the vector to be added.
     * @return the sum of the vectors.
     */
    operator fun plus(other: T): T

    /**
     * @param other the vector to be subtracted.
     * @return the difference between the vectors.
     */
    operator fun minus(other: T): T

    /**
     * @param other the vector to be multiplied by.
     * @return the element-wise product of the vectors.
     */
    operator fun times(other: T): T

    /**
     * @param other the vector to be divided by.
     * @return the element-wise fraction of the vectors.
     */
    operator fun div(other: T): T

    /**
     * @param factor the scalar multiplication.
     * @return the scalar multiple of the vector.
     */
    operator fun times(factor: Float): T

    /**
     * @param factor the scalar multiplication.
     * @return the scalar multiple of the vector.
     */
    operator fun times(factor: Int): T

    /**
     * @param factor the scalar division.
     * @return the scalar fraction of the vector.
     */
    operator fun div(factor: Float): T

    /**
     * @return the inverse (negative) of the vector.
     */
    fun inverse() = unaryMinus()

    /**
     * @param other the vector to be measured to.
     * @return the euclidean distance between the vectors.
     */
    infix fun distance(other: T) = minus(other).norm()

    /**
     * @param other the vector to be multiplied by.
     * @return the dot-product of the vectors.
     */
    infix fun dot(other: T): Float

    /**
     * @param other the vector to be multiplied by.
     * @return the scalar-product (dot-product) of the vectors.
     */
    fun scalarProduct(other: T) = dot(other)

    /**
     * @param other the vector to be multiplied by.
     * @return the inner-product (dot-product) of the vectors.
     */
    fun innerProduct(other: T) = dot(other)

    /**
     * @return the norm of the vector.
     */
    fun norm() = sqrt(dot(this as T))

    /**
     * @return the euclidean length (norm) of the vector.
     */
    fun length() = norm()

    /**
     * @return the size (norm) of the vector.
     */
    fun size() = norm()

    /**
     * @return the absolute value (norm) of the vector.
     */
    fun absolute(): T

    /**
     * @return the magnitude (norm) of the vector.
     */
    fun magnitude() = norm()

    /**
     * @return the modulus (norm) of the vector.
     */
    fun modulus() = norm()

    /**
     * Compute the normal vector by dividing the vector by its norm.
     * @return the normal vector.
     */
    fun normal() = div(norm())

    /**
     * @return the unit (normal) vector.
     */
    fun unit() = normal()

    /**
     * @return the direction (normal) vector.
     */
    fun direction() = normal()

    /**
     * Normalize the vector.
     */
    fun normalize()

    /**
     * @return a human-readable representation of the vector.
     */
    override fun toString(): String

    /**
     * @return an array containing the vector components.
     */
    fun toArray(): FloatArray
    
    /**
     * Compare the vectors based on their euclidean length.
     * @param other the vector to compare with.
     * @return the comparison's result.
     */
    operator fun compareTo(other: Vector3): Int {
        val difference = length() - other.length()
        return when {
            (difference > 0.0f) -> 1
            (difference < 0.0f) -> -1
            else -> 0
        }
    }

}
