package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

/**
 * The player character — belt-scroller edition.
 *
 * Movement is fully D-pad controlled in all 4 directions.
 * worldY = feet Y (depth in ground band); top of character = worldY - height.
 *
 * Animation:
 *   [legPhase]  — continuously advancing float (radians).  Drives all limb
 *                 swings via sin(legPhase) so motion is perfectly smooth (A).
 *   [idlePhase] — slow breathing oscillator active when standing still (C).
 *
 * The body bobs vertically at twice the leg frequency — sin(legPhase * 2)
 * gives a natural heel-strike rise-and-fall (B).
 */
class Player(
    startX: Float,
    startY: Float,
    val characterType: CharacterType = CharacterType.CAT
) {

    var worldX: Float = startX
    var worldY: Float = startY   // feet Y / depth

    val width  = 64f * characterType.widthScale
    val height = 80f * characterType.heightScale

    var facingRight = true

    var lives = 3
    var score = 0

    // ── Animation state ───────────────────────────────────────────────────────

    /** Walking gait phase (radians). Advances at [WALK_RATE] rad/s while moving. */
    private var legPhase  = 0f

    /** Idle breathing phase (radians). Advances at [IDLE_RATE] rad/s while still. */
    private var idlePhase = 0f

    var isMoving = false
        private set

    private var wrongFlashTicks = 0

    companion object {
        const val MOVE_SPEED_X = 360f   // px/s horizontal
        const val MOVE_SPEED_Y = 220f   // px/s depth
        private const val WALK_RATE = 7.5f   // rad/s — full stride cycle feels natural
        private const val IDLE_RATE = 1.4f   // rad/s — slow, calm breathing
    }

    // ── Paints ────────────────────────────────────────────────────────────────

    private val flashPaint  = Paint().apply { color = 0x88FF0000.toInt() }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44000000.toInt() }

    // ── Update ────────────────────────────────────────────────────────────────

    fun update(
        deltaSeconds: Float,
        movingLeft:  Boolean, movingRight: Boolean,
        movingUp:    Boolean, movingDown:  Boolean,
        groundTop:   Float,   groundBottom: Float,
        levelLeft:   Float,   levelRight:   Float
    ) {
        isMoving = movingLeft || movingRight || movingUp || movingDown

        if (movingRight) { worldX += MOVE_SPEED_X * deltaSeconds; facingRight = true  }
        if (movingLeft)  { worldX -= MOVE_SPEED_X * deltaSeconds; facingRight = false }
        if (movingDown)  worldY += MOVE_SPEED_Y * deltaSeconds
        if (movingUp)    worldY -= MOVE_SPEED_Y * deltaSeconds

        worldX = worldX.coerceIn(levelLeft,  levelRight)
        worldY = worldY.coerceIn(groundTop, groundBottom)

        // Option A — advance continuous gait phase while moving
        if (isMoving) {
            legPhase  += WALK_RATE * deltaSeconds
        } else {
            // Option C — advance slow idle breathing phase while still
            idlePhase += IDLE_RATE * deltaSeconds
        }

        if (wrongFlashTicks > 0) wrongFlashTicks--
    }

    fun triggerWrongFlash() { wrongFlashTicks = 30 }

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw(canvas: Canvas, cameraX: Float) {
        val sx = worldX - cameraX

        // Option B — body bobs at 2× leg frequency while walking; breathes while idle
        val bobOffset = if (isMoving)
            sin(legPhase * 2.0).toFloat() * 2.8f      // subtle heel-strike bounce
        else
            sin(idlePhase.toDouble()).toFloat() * 1.4f // gentle resting breath

        val sy = worldY - height + bobOffset   // top of character

        // Ground shadow (squashes slightly with bob to reinforce lift)
        val shadowScale = 1f - bobOffset * 0.015f
        canvas.drawOval(
            RectF(
                sx + width * (0.10f + 0.02f * (1f - shadowScale)),
                worldY - 10f,
                sx + width * (0.90f - 0.02f * (1f - shadowScale)),
                worldY + 8f
            ),
            shadowPaint
        )

        // Red flash overlay on wrong answer
        if (wrongFlashTicks > 0) canvas.drawRect(sx, sy, sx + width, worldY + bobOffset, flashPaint)

        CharacterRenderer.draw(
            canvas      = canvas,
            sx          = sx,
            sy          = sy,
            width       = width,
            height      = height,
            type        = characterType,
            facingRight = facingRight,
            legPhase    = legPhase,
            isMoving    = isMoving
        )
    }

    /** World-space bounds (feet at worldY). */
    val worldBounds: RectF get() = RectF(worldX, worldY - height, worldX + width, worldY)
}
