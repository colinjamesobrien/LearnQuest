package com.learnquest.game.game

import android.view.KeyEvent

/**
 * Maps raw Android KeyEvent codes to discrete game actions.
 *
 * Movement (DPAD_LEFT/RIGHT/UP/DOWN) is handled directly in [GameEngine]
 * via onKeyDown/onKeyUp held-key tracking — NOT here.
 * This handler is only used for one-shot actions: answer cycling, confirm, menu select.
 */
object InputHandler {

    enum class Action {
        ANSWER_LEFT,
        ANSWER_RIGHT,
        ANSWER_CONFIRM,
        MENU_SELECT,
        NONE
    }

    /**
     * Returns the discrete action triggered by [keyCode] in the given [state].
     * Returns [Action.NONE] for movement keys and any unrecognised codes.
     */
    fun discreteActionFor(keyCode: Int, state: GameEngine.GameState): Action {
        return when (state) {
            GameEngine.GameState.QUESTION_PAUSE -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT  -> Action.ANSWER_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> Action.ANSWER_RIGHT

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BUTTON_A   -> Action.ANSWER_CONFIRM

                else -> Action.NONE
            }

            GameEngine.GameState.LEVEL_COMPLETE,
            GameEngine.GameState.GAME_OVER -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BUTTON_A   -> Action.MENU_SELECT

                else -> Action.NONE
            }

            // During PLAYING, all navigation is handled as held-key movement in GameEngine
            else -> Action.NONE
        }
    }
}
