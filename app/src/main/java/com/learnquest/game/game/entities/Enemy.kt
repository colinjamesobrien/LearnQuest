package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.learnquest.game.game.questions.Question
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class EnemyState { IDLE, WALKING, QUESTION_READY, HURT, DEFEATED }

/**
 * A villain enemy — drawn as a dark sorcerer/wizard.
 *
 * Visual layers (back → front):
 *   cape → arms → body → belt → head → hat → brows → eyes → grin → staff
 *
 * State machine:
 *   IDLE           → activates when player is within 600 px
 *   WALKING        → moves left; legs + arms animate
 *   QUESTION_READY → bobs "?" bubble
 *   HURT           → white flash + rightward stagger → DEFEATED
 *   DEFEATED       → spin + shrink + 5 orbiting stars → isRemoved
 */
class Enemy(
    var worldX: Float,
    var worldY: Float,   // feet Y in ground band
    val question: Question
) {
    val width  = 72f
    val height = 100f

    var state: EnemyState = EnemyState.IDLE
    var isRemoved = false

    private var animTick    = 0f
    private var hurtTimer   = 0f
    private var defeatTimer = 0f
    private var legFrame    = 0
    private var legAnimTick = 0f

    // ── Paints ────────────────────────────────────────────────────────────────

    // Body / robe
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6A1B9A")   // rich purple robe
    }
    // Head skin
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCC80")   // warm skin tone
    }
    // Cape (very dark, behind body)
    private val capePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E0050")   // near-black purple
    }
    // Pointed hat
    private val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A0030")   // darkest purple
    }
    // Hat brim band
    private val hatBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")   // gold band
    }
    // Hat stars (decoration)
    private val hatStarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF176")   // yellow star
    }
    // Gold sash / belt
    private val sashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
    }
    // Belt buckle accent
    private val bucklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F00")
    }
    // Arms / sleeves (same as robe)
    private val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
        strokeWidth = 11f
        color       = Color.parseColor("#6A1B9A")
    }
    // Fist / hand
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCC80")
    }
    // Staff pole
    private val staffPolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 5f
        color       = Color.parseColor("#795548")
    }
    // Staff orb glow
    private val staffOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")   // cyan magic
    }
    private val staffOrbGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x5000E5FF.toInt()
    }
    // Legs
    private val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 10f
        color       = Color.parseColor("#1A0030")
    }
    // Outline (shared)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 2.5f
        color       = 0x66000000.toInt()
    }
    // Body gloss highlight
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x30FFFFFF.toInt()
    }
    // Angry eyebrows
    private val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 5f
        color       = Color.parseColor("#1A0030")
    }
    // Eye white sclera
    private val scleraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    // Red glowing iris
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
    }
    // Pupil
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    // Eye shine
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xBBFFFFFF.toInt()
    }
    // Grin outline
    private val grinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 3f
        color       = Color.parseColor("#1A0030")
    }
    // Teeth
    private val teethPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
    }
    // Hurt overlay
    private val hurtOverlayPaint = Paint().apply {
        color = 0xBBFFFFFF.toInt()
    }
    // Question bubble
    private val qBubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
    }
    private val qTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.BLACK
        textSize    = 44f
        textAlign   = Paint.Align.CENTER
        isFakeBoldText = true
    }
    // Ground shadow
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000.toInt()
    }
    // Defeat stars
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
    }
    private val starOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#FF6F00")
        style       = Paint.Style.STROKE
        strokeWidth = 2f
    }

    companion object {
        const val WALK_SPEED      = 140f
        const val HURT_DURATION   = 0.3f
        const val DEFEAT_DURATION = 0.5f
        const val ACTIVATE_DIST   = 600f
    }

    // ── Update ────────────────────────────────────────────────────────────────

    fun update(deltaSeconds: Float, playerWorldX: Float, playerWorldY: Float) {
        animTick += deltaSeconds
        when (state) {
            EnemyState.IDLE -> {
                if (abs(worldX - playerWorldX) < ACTIVATE_DIST) state = EnemyState.WALKING
            }
            EnemyState.WALKING -> {
                worldX -= WALK_SPEED * deltaSeconds
                legAnimTick += deltaSeconds
                if (legAnimTick > 0.12f) { legAnimTick = 0f; legFrame = (legFrame + 1) % 4 }
            }
            EnemyState.QUESTION_READY -> { /* bob handled in draw */ }
            EnemyState.HURT -> {
                hurtTimer -= deltaSeconds
                worldX += 60f * deltaSeconds
                if (hurtTimer <= 0f) { state = EnemyState.DEFEATED; defeatTimer = DEFEAT_DURATION }
            }
            EnemyState.DEFEATED -> {
                defeatTimer -= deltaSeconds
                if (defeatTimer <= 0f) isRemoved = true
            }
        }
    }

    fun triggerQuestion() {
        if (state == EnemyState.IDLE || state == EnemyState.WALKING) state = EnemyState.QUESTION_READY
    }

    fun triggerHurt() {
        if (state != EnemyState.HURT && state != EnemyState.DEFEATED) {
            state = EnemyState.HURT; hurtTimer = HURT_DURATION
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw(canvas: Canvas, cameraX: Float) {
        if (isRemoved) return
        val sx = worldX - cameraX
        val sy = worldY - height
        when (state) {
            EnemyState.DEFEATED -> drawDefeated(canvas, sx, sy)
            else -> {
                drawShadow(canvas, sx)
                drawCharacter(canvas, sx, sy)
                if (state == EnemyState.HURT) {
                    canvas.drawRect(sx - 4f, sy - 4f, sx + width + 4f, worldY + 4f, hurtOverlayPaint)
                }
                if (state == EnemyState.QUESTION_READY) drawQuestionBubble(canvas, sx, sy)
            }
        }
    }

    private fun drawShadow(canvas: Canvas, sx: Float) {
        canvas.drawOval(
            RectF(sx + width * 0.1f, worldY - 10f, sx + width * 0.9f, worldY + 8f),
            shadowPaint
        )
    }

    private fun drawCharacter(canvas: Canvas, sx: Float, sy: Float) {
        val cx     = sx + width / 2f
        val swing  = legSwing()

        // ── Cape behind body ──────────────────────────────────────────────────
        val capeL = sx - 6f
        val capeR = sx + width + 6f
        val capeTopY = sy + height * 0.26f
        val capeBot  = sy + height * 0.90f
        val capePath = Path().apply {
            moveTo(cx - width * 0.28f, capeTopY)
            lineTo(capeL, capeBot)
            lineTo(capeR, capeBot)
            lineTo(cx + width * 0.28f, capeTopY)
            close()
        }
        canvas.drawPath(capePath, capePaint)
        outlinePaint.strokeWidth = 2f
        canvas.drawPath(capePath, outlinePaint)

        // ── Arms (behind body) ────────────────────────────────────────────────
        val armTopY  = sy + height * 0.33f
        val armBotY  = sy + height * 0.64f
        val lSwing   = swing * 0.7f    // left arm swings forward
        val rSwing   = -swing * 0.7f   // right arm opposite phase

        // Left arm (holds staff)
        armPaint.strokeWidth = 11f
        canvas.drawLine(sx + width * 0.22f, armTopY, sx - 2f, armBotY + lSwing, armPaint)
        // Left hand
        canvas.drawCircle(sx - 2f, armBotY + lSwing, 7f, handPaint)

        // Right arm
        canvas.drawLine(sx + width * 0.78f, armTopY, sx + width + 2f, armBotY + rSwing, armPaint)
        // Right fist
        canvas.drawCircle(sx + width + 2f, armBotY + rSwing, 7f, handPaint)

        // Staff in left hand
        val staffTopX = sx - 14f
        val staffTopY = sy - 20f
        val staffBotX = sx - 2f
        val staffBotY = armBotY + lSwing
        canvas.drawLine(staffBotX, staffBotY, staffTopX, staffTopY, staffPolePaint)
        // Orb glow halo
        canvas.drawCircle(staffTopX, staffTopY, 14f, staffOrbGlowPaint)
        // Orb core (pulsing via animTick)
        val pulse = 9f + sin(animTick * 4.0).toFloat() * 2f
        canvas.drawCircle(staffTopX, staffTopY, pulse, staffOrbPaint)
        // Orb shine
        canvas.drawCircle(staffTopX - 4f, staffTopY - 4f, 4f, shinePaint)

        // ── Body / robe ───────────────────────────────────────────────────────
        val bodyRect = RectF(sx + 8f, sy + height * 0.28f, sx + width - 8f, sy + height * 0.78f)
        canvas.drawRoundRect(bodyRect, 10f, 10f, bodyPaint)
        outlinePaint.strokeWidth = 2f
        canvas.drawRoundRect(bodyRect, 10f, 10f, outlinePaint)
        // Body gloss
        canvas.drawOval(
            RectF(sx + 10f, sy + height * 0.29f, sx + width * 0.55f, sy + height * 0.48f),
            glossPaint
        )

        // Gold sash
        val sashTop = sy + height * 0.56f
        val sashBot = sy + height * 0.65f
        canvas.drawRect(sx + 8f, sashTop, sx + width - 8f, sashBot, sashPaint)
        // Buckle
        canvas.drawCircle(cx, (sashTop + sashBot) / 2f, 7f, bucklePaint)
        canvas.drawCircle(cx, (sashTop + sashBot) / 2f, 4f, sashPaint)

        // ── Legs ──────────────────────────────────────────────────────────────
        val legTopY = sy + height * 0.78f
        canvas.drawLine(sx + width * 0.30f, legTopY, sx + width * 0.30f + swing, worldY, legPaint)
        canvas.drawLine(sx + width * 0.70f, legTopY, sx + width * 0.70f - swing, worldY, legPaint)
        // Feet
        handPaint.color = Color.parseColor("#1A0030")
        canvas.drawCircle(sx + width * 0.30f + swing, worldY - 4f, 7f, handPaint)
        canvas.drawCircle(sx + width * 0.70f - swing, worldY - 4f, 7f, handPaint)
        handPaint.color = Color.parseColor("#FFCC80")

        // ── Head ──────────────────────────────────────────────────────────────
        val headR  = width * 0.31f
        val headCX = cx
        val headCY = sy + height * 0.19f
        canvas.drawCircle(headCX, headCY, headR, skinPaint)
        outlinePaint.strokeWidth = 2f
        canvas.drawCircle(headCX, headCY, headR, outlinePaint)
        // Head gloss
        canvas.drawCircle(headCX - headR * 0.2f, headCY - headR * 0.25f, headR * 0.32f, glossPaint)

        // ── Pointed hat ───────────────────────────────────────────────────────
        val hatTipX = headCX + headR * 0.08f
        val hatTipY = sy - headR * 0.5f
        val hatPath = Path().apply {
            moveTo(hatTipX, hatTipY)
            lineTo(headCX - headR * 1.25f, headCY - headR * 0.25f)
            lineTo(headCX + headR * 1.25f, headCY - headR * 0.25f)
            close()
        }
        canvas.drawPath(hatPath, hatPaint)
        canvas.drawPath(hatPath, outlinePaint)
        // Hat brim band
        val brimY1 = headCY - headR * 0.30f
        val brimY2 = headCY - headR * 0.08f
        canvas.drawRect(headCX - headR * 1.25f, brimY1, headCX + headR * 1.25f, brimY2, hatBandPaint)
        // Stars on hat
        drawMiniStar(canvas, hatTipX - headR * 0.3f, hatTipY + headR * 0.45f, 5f)
        drawMiniStar(canvas, hatTipX + headR * 0.35f, hatTipY + headR * 0.75f, 4f)

        // ── Angry V-shaped eyebrows ───────────────────────────────────────────
        val eyeOffX = headR * 0.30f
        val eyeY    = headCY + headR * 0.02f
        // Left brow (slanting inward-up)
        canvas.drawLine(
            headCX - eyeOffX - headR * 0.28f, eyeY - headR * 0.32f,
            headCX - eyeOffX + headR * 0.18f, eyeY - headR * 0.50f,
            browPaint
        )
        // Right brow (mirror)
        canvas.drawLine(
            headCX + eyeOffX - headR * 0.18f, eyeY - headR * 0.50f,
            headCX + eyeOffX + headR * 0.28f, eyeY - headR * 0.32f,
            browPaint
        )

        // ── Eyes: sclera → iris → pupil → shine ──────────────────────────────
        val eyeR = headR * 0.20f
        // Left eye
        canvas.drawCircle(headCX - eyeOffX, eyeY, eyeR, scleraPaint)
        canvas.drawCircle(headCX - eyeOffX + eyeR * 0.06f, eyeY + eyeR * 0.04f, eyeR * 0.72f, irisPaint)
        canvas.drawCircle(headCX - eyeOffX + eyeR * 0.06f, eyeY + eyeR * 0.04f, eyeR * 0.36f, pupilPaint)
        canvas.drawCircle(headCX - eyeOffX - eyeR * 0.2f, eyeY - eyeR * 0.28f, eyeR * 0.18f, shinePaint)
        // Right eye
        canvas.drawCircle(headCX + eyeOffX, eyeY, eyeR, scleraPaint)
        canvas.drawCircle(headCX + eyeOffX + eyeR * 0.06f, eyeY + eyeR * 0.04f, eyeR * 0.72f, irisPaint)
        canvas.drawCircle(headCX + eyeOffX + eyeR * 0.06f, eyeY + eyeR * 0.04f, eyeR * 0.36f, pupilPaint)
        canvas.drawCircle(headCX + eyeOffX - eyeR * 0.2f, eyeY - eyeR * 0.28f, eyeR * 0.18f, shinePaint)

        // ── Evil grin ─────────────────────────────────────────────────────────
        val grinRect = RectF(
            headCX - headR * 0.42f, headCY + headR * 0.20f,
            headCX + headR * 0.42f, headCY + headR * 0.60f
        )
        // Mouth fill (dark opening)
        canvas.drawArc(grinRect, 0f, 180f, true, pupilPaint)
        // Two sharp teeth
        val tW = headR * 0.14f
        canvas.drawRect(headCX - headR * 0.34f, headCY + headR * 0.20f,
            headCX - headR * 0.34f + tW, headCY + headR * 0.44f, teethPaint)
        canvas.drawRect(headCX + headR * 0.34f - tW, headCY + headR * 0.20f,
            headCX + headR * 0.34f, headCY + headR * 0.44f, teethPaint)
        // Grin outline arc
        canvas.drawArc(grinRect, 0f, 180f, false, grinPaint)
    }

    private fun drawQuestionBubble(canvas: Canvas, sx: Float, sy: Float) {
        val bob = (sin(animTick * 4.0) * 6f).toFloat()
        val cx  = sx + width / 2f
        val cy  = sy - 30f + bob
        val r   = 34f
        canvas.drawCircle(cx, cy, r, qBubblePaint)
        val tailPath = Path().apply {
            moveTo(cx - 8f, cy + r - 4f)
            lineTo(cx + 8f, cy + r - 4f)
            lineTo(cx, cy + r + 14f)
            close()
        }
        canvas.drawPath(tailPath, qBubblePaint)
        canvas.drawText("?", cx, cy + qTextPaint.textSize * 0.35f, qTextPaint)
    }

    private fun drawDefeated(canvas: Canvas, sx: Float, sy: Float) {
        val progress = 1f - (defeatTimer / DEFEAT_DURATION).coerceIn(0f, 1f)
        val scale    = (1f - progress * 0.9f).coerceAtLeast(0.05f)
        val alpha    = ((1f - progress) * 255f).toInt().coerceIn(0, 255)
        val cx       = sx + width / 2f
        val cy       = sy + height / 2f

        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(progress * 540f, cx, cy)
        val prevAlpha = bodyPaint.alpha
        listOf(bodyPaint, skinPaint, capePaint, hatPaint, legPaint, armPaint).forEach { it.alpha = alpha }
        drawCharacter(canvas, sx, sy)
        listOf(bodyPaint, skinPaint, capePaint, hatPaint, legPaint, armPaint).forEach { it.alpha = prevAlpha }
        canvas.restore()

        // 5 orbiting stars
        starPaint.alpha = alpha
        starOutlinePaint.alpha = alpha
        val orbitR = 70f * progress
        for (i in 0 until 5) {
            val angle = (i * 72.0 * Math.PI / 180.0) + animTick * 3.0
            val starX = cx + cos(angle).toFloat() * orbitR
            val starY = cy + sin(angle).toFloat() * orbitR
            drawStar(canvas, starX, starY, 14f * scale.coerceAtLeast(0.3f))
        }
        starPaint.alpha = 255; starOutlinePaint.alpha = 255
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val innerR = r * 0.4f
        val path = Path()
        for (i in 0 until 10) {
            val angle  = (i * 36.0 - 90.0) * Math.PI / 180.0
            val radius = if (i % 2 == 0) r else innerR
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, starPaint)
        canvas.drawPath(path, starOutlinePaint)
    }

    /** Small 5-pointed star for hat decoration. */
    private fun drawMiniStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val innerR = r * 0.4f
        val path = Path()
        for (i in 0 until 10) {
            val angle  = (i * 36.0 - 90.0) * Math.PI / 180.0
            val radius = if (i % 2 == 0) r else innerR
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, hatStarPaint)
    }

    private fun legSwing() = when (legFrame) { 0 -> 0f; 1 -> 14f; 2 -> 0f; else -> -14f }

    val triggerBounds: RectF get() = RectF(
        worldX - 10f, worldY - height - 10f,
        worldX + width + 10f, worldY + 10f
    )
}
