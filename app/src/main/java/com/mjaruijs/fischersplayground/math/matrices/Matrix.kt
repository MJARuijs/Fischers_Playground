package com.mjaruijs.fischersplayground.math.matrices

import java.util.*

/**
 * The matrix class provides a base class for n by n matrices.
 * @param dimensions the dimensions of the matrix.
 * @param elements the initial elements representing the matrix.
 */
abstract class Matrix<T: Matrix<T>>(private val dimensions: Int, var elements: FloatArray) {

    companion object {

        /**
         * @param dimensions the dimensions of the matrix.
         * @return an array of floating-point values representing the elements of an identity matrix of the requested
         * dimensions.
         */
        @JvmStatic
        protected fun generateIdentityElements(dimensions: Int) = FloatArray(dimensions * dimensions) {
            index -> if (index % (dimensions + 1) == 0) 1.0f else 0.0f
        }

    }

    private val size = dimensions * dimensions

    init {

        assert(size > 0) {
            "matrix must contain at least 1 element"
        }

        assert(elements.size == size) {
            "$dimensions by $dimensions matrix must contain $size elements"
        }
    }

    /**
     * @param elements the elements representing a matrix of the defined dimensions.
     * @return a matrix instantiated using the provided elements.
     */
    protected abstract fun create(elements: FloatArray = FloatArray(size) {
        index -> if (index % dimensions == 0) 1.0f else 0.0f
    }): T

    /**
     * @param index the element's index.
     * @return the element's value.
     */
    operator fun get(index: Int) = elements[index]

    /**
     * Update the value of an element.
     * @param index the element's index.
     * @param value the element's new value.
     */
    operator fun set(index: Int, value: Float) {
        elements[index] = value
    }

    /**
     * @param row the element's row index.
     * @param column the element's column index.
     * @return the element's value.
     */
    operator fun get(row: Int, column: Int) = elements[dimensions * row + column]

    /**
     * Update the value of an element.
     * @param row the element's row index.
     * @param column the element's column index.
     * @param value the element's new value.
     */
    operator fun set(row: Int, column: Int, value: Float) {
        elements[dimensions * row + column] = value
    }

    /**
     * @return the matrix element-wise multiplied by -1.
     */
    operator fun unaryMinus() = map(Float::unaryMinus)

    /**
     * @param matrix the matrix to be added.
     * @return the element-wise sum of the matrices.
     */
    operator fun plus(matrix: T) = map(matrix, Float::plus)

    /**
     * @param matrix the matrix to be subtracted.
     * @return the element-wise difference between the matrices.
     */
    operator fun minus(matrix: T) = map(matrix, Float::minus)

    /**
     * @param matrix the matrix to be multiplied by.
     * @return the element-wise product of the matrices.
     */
    operator fun times(matrix: T) = map(matrix, Float::times)

    /**
     * @param matrix the matrix to be divided by.
     * @return the element-wise fraction of the matrices.
     */
    operator fun div(matrix: T) = map(matrix, Float::div)

    /**
     * @param factor the scalar factor to be multiplied by.
     * @return the product of the matrix and the scalar.
     */
    operator fun times(factor: Float) = map(factor, Float::times)

    /**
     * @param factor the scalar factor to be divided by.
     * @return the fraction of the matrix and the scalar.
     */
    operator fun div(factor: Float) = map(factor, Float::div)

    /**
     * @param matrix the matrix to be multiplied by.
     * @return the dot-product of the matrices.
     */
    infix fun dot(matrix: T): T {
        val result = create()
        for (row in 0 until dimensions) {
            for (column in 0 until dimensions) {
                var sum = 0.0f
                for (index in 0 until dimensions) {
                     sum += this[row, index] * matrix[index, column]
                }
                result[row, column] = sum
            }
        }
        return result
    }

    /**
     * @return the transpose of the matrix.
     */
    fun transpose(): T {
        val result = create()
        for (row in 0 until dimensions) {
            for (column in 0 until dimensions) {
                result[row, column] = this[column, row]
            }
        }
        return result
    }

    private fun computeMinor(dimensions: Int, elements: FloatArray, row: Int, column: Int): Float {
        val minorElements = ArrayList<Float>()
        for (r in 0 until dimensions) {
            if (r != row) {
                for (c in 0 until dimensions) {
                    if (c != column) {
                        minorElements += elements[dimensions * r + c]
                    }
                }
            }
        }
        return computeDeterminant(dimensions - 1, minorElements.toFloatArray())
    }

    private fun computeDeterminant(dimensions: Int, elements: FloatArray): Float = when (dimensions) {
        0 -> 1.0f
        1 -> elements[0]
        2 -> (elements[0] * elements[3]) - (elements[1] * elements[2])
        else -> {
            var sum = 0.0f
            for (column in 0 until dimensions) {
                val sign = if (column % 2 == 0) 1.0f else -1.0f
                sum += sign * elements[column] * computeMinor(dimensions, elements, 0, column)
            }
            sum
        }
    }

    fun isZeroMatrix(): Boolean {
        if (elements.all { element -> element == 0f }) {
            return true
        }
        return false
    }

    fun determinant() = computeDeterminant(dimensions, elements)

    /**
     * @return the inverse of the matrix.
     */
    fun inverse(): T {

        val determinant = determinant()
        val adjugate = create()

        for (row in 0 until dimensions) {
            for (column in 0 until dimensions) {
                val sign =  if ((row + column) % 2 == 0) 1.0f else -1.0f
                val minor = computeMinor(dimensions, elements, row, column)
                adjugate[column, row] = (sign * minor) / determinant
            }
        }

        return adjugate
    }

    /**
     * @param matrix the matrix to be transformed by.
     * @return the current matrix transformed by the provided matrix.
     */
    fun transform(matrix: T) = dot(matrix)

    /**
     * @return a copy of the matrix.
     */
    fun copy() = create(elements.copyOf(size))

    fun toArray(): FloatArray {
        var floats = FloatArray(0)

        for (element in elements) {
            floats += element
        }

        return floats
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix<*>) return false
        if (dimensions != other.dimensions) return false
        return elements.contentEquals(other.elements)
    }
    
    override fun hashCode() = elements.contentHashCode()

    override fun toString(): String {
        val outer = StringJoiner(", ")
        for (row in 0 until dimensions) {
            val inner = StringJoiner(", ")
            for (column in 0 until dimensions) {
                inner.add(
                    this[row, column].toString()
//                        FloatUtils.roundToDecimal(this[row, column], 1).toString()
                )
            }
            outer.add("\n[$inner]")
        }
        return "$outer]"
    }

    private fun map(transform: (Float) -> Float) = create(FloatArray(size) {
        index -> transform(elements[index])
    })

    private fun map(scalar: Float, transform: (Float, Float) -> Float) = create(FloatArray(size) {
        index -> transform(elements[index], scalar)
    })

    private fun map(matrix: T, transform: (Float, Float) -> Float) = create(FloatArray(size) {
        index -> transform(elements[index], matrix.elements[index])
    })

}