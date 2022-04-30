//package com.mjaruijs.fischersplayground.game.states
//
////import userinterface.UIPage
//import com.mjaruijs.fischersplayground.userinterface.items.UIButton
//import userinterface.layout.constraints.ConstraintDirection
//import com.mjaruijs.fischersplayground.userinterface.layout.constraints.ConstraintSet
//import game.states.State
//import userinterface.layout.constraints.constrainttypes.AspectRatioConstraint
//import userinterface.layout.constraints.constrainttypes.CenterConstraint
//import userinterface.layout.constraints.constrainttypes.RelativeConstraint
//
//class MainMenuState(windowWidth: Int, windowHeight: Int, enterState: (String) -> Unit) : State("main_menu_state", windowWidth, windowHeight, enterState) {
//
////    override var userInterface: UIPage = UIPage("main_menu")
//
//    init {
//        val playButtonConstraints = ConstraintSet(
//            CenterConstraint(ConstraintDirection.HORIZONTAL),
//            CenterConstraint(ConstraintDirection.VERTICAL),
//            RelativeConstraint(ConstraintDirection.VERTICAL, 0.2f),
//            AspectRatioConstraint(ConstraintDirection.HORIZONTAL, 2f)
//        )
//
//        val playButton = UIButton("play", playButtonConstraints)
//            .setOnClick {
//                enterState("play_game_state")
//            }
//
////        userInterface += playButton
//    }
//
//}