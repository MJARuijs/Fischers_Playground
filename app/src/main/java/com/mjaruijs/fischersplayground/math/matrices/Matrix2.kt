package com.mjaruijs.fischersplayground.math.matrices

import com.mjaruijs.fischersplayground.math.vectors.Vector2

class Matrix2(elements: FloatArray = generateIdentityElements(2)): Matrix<Matrix2>(2, elements) {

    override fun create(elements: FloatArray) = Matrix2(elements)

    infix fun dot(vector: Vector2): Vector2 {
        val result = Vector2()
        for (r in 0 until 2) {
            for (c in 0 until 2) {
                result[r] += this[r, c] * vector[c]
            }
        }
        return result
    }

}