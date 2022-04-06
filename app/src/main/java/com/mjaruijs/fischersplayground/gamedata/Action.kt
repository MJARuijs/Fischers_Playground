package com.mjaruijs.fischersplayground.gamedata

import com.mjaruijs.fischersplayground.math.vectors.Vector2

data class Action(val clickedPosition: Vector2, val type: ActionType, val previouslySelectedPosition: Vector2? = null)