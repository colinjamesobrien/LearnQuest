package com.learnquest.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.learnquest.game.game.GameSurfaceView
import com.learnquest.game.game.entities.CharacterType
import com.learnquest.game.game.questions.QuestionBank

/**
 * Full-screen game activity.
 *
 * On level complete: saves progress, then navigates back to [WorldMapActivity]
 * (which will have the next level unlocked).
 * On game over: navigates back to [WorldMapActivity] at the same level.
 *
 * Both onKeyDown and onKeyUp are forwarded so the game can track held keys
 * for smooth 4-directional belt-scroller movement.
 */
class GameActivity : Activity() {

    companion object {
        const val EXTRA_MODE      = "mode"
        const val EXTRA_LEVEL     = "level"
        const val EXTRA_CHARACTER = "character"
    }

    private lateinit var gameView: GameSurfaceView

    // Kept as fields so the callbacks can reference them
    private var modeName  = QuestionBank.GameMode.MIXED.name
    private var level     = 1
    private var charName  = CharacterType.CAT.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        modeName = intent.getStringExtra(EXTRA_MODE)      ?: QuestionBank.GameMode.MIXED.name
        level    = intent.getIntExtra(EXTRA_LEVEL, 1)
        charName = intent.getStringExtra(EXTRA_CHARACTER) ?: CharacterType.CAT.name

        val mode          = QuestionBank.GameMode.valueOf(modeName)
        val characterType = try { CharacterType.valueOf(charName) } catch (_: Exception) { CharacterType.CAT }

        gameView = GameSurfaceView(
            context         = this,
            questionMode    = mode,
            level           = level,
            characterType   = characterType,
            onGameOver      = { returnToWorldMap(levelCompleted = false) },
            onLevelComplete = { returnToWorldMap(levelCompleted = true)  }
        )

        val container = FrameLayout(this)
        container.addView(gameView)
        setContentView(container)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (gameView.onGameKeyDown(keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (gameView.onGameKeyUp(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause()  { super.onPause();  gameView.pause()  }
    override fun onResume() { super.onResume(); gameView.resume() }

    /**
     * Returns to the world map.
     * If [levelCompleted] the next level is unlocked first.
     * Called from the game thread → must post to UI thread.
     */
    private fun returnToWorldMap(levelCompleted: Boolean) {
        if (levelCompleted) {
            LevelProgress.completeLevel(this, level)
        }
        runOnUiThread {
            val intent = Intent(this, WorldMapActivity::class.java).apply {
                putExtra(WorldMapActivity.EXTRA_MODE,      modeName)
                putExtra(WorldMapActivity.EXTRA_CHARACTER, charName)
                // Clear the back-stack up to (and including) any existing WorldMapActivity
                // so pressing Back from the map doesn't re-enter a finished game
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
