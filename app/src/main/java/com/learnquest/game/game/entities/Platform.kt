package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * A ground/platform tile that the player runs on.
 * All coordinates are in world space; rendering converts to screen space.
 *
 * @param worldX   Left edge in world pixels
 * @param worldY   Top edge in world pixels
 * @param width    Platform width in pixels
 * @param height   Platform height in pixels
 */
class Platform(
    val worldX: Float,
    val worldY: Float,
    val width: Float,
    val height: Float
) {
    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66BB6A")    // green top
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5D4037")    // brown body
    }
    private val topStripeHeight = height * 0.18f

    /** Returns the bounding rect in world space (for collision). */
    val worldBounds get() = RectF(worldX, worldY, worldX + width, worldY + height)

    fun draw(canvas: Canvas, cameraX: Float) {
        val sx = worldX - cameraX
        // body
        canvas.drawRect(sx, worldY + topStripeHeight, sx + width, worldY + height, bodyPaint)
        // grass top stripe
        canvas.drawRect(sx, worldY, sx + width, worldY + topStripeHeight, topPaint)
    }
}
