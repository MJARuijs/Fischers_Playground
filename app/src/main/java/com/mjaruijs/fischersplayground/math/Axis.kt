package com.mjaruijs.fischersplayground.math

import com.mjaruijs.fischersplayground.math.vectors.Vector3

enum class Axis(val normal: Vector3, val index: Int) {

    X(Vector3(1.0f, 0.0f, 0.0f), 0),
    Y(Vector3(0.0f, 1.0f, 0.0f), 1),
    Z(Vector3(0.0f, 0.0f, 1.0f), 2)

}