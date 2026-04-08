package com.learnquest.game.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Draws the heads-up display: score, lives (hearts), and level indicator.
 * Always rendered last so it appears above all game elements.
 */
class HUD {

    private val bgPaint = Paint().apply {
        color = 0xCC000000.toInt()
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        textSize = 36f
        isFakeBoldText = true
    }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
    }
    private val heartFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF5350")
    }
    private val heartEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
    }

    fun draw(canvas: Canvas, score: Int, lives: Int, maxLives: Int, level: Int) {
        val padH = 12f
        val barH = 56f

        // Semi-transparent bar across the top
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), barH + padH * 2, bgPaint)

        // Score
        canvas.drawText("⭐ $score", 24f, barH, scorePaint)

        // Level label (centred)
        val lvlText = "Level $level"
        val lvlW = levelPaint.measureText(lvlText)
        canvas.drawText(lvlText, canvas.width / 2f - lvlW / 2f, barH, levelPaint)

        // Hearts (lives)
        val heartSize = 36f
        val heartPad  = 8f
        val startX    = canvas.width - (heartSize + heartPad) * maxLives - 16f
        for (i in 0 until maxLives) {
            val hx = startX + i * (heartSize + heartPad)
            val paint = if (i < lives) heartFillPaint else heartEmptyPaint
            drawHeart(canvas, hx, padH + 4f, heartSize, paint)
        }
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val path = android.graphics.Path().apply {
            val cx = x + size / 2f
            val cy = y + size * 0.35f
            val r  = size * 0.28f
            // Left lobe
            addCircle(cx - r, cy, r, android.graphics.Path.Direction.CW)
            // Right lobe
            addCircle(cx + r, cy, r, android.graphics.Path.Direction.CW)
            // Bottom triangle point
            moveTo(x, cy + r * 0.5f)
            lineTo(cx, y + size)
            lineTo(x + size, cy + r * 0.5f)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
