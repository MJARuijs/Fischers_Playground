package game.states

//import devices.Keyboard
//import devices.Mouse
//import graphics.shaders.ShaderProgram
//import math.vectors.Vector2
//import userinterface.UIPage

abstract class State(val name: String, protected val windowWidth: Int, protected val windowHeight: Int, val enterState: (String) -> Unit) {

//    protected abstract var userInterface: UIPage

    protected var aspectRatio = windowWidth.toFloat() / windowHeight.toFloat()

//    open fun render(uiProgram: ShaderProgram, textProgram: ShaderProgram) {
//        userInterface.draw(uiProgram, textProgram, aspectRatio, Vector2(windowWidth, windowHeight))
//    }

//    open fun update(mouse: Mouse, keyboard: Keyboard, deltaTime: Float) {
//        userInterface.update(mouse, keyboard, aspectRatio, deltaTime)
//    }

}