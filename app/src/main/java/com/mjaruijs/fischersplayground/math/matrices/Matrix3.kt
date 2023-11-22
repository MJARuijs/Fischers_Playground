package com.mjaruijs.fischersplayground.math.matrices

import com.mjaruijs.fischersplayground.math.vectors.Vector3

class Matrix3(elements: FloatArray = generateIdentityElements(3)): Matrix<Matrix3>(3, elements) {

    constructor(matrix: Matrix2): this(floatArrayOf(
            matrix[0], matrix[1], 0.0f,
            matrix[2], matrix[3], 0.0f,
            0.0f, 0.0f, 1.0f
    ))

    constructor(matrix: Matrix4): this(floatArrayOf(
            matrix[0], matrix[1], matrix[2],
            matrix[4], matrix[5], matrix[6],
            matrix[8], matrix[9], matrix[10]
    ))

    override fun create(elements: FloatArray) = Matrix3(elements)

    infix fun dot(vector: Vector3): Vector3 {
        val result = Vector3()
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                result[r] += this[r, c] * vector[c]
            }
        }
        return result
    }

}
