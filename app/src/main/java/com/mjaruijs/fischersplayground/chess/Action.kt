package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class Action(val clickedPosition: Vector2, val type: ActionType, val previouslySelectedPosition: Vector2? = null)