package com.learnquest.game.game

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.learnquest.game.game.entities.CharacterType
import com.learnquest.game.game.questions.QuestionBank

/**
 * The main game view.
 *
 * Lifecycle:
 *  surfaceCreated   → creates [GameEngine] + starts [GameThread]
 *  surfaceDestroyed → stops [GameThread]
 *
 * Key events are forwarded here from [com.learnquest.game.GameActivity]:
 *  [onGameKeyDown] — key-down events (sets held-movement state, queues discrete actions)
 *  [onGameKeyUp]   — key-up events   (clears held-movement state)
 */
class GameSurfaceView(
    context: Context,
    private val questionMode:    QuestionBank.GameMode,
    private val level:           Int,
    private val characterType:   CharacterType = CharacterType.CAT,
    private val onGameOver:      () -> Unit,
    private val onLevelComplete: () -> Unit
) : SurfaceView(context), SurfaceHolder.Callback {

    private lateinit var engine: GameEngine
    private lateinit var gameThread: GameThread

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // ── SurfaceHolder.Callback ────────────────────────────────────────────────

    override fun surfaceCreated(h: SurfaceHolder) {
        engine = GameEngine(
            screenWidth     = width,
            screenHeight    = height,
            questionMode    = questionMode,
            level           = level,
            characterType   = characterType,
            onGameOver      = onGameOver,
            onLevelComplete = onLevelComplete
        )
        gameThread = GameThread(h, engine).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, h2: Int) {
        // Fixed landscape layout — no action needed
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        stopThread()
    }

    // ── Input forwarding ──────────────────────────────────────────────────────

    /** Called by GameActivity for every key-down. Returns true if consumed. */
    fun onGameKeyDown(keyCode: Int): Boolean {
        if (!::engine.isInitialized) return false
        engine.onKeyDown(keyCode)
        return true
    }

    /** Called by GameActivity for every key-up. Returns true if consumed. */
    fun onGameKeyUp(keyCode: Int): Boolean {
        if (!::engine.isInitialized) return false
        engine.onKeyUp(keyCode)
        return true
    }

    // ── Activity lifecycle ────────────────────────────────────────────────────

    fun pause() {
        if (::gameThread.isInitialized) stopThread()
    }

    fun resume() {
        if (::gameThread.isInitialized && !gameThread.running && ::engine.isInitialized) {
            gameThread = GameThread(holder, engine).also {
                it.running = true
                it.start()
            }
        }
    }

    private fun stopThread() {
        gameThread.running = false
        try { gameThread.join(2000) } catch (_: InterruptedException) { }
    }
}
