package game

import android.opengl.GLES20.*
//import com.mjaruijs.fischersplayground.game.states.MainMenuState
//import game.states.PlayGameState
import game.states.State

class Game(windowWidth: Int, windowHeight: Int) {

//    private val uiProgram = ShaderProgram(
//        ShaderLoader.load(R.raw.)
//    )
//        .load("shaders/ui.vert", "shaders/ui.frag")
//    private val textProgram = ShaderProgram.load("shaders/text.vert", "shaders/text.frag")

    private val states = ArrayList<State>()

//    private var currentState: State

    init {
//        states += MainMenuState(windowWidth, windowHeight, ::enterState)
//        states += PlayGameState(windowWidth, windowHeight, ::enterState)

//        currentState = states[0]
    }

//    fun update(mouse: Mouse, keyboard: Keyboard, deltaTime: Float) {
//        currentState.update(mouse, keyboard, deltaTime)
//    }

    fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

//        currentState.render(uiProgram, textProgram)
    }

    fun enterState(name: String) {
//        currentState = states.find { state -> state.name == name } ?: return
    }

}