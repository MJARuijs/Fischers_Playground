//package game.states
//
////import graphics.Renderer
////import graphics.shaders.ShaderProgram
////import userinterface.UIPage
//import com.mjaruijs.fischersplayground.userinterface.items.UIButton
//import userinterface.layout.constraints.ConstraintDirection
//import com.mjaruijs.fischersplayground.userinterface.layout.constraints.ConstraintSet
//import userinterface.layout.constraints.constrainttypes.AspectRatioConstraint
//import userinterface.layout.constraints.constrainttypes.PixelConstraint
//import userinterface.layout.constraints.constrainttypes.RelativeConstraint
//
//class PlayGameState(windowWidth: Int, windowHeight: Int, enterState: (String) -> Unit) : State("play_game_state", windowWidth, windowHeight, enterState) {
//
////    override var userInterface: UIPage = UIPage("game_state")
//
////    private val renderer = Renderer(windowWidth, windowHeight)
//
//    init {
//        val playButtonConstraints = ConstraintSet(
//            PixelConstraint(ConstraintDirection.TO_LEFT),
//            PixelConstraint(ConstraintDirection.TO_TOP),
//            RelativeConstraint(ConstraintDirection.VERTICAL, 0.2f),
//            AspectRatioConstraint(ConstraintDirection.HORIZONTAL, 2f)
//        )
//
//        val playButton = UIButton("play", playButtonConstraints)
//            .setOnClick {
//                enterState("main_menu_state")
//            }
//
////        userInterface += playButton
//    }
//
////    override fun render(uiProgram: ShaderProgram, textProgram: ShaderProgram) {
////        renderer.render()
////        super.render(uiProgram, textProgram)
////    }
//
//}