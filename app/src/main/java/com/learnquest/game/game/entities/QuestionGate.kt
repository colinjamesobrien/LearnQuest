package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.learnquest.game.game.questions.Question

/**
 * A gate entity that blocks the player's path.
 * When the player touches it the game pauses and shows the question overlay.
 * A correct answer destroys this gate; wrong answers flash the player.
 */
class QuestionGate(
    val worldX: Float,
    val worldY: Float,
    val question: Question
) {
    val width  = 48f
    val height = 220f

    var isDestroyed = false

    // Bobbing / glow animation
    private var tick = 0f
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCA28")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F00")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val questionMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun update(deltaSeconds: Float) {
        tick += deltaSeconds
    }

    fun draw(canvas: Canvas, cameraX: Float) {
        if (isDestroyed) return
        val sx = worldX - cameraX
        val glow = (Math.sin(tick * 3.0) * 4f).toFloat()

        // Gate pillar left
        canvas.drawRect(sx - width / 2f + glow, worldY, sx - width / 2f + 12f + glow, worldY + height, gatePaint)
        // Gate pillar right
        canvas.drawRect(sx + width / 2f - 12f - glow, worldY, sx + width / 2f - glow, worldY + height, gatePaint)
        // Gate top bar
        canvas.drawRect(sx - width / 2f + glow, worldY, sx + width / 2f - glow, worldY + 28f, gatePaint)

        // Glow outline
        val glowRect = RectF(sx - width / 2f - 4f, worldY - 4f, sx + width / 2f + 4f, worldY + height + 4f)
        canvas.drawRoundRect(glowRect, 8f, 8f, glowPaint)

        // "?" label in the centre gap
        canvas.drawText("?", sx, worldY + height / 2f + 24f, questionMarkPaint)
    }

    /** Trigger rect — slightly wider than visual to make it easy to hit. */
    val triggerBounds: RectF get() = RectF(
        worldX - width / 2f - 8f, worldY,
        worldX + width / 2f + 8f, worldY + height
    )
}
