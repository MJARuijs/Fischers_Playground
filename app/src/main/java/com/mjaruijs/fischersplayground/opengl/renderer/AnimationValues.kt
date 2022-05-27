package com.mjaruijs.fischersplayground.opengl.renderer

import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class AnimationValues(val animatingRow: Int, val animatingCol: Int, var translation: Vector2, val totalDistance: Vector2, val onFinish: () -> Unit = {}, var stopAnimating: Boolean = false)