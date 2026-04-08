package com.learnquest.game.game

import android.graphics.Canvas
import android.view.KeyEvent
import com.learnquest.game.game.entities.Background
import com.learnquest.game.game.entities.CharacterType
import com.learnquest.game.game.entities.Enemy
import com.learnquest.game.game.entities.EnemyState
import com.learnquest.game.game.entities.Player
import com.learnquest.game.game.questions.QuestionBank
import com.learnquest.game.game.ui.HUD
import com.learnquest.game.game.ui.QuestionOverlay
import kotlin.math.abs

/**
 * Master game state machine — belt-scroller edition.
 *
 * Input model:
 *   onKeyDown(keyCode) — called from UI thread on every key-down event
 *   onKeyUp(keyCode)   — called from UI thread on every key-up event
 *
 * D-pad movement keys are tracked as @Volatile held-booleans for smooth per-frame motion.
 * Discrete actions (answer select/confirm) are pushed into a synchronized queue.
 *
 * update() and draw() are called from [GameThread] on a background thread.
 */
class GameEngine(
    private val screenWidth:     Int,
    private val screenHeight:    Int,
    private val questionMode:    QuestionBank.GameMode,
    private val level:           Int,
    private val characterType:   CharacterType = CharacterType.CAT,
    private val onGameOver:      () -> Unit,
    private val onLevelComplete: () -> Unit
) {
    enum class GameState { PLAYING, QUESTION_PAUSE, LEVEL_COMPLETE, GAME_OVER }

    var state: GameState = GameState.PLAYING
        private set

    // ── Ground band ───────────────────────────────────────────────────────────
    val groundTop    = screenHeight * 0.62f   // back of scene (smaller Y = higher on screen)
    val groundBottom = screenHeight * 0.86f   // front of scene (larger Y = lower on screen)
    private val groundMid = (groundTop + groundBottom) / 2f

    // ── Level ─────────────────────────────────────────────────────────────────
    private val levelLength = LevelGenerator.levelLength(level)

    // ── Camera ────────────────────────────────────────────────────────────────
    var cameraX = 0f
        private set
    private val cameraLeadX    = screenWidth * 0.40f   // player shown at ~40% from left edge
    private val cameraMaxX     = (LevelGenerator.levelLength(level) - screenWidth).coerceAtLeast(0f)

    companion object {
        /** Exponential-smoothing rate (per second). Higher = snappier follow.
         *  At 7 rad/s the camera covers 99.9% of remaining lag within ~1 s,
         *  but each individual frame glides rather than snapping. */
        private const val CAMERA_SMOOTH = 7f
    }

    // ── Entities ──────────────────────────────────────────────────────────────
    private val player          = Player(200f, groundMid, characterType)
    private val background      = Background()
    private val hud             = HUD()
    private val questionOverlay = QuestionOverlay()

    private val enemies = mutableListOf<Enemy>()
    private var activeEnemy: Enemy? = null

    // ── Question/feedback state ───────────────────────────────────────────────
    private var selectedAnswerIndex = 0
    private var feedbackState: Boolean? = null   // null = waiting, true = correct, false = wrong
    private var feedbackTicks = 0
    private var wrongAttempts = 0

    // ── Held movement keys (@Volatile → written on UI thread, read on game thread) ──
    @Volatile private var movingLeft  = false
    @Volatile private var movingRight = false
    @Volatile private var movingUp    = false
    @Volatile private var movingDown  = false

    // ── Discrete action queue (synchronized) ─────────────────────────────────
    private val actionQueue = ArrayDeque<InputHandler.Action>()

    init {
        val questionBank = QuestionBank(questionMode, level.coerceAtMost(3))
        LevelGenerator.generate(questionBank, groundTop, groundBottom, levelLength, level)
            .forEach { enemies.add(it) }
    }

    // ── Public API (called from UI thread) ────────────────────────────────────

    /**
     * Called by [GameSurfaceView] on every key-down event.
     * Updates held-movement state AND queues any discrete action.
     */
    fun onKeyDown(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT  -> movingLeft  = true
            KeyEvent.KEYCODE_DPAD_RIGHT -> movingRight = true
            KeyEvent.KEYCODE_DPAD_UP    -> movingUp    = true
            KeyEvent.KEYCODE_DPAD_DOWN  -> movingDown  = true
        }
        val action = InputHandler.discreteActionFor(keyCode, state)
        if (action != InputHandler.Action.NONE) {
            synchronized(actionQueue) { actionQueue.addLast(action) }
        }
    }

    /** Called by [GameSurfaceView] on every key-up event. */
    fun onKeyUp(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT  -> movingLeft  = false
            KeyEvent.KEYCODE_DPAD_RIGHT -> movingRight = false
            KeyEvent.KEYCODE_DPAD_UP    -> movingUp    = false
            KeyEvent.KEYCODE_DPAD_DOWN  -> movingDown  = false
        }
    }

    // ── Game loop (called from GameThread) ────────────────────────────────────

    fun update(deltaSeconds: Float) {
        val actions: List<InputHandler.Action>
        synchronized(actionQueue) {
            actions = actionQueue.toList()
            actionQueue.clear()
        }

        when (state) {
            GameState.PLAYING        -> updatePlaying(deltaSeconds)
            GameState.QUESTION_PAUSE -> updateQuestion(actions)
            GameState.LEVEL_COMPLETE,
            GameState.GAME_OVER      -> { /* Waiting for Activity to finish() */ }
        }
    }

    fun draw(canvas: Canvas) {
        // 1. Background: sky + parallax hills + tiled floor
        background.draw(canvas, cameraX, screenWidth, screenHeight, groundTop.toInt())

        // 2. Entities depth-sorted (larger worldY → in front → drawn last)
        buildDrawList().forEach { entity ->
            when (entity) {
                is Player -> entity.draw(canvas, cameraX)
                is Enemy  -> entity.draw(canvas, cameraX)
            }
        }

        // 3. HUD always on top
        hud.draw(canvas, player.score, player.lives, 3, level)

        // 4. Question overlay during pause
        if (state == GameState.QUESTION_PAUSE) {
            activeEnemy?.question?.let { q ->
                questionOverlay.draw(canvas, q, selectedAnswerIndex, feedbackState)
            }
        }

        // 5. End-screen dim + text
        if (state == GameState.LEVEL_COMPLETE || state == GameState.GAME_OVER) {
            drawEndScreen(canvas)
        }
    }

    // ── Private: PLAYING update ───────────────────────────────────────────────

    private fun updatePlaying(deltaSeconds: Float) {
        player.update(
            deltaSeconds,
            movingLeft, movingRight, movingUp, movingDown,
            groundTop, groundBottom,
            levelLeft  = 0f,
            levelRight = levelLength
        )

        // Camera smoothly follows player (exponential ease toward target)
        val targetCameraX = (player.worldX - cameraLeadX).coerceIn(0f, cameraMaxX)
        cameraX += (targetCameraX - cameraX) * CAMERA_SMOOTH * deltaSeconds
        cameraX = cameraX.coerceIn(0f, cameraMaxX)

        // Update enemies; purge removed ones
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.isRemoved) { iter.remove(); continue }
            e.update(deltaSeconds, player.worldX, player.worldY)

            // Collision check: walking enemy touches player → trigger question
            if (e.state == EnemyState.WALKING && playerOverlapsEnemy(e)) {
                triggerQuestion(e)
                return  // only one question at a time
            }
        }

        // All enemies defeated → level complete
        if (enemies.isEmpty()) {
            state = GameState.LEVEL_COMPLETE
            onLevelComplete()
        }
    }

    private fun triggerQuestion(enemy: Enemy) {
        enemy.triggerQuestion()
        activeEnemy         = enemy
        selectedAnswerIndex = 0
        feedbackState       = null
        wrongAttempts       = 0
        // Stop the player drifting while the overlay is shown
        movingLeft  = false; movingRight = false
        movingUp    = false; movingDown  = false
        state = GameState.QUESTION_PAUSE
    }

    // ── Private: QUESTION_PAUSE update ───────────────────────────────────────

    private fun updateQuestion(actions: List<InputHandler.Action>) {
        if (feedbackTicks > 0) {
            feedbackTicks--
            if (feedbackTicks == 0) handleFeedbackComplete()
            return
        }

        actions.forEach { action ->
            val q = activeEnemy?.question ?: return@forEach
            when (action) {
                InputHandler.Action.ANSWER_LEFT  ->
                    selectedAnswerIndex = (selectedAnswerIndex + q.choices.size - 1) % q.choices.size

                InputHandler.Action.ANSWER_RIGHT ->
                    selectedAnswerIndex = (selectedAnswerIndex + 1) % q.choices.size

                InputHandler.Action.ANSWER_CONFIRM -> {
                    val correct = selectedAnswerIndex == q.correctIndex
                    feedbackState = correct
                    feedbackTicks = 20   // ~0.33 s of feedback (snappy)
                    if (!correct) {
                        player.triggerWrongFlash()
                        wrongAttempts++
                    }
                }

                else -> Unit
            }
        }
    }

    private fun handleFeedbackComplete() {
        when {
            feedbackState == true -> {
                // Correct answer — defeat the enemy and resume play
                activeEnemy?.triggerHurt()
                player.score += 100
                state = GameState.PLAYING
            }
            wrongAttempts >= 3 -> {
                // Child-friendly: after 3 wrong attempts still defeat enemy and move on
                activeEnemy?.triggerHurt()
                state = GameState.PLAYING
            }
            else -> {
                // Wrong answer, attempts remaining — deduct a life and let them retry
                player.lives--
                if (player.lives <= 0) {
                    state = GameState.GAME_OVER
                    onGameOver()
                } else {
                    feedbackState       = null
                    selectedAnswerIndex = 0
                }
            }
        }
    }

    // ── Private: helpers ──────────────────────────────────────────────────────

    /**
     * True when the player's centre is within 90 px horizontally and 70 px in depth
     * of an enemy's centre.  Both axes must be close for a belt-scroller "hit".
     */
    private fun playerOverlapsEnemy(enemy: Enemy): Boolean {
        val dx = abs((player.worldX + player.width / 2f) - (enemy.worldX + enemy.width / 2f))
        val dy = abs(player.worldY - enemy.worldY)
        return dx < 90f && dy < 70f
    }

    /**
     * Builds a depth-sorted draw list of the player + all living enemies.
     * Entities with a larger worldY (closer to camera) are placed later in the list
     * so they are drawn on top.
     */
    private fun buildDrawList(): List<Any> {
        val list = mutableListOf<Any>(player)
        enemies.filter { !it.isRemoved }.forEach { list.add(it) }
        return list.sortedBy { e ->
            when (e) {
                is Player -> e.worldY
                is Enemy  -> e.worldY
                else      -> 0f
            }
        }
    }

    // ── End-screen rendering ──────────────────────────────────────────────────

    private val overlayPaint = android.graphics.Paint().apply { color = 0xCC000000.toInt() }
    private val endTitlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 80f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val endSubPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFFFFFF.toInt()
        textSize = 36f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    private fun drawEndScreen(canvas: Canvas) {
        val W = canvas.width.toFloat()
        val H = canvas.height.toFloat()
        canvas.drawRect(0f, 0f, W, H, overlayPaint)
        val title = if (state == GameState.LEVEL_COMPLETE) "Level Complete! ⭐" else "Game Over"
        canvas.drawText(title,              W / 2f, H / 2f - 40f, endTitlePaint)
        canvas.drawText("Score: ${player.score}", W / 2f, H / 2f + 40f,  endSubPaint)
        canvas.drawText("Press OK to continue",   W / 2f, H / 2f + 100f, endSubPaint)
    }
}
