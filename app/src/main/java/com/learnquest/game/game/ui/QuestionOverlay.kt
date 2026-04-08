package com.learnquest.game.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.learnquest.game.game.questions.Question

/**
 * Draws the full-screen question overlay during QUESTION_PAUSE state.
 *
 * Layout:
 *   ┌────────────────────────────────────┐
 *   │  [prompt text]                     │
 *   │                                    │
 *   │  [A]  [B]  [C]  [D]               │
 *   │                                    │
 *   │  ← OK to answer  ▶ to move →      │
 *   └────────────────────────────────────┘
 *
 * The selected answer is highlighted in yellow.
 * After an answer, shows brief feedback (correct / wrong) before resuming.
 */
class QuestionOverlay {

    private val dimPaint = Paint().apply { color = 0xDD001F3D.toInt() }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
    }
    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 52f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFFFFFF.toInt()
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private val answerNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0") }
    private val answerSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
    private val answerCorrectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }
    private val answerWrongPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }

    private val answerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val answerSelectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 38f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val feedbackCorrectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#43A047")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val feedbackWrongPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    /**
     * Draw the overlay.
     *
     * @param question       The current question.
     * @param selectedIndex  Which answer box is currently highlighted.
     * @param feedbackState  null = awaiting input, true = correct, false = wrong.
     */
    fun draw(
        canvas: Canvas,
        question: Question,
        selectedIndex: Int,
        feedbackState: Boolean?
    ) {
        val W = canvas.width.toFloat()
        val H = canvas.height.toFloat()

        // Dim the game behind
        canvas.drawRect(0f, 0f, W, H, dimPaint)

        // Card
        val cardW = W * 0.76f
        val cardH = H * 0.70f
        val cardL = (W - cardW) / 2f
        val cardT = (H - cardH) / 2f
        val cardRect = RectF(cardL, cardT, cardL + cardW, cardT + cardH)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Prompt (may contain newline for multi-line questions)
        val promptLines = question.prompt.split("\n")
        val promptCX = W / 2f
        var promptY = cardT + 72f
        for (line in promptLines) {
            canvas.drawText(line, promptCX, promptY, promptPaint)
            promptY += promptPaint.textSize + 8f
        }

        // Answer boxes — 4 in a row
        val boxW   = cardW * 0.20f
        val boxH   = 90f
        val boxGap = (cardW - 4 * boxW) / 5f
        val boxY   = cardT + cardH * 0.52f

        question.choices.forEachIndexed { i, choice ->
            val bx = cardL + boxGap + i * (boxW + boxGap)
            val boxRect = RectF(bx, boxY, bx + boxW, boxY + boxH)

            val bgPaint = when {
                feedbackState != null && i == selectedIndex ->
                    if (feedbackState) answerCorrectPaint else answerWrongPaint
                i == selectedIndex -> answerSelectedPaint
                else               -> answerNormalPaint
            }
            canvas.drawRoundRect(boxRect, 20f, 20f, bgPaint)

            val txtPaint = if (i == selectedIndex && feedbackState == null) answerSelectedTextPaint
                           else answerTextPaint
            canvas.drawText(choice, bx + boxW / 2f, boxY + boxH * 0.64f, txtPaint)
        }

        // Feedback message
        if (feedbackState != null) {
            val msg = if (feedbackState) "⭐ Great job! ⭐" else "Try again!"
            val paint = if (feedbackState) feedbackCorrectPaint else feedbackWrongPaint
            canvas.drawText(msg, W / 2f, cardT + cardH * 0.88f, paint)
        } else {
            // Navigation hint
            canvas.drawText("◀ ▶  to choose    OK to answer", W / 2f, cardT + cardH * 0.90f, hintPaint)
        }
    }
}
