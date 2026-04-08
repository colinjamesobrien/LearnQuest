package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stateless Canvas renderer for all 8 playable [CharacterType]s.
 *
 * Drawing order per character (back → front):
 *   tails / wings / frills  → arms → body + head → legs + feet
 *   → ears / horns / crests → eyes → nose / snout → mouth / whiskers
 *
 * No external assets — pure Canvas drawing only.
 */
object CharacterRenderer {

    // ── Shared paints ─────────────────────────────────────────────────────────

    private val bodyPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val legPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 10f
    }
    private val armPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 8f
    }
    private val tailPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }
    private val eyePaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val whiteP         = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val irisPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x50FFFFFF.toInt() }
    private val shadowPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000.toInt() }
    private val facePaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeWidth = 2.5f
    }

    // ── Public entry point ────────────────────────────────────────────────────

    fun draw(
        canvas:      Canvas,
        sx:          Float,
        sy:          Float,
        width:       Float,
        height:      Float,
        type:        CharacterType,
        facingRight: Boolean,
        legPhase:    Float,
        isMoving:    Boolean
    ) {
        val cx = sx + width / 2f
        if (!facingRight) {
            canvas.save()
            canvas.scale(-1f, 1f, cx, sy + height / 2f)
        }
        when (type) {
            CharacterType.CAT         -> drawCat        (canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.DOG         -> drawDog        (canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.RABBIT      -> drawRabbit     (canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.FOX         -> drawFox        (canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.T_REX       -> drawTRex       (canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.TRICERATOPS -> drawTriceratops(canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.STEGOSAURUS -> drawStegosaurus(canvas, sx, sy, width, height, cx, legPhase, isMoving)
            CharacterType.PTERODACTYL -> drawPterodactyl(canvas, sx, sy, width, height, cx, legPhase, isMoving)
        }
        if (!facingRight) canvas.restore()
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun drawBodyAndHead(
        canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float, cx: Float,
        bodyColor: Int, outlineColor: Int
    ): Pair<Float, Float> {
        bodyPaint.color    = bodyColor
        outlinePaint.color = outlineColor
        outlinePaint.strokeWidth = 3f

        val bodyTop  = sy + height * 0.38f
        val bodyBot  = sy + height * 0.80f
        val bodyRect = RectF(sx + width * 0.14f, bodyTop, sx + width * 0.86f, bodyBot)
        canvas.drawRoundRect(bodyRect, 14f, 14f, bodyPaint)
        canvas.drawRoundRect(bodyRect, 14f, 14f, outlinePaint)
        canvas.drawRoundRect(
            RectF(sx + width * 0.20f, bodyTop + 4f, sx + width * 0.68f, bodyTop + (bodyBot - bodyTop) * 0.44f),
            7f, 7f, highlightPaint
        )

        val headR  = width * 0.34f
        val headCX = cx
        val headCY = sy + height * 0.26f
        canvas.drawCircle(headCX, headCY, headR, bodyPaint)
        canvas.drawCircle(headCX, headCY, headR, outlinePaint)
        canvas.drawCircle(headCX - headR * 0.20f, headCY - headR * 0.26f, headR * 0.34f, highlightPaint)

        return headCX to headCY
    }

    private fun drawArms(
        canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
        armColor: Int, legPhase: Float, isMoving: Boolean
    ) {
        armPaint.color = armColor
        val shoulderY = sy + height * 0.44f
        val swing     = if (isMoving) legSwing(legPhase) * 0.75f else 0f
        canvas.drawLine(sx + width * 0.14f, shoulderY, sx + width * 0.14f + swing,  shoulderY + height * 0.26f, armPaint)
        canvas.drawLine(sx + width * 0.86f, shoulderY, sx + width * 0.86f - swing,  shoulderY + height * 0.26f, armPaint)
    }

    private fun drawLegs(
        canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
        legColor: Int, legPhase: Float, isMoving: Boolean
    ) {
        legPaint.color = legColor
        val legTopY = sy + height * 0.80f
        val feetY   = sy + height
        val swing   = if (isMoving) legSwing(legPhase) else 0f
        canvas.drawLine(sx + width * 0.32f, legTopY, sx + width * 0.32f - swing, feetY, legPaint)
        canvas.drawLine(sx + width * 0.68f, legTopY, sx + width * 0.68f + swing, feetY, legPaint)
    }

    /** Rounded paw at foot position. */
    private fun drawPaws(
        canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
        pawColor: Int, legPhase: Float, isMoving: Boolean
    ) {
        val feetY = sy + height
        val swing = if (isMoving) legSwing(legPhase) else 0f
        bodyPaint.color = pawColor
        canvas.drawOval(RectF(sx + width * 0.32f - swing - 8f, feetY - 7f,
            sx + width * 0.32f - swing + 8f, feetY + 4f), bodyPaint)
        canvas.drawOval(RectF(sx + width * 0.68f + swing - 8f, feetY - 7f,
            sx + width * 0.68f + swing + 8f, feetY + 4f), bodyPaint)
    }

    private fun drawEnhancedEyes(canvas: Canvas, hcx: Float, hcy: Float, headR: Float, irisColor: Int) {
        val eY = hcy - headR * 0.06f
        for (sign in floatArrayOf(-1f, 1f)) {
            val ex = hcx + sign * headR * 0.33f
            canvas.drawCircle(ex,                      eY,                   headR * 0.21f, whiteP)
            irisPaint.color = irisColor
            canvas.drawCircle(ex + headR * 0.040f,     eY + headR * 0.030f, headR * 0.14f, irisPaint)
            canvas.drawCircle(ex + headR * 0.055f,     eY + headR * 0.040f, headR * 0.08f, eyePaint)
            canvas.drawCircle(ex,                      eY - headR * 0.08f,  headR * 0.05f, whiteP)
        }
    }

    private fun drawSmile(canvas: Canvas, hcx: Float, hcy: Float, headR: Float,
                          color: Int = 0xFF5D4037.toInt()) {
        facePaint.color       = color
        facePaint.strokeWidth = 3f
        val r = headR * 0.25f
        canvas.drawArc(
            RectF(hcx - r, hcy + headR * 0.26f, hcx + r, hcy + headR * 0.62f),
            0f, 180f, false, facePaint
        )
    }

    private fun drawHorn(canvas: Canvas, cx: Float, tipY: Float, w: Float, h: Float, paint: Paint) {
        canvas.drawPath(Path().apply {
            moveTo(cx - w, tipY + h); lineTo(cx, tipY); lineTo(cx + w, tipY + h); close()
        }, paint)
    }

    /** Continuous sine-wave swing — returns ±14 px, smooth at any frame rate. */
    private fun legSwing(phase: Float) = sin(phase.toDouble()).toFloat() * 14f

    // ── CAT ───────────────────────────────────────────────────────────────────

    private fun drawCat(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                        cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFFFFA726.toInt()   // amber orange
        val dark  = 0xFFE65100.toInt()   // burnt orange
        val headR = width * 0.34f

        // Curling tail with stripe
        tailPaint.color = body; tailPaint.strokeWidth = 9f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.84f, sy + height * 0.62f)
            quadTo(sx + width * 1.46f, sy + height * 0.22f, sx + width * 1.18f, sy + height * -0.02f)
        }, tailPaint)
        tailPaint.color = dark; tailPaint.strokeWidth = 4f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.05f, sy + height * 0.20f)
            quadTo(sx + width * 1.22f, sy + height * 0.06f, sx + width * 1.18f, sy + height * -0.02f)
        }, tailPaint)
        tailPaint.color = Color.WHITE; tailPaint.strokeWidth = 5f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.22f, sy + height * 0.06f)
            quadTo(sx + width * 1.18f, sy + height * -0.02f, sx + width * 1.12f, sy + height * -0.05f)
        }, tailPaint)

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)

        // Tabby stripes on body
        facePaint.color = dark; facePaint.strokeWidth = 3.5f
        canvas.drawLine(cx - width * 0.22f, sy + height * 0.44f, cx - width * 0.10f, sy + height * 0.68f, facePaint)
        canvas.drawLine(cx,                 sy + height * 0.42f, cx,                 sy + height * 0.70f, facePaint)
        canvas.drawLine(cx + width * 0.22f, sy + height * 0.44f, cx + width * 0.10f, sy + height * 0.68f, facePaint)

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        drawPaws(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Triangular ears with pink inner
        for (sign in floatArrayOf(-1f, 1f)) {
            val eX = hcx + sign * headR * 0.55f
            val eY = hcy - headR * 0.78f
            val eW = headR * 0.30f; val eH = headR * 0.68f
            accentPaint.color = dark; accentPaint.style = Paint.Style.FILL
            canvas.drawPath(Path().apply { moveTo(eX - eW, eY + eH); lineTo(eX, eY); lineTo(eX + eW, eY + eH); close() }, accentPaint)
            accentPaint.color = 0xFFFF8A80.toInt()
            canvas.drawPath(Path().apply { moveTo(eX - eW * 0.50f, eY + eH * 0.92f); lineTo(eX, eY + eH * 0.14f); lineTo(eX + eW * 0.50f, eY + eH * 0.92f); close() }, accentPaint)
        }

        // Tabby forehead stripes
        facePaint.color = dark; facePaint.strokeWidth = 2.5f
        canvas.drawLine(hcx - headR * 0.30f, hcy - headR * 0.60f, hcx - headR * 0.22f, hcy - headR * 0.82f, facePaint)
        canvas.drawLine(hcx,                 hcy - headR * 0.58f, hcx,                 hcy - headR * 0.84f, facePaint)
        canvas.drawLine(hcx + headR * 0.30f, hcy - headR * 0.60f, hcx + headR * 0.22f, hcy - headR * 0.82f, facePaint)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFF388E3C.toInt())

        // Cheek blush
        accentPaint.color = 0x44FF8A80.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(hcx - headR * 0.72f, hcy + headR * 0.04f, hcx - headR * 0.36f, hcy + headR * 0.28f), accentPaint)
        canvas.drawOval(RectF(hcx + headR * 0.36f, hcy + headR * 0.04f, hcx + headR * 0.72f, hcy + headR * 0.28f), accentPaint)

        // Pink triangle nose
        accentPaint.color = 0xFFFF8A80.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(hcx, hcy + headR * 0.19f)
            lineTo(hcx - headR * 0.13f, hcy + headR * 0.32f)
            lineTo(hcx + headR * 0.13f, hcy + headR * 0.32f)
            close()
        }, accentPaint)

        // Whiskers (3 per side)
        facePaint.color = 0xFF8D6E63.toInt(); facePaint.strokeWidth = 1.8f
        for (sign in floatArrayOf(-1f, 1f)) {
            canvas.drawLine(hcx + sign * headR * 0.14f, hcy + headR * 0.18f, hcx + sign * headR * 0.94f, hcy + headR * 0.10f, facePaint)
            canvas.drawLine(hcx + sign * headR * 0.14f, hcy + headR * 0.27f, hcx + sign * headR * 0.94f, hcy + headR * 0.27f, facePaint)
            canvas.drawLine(hcx + sign * headR * 0.14f, hcy + headR * 0.34f, hcx + sign * headR * 0.94f, hcy + headR * 0.42f, facePaint)
        }
        drawSmile(canvas, hcx, hcy, headR)
    }

    // ── DOG ───────────────────────────────────────────────────────────────────

    private fun drawDog(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                        cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFFFFCC80.toInt()   // golden cream
        val dark  = 0xFF8D6E63.toInt()   // warm brown
        val headR = width * 0.34f

        // Wagging tail with a slight curl at tip
        tailPaint.color = 0xFFFFAB40.toInt(); tailPaint.strokeWidth = 12f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.84f, sy + height * 0.52f)
            quadTo(sx + width * 1.44f, sy + height * 0.26f, sx + width * 1.32f, sy + height * 0.44f)
        }, tailPaint)
        tailPaint.color = 0xFFFFF9C4.toInt(); tailPaint.strokeWidth = 5f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.38f, sy + height * 0.32f)
            quadTo(sx + width * 1.32f, sy + height * 0.44f, sx + width * 1.24f, sy + height * 0.46f)
        }, tailPaint)

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, 0xFFBF360C.toInt())

        // Brown body spots
        accentPaint.color = 0xFFBCAAA4.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(cx + width * 0.10f, sy + height * 0.44f, cx + width * 0.40f, sy + height * 0.60f), accentPaint)
        canvas.drawOval(RectF(cx - width * 0.38f, sy + height * 0.52f, cx - width * 0.12f, sy + height * 0.64f), accentPaint)

        // Collar (red fabric band + tag)
        accentPaint.color = 0xFFE53935.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(hcx - headR * 0.82f, hcy + headR * 0.74f, hcx + headR * 0.82f, hcy + headR * 0.96f),
            5f, 5f, accentPaint
        )
        // Tag (small gold circle hanging below collar centre)
        accentPaint.color = 0xFFFFD54F.toInt()
        canvas.drawCircle(hcx, hcy + headR * 1.10f, headR * 0.14f, accentPaint)
        canvas.drawCircle(hcx, hcy + headR * 1.10f, headR * 0.08f, eyePaint)

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        drawPaws(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Floppy hanging ears
        accentPaint.color = 0xFFFFAB40.toInt(); accentPaint.style = Paint.Style.FILL
        val earW = headR * 0.50f; val earH = headR * 1.04f
        canvas.drawRoundRect(RectF(hcx - headR - earW + 5f, hcy - headR * 0.28f,
            hcx - headR + 5f, hcy - headR * 0.28f + earH), 12f, 12f, accentPaint)
        canvas.drawRoundRect(RectF(hcx + headR - 5f, hcy - headR * 0.28f,
            hcx + headR + earW - 5f, hcy - headR * 0.28f + earH), 12f, 12f, accentPaint)
        // Inner ear shade
        accentPaint.color = 0xFFE6A04A.toInt()
        canvas.drawRoundRect(RectF(hcx - headR - earW * 0.65f + 5f, hcy - headR * 0.10f,
            hcx - headR + earW * 0.35f + 5f, hcy - headR * 0.10f + earH * 0.7f), 8f, 8f, accentPaint)
        canvas.drawRoundRect(RectF(hcx + headR + earW * 0.35f - 5f, hcy - headR * 0.10f,
            hcx + headR + earW * 1.35f - 5f, hcy - headR * 0.10f + earH * 0.7f), 8f, 8f, accentPaint)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFF6D4C41.toInt())

        // Cream snout
        bodyPaint.color = 0xFFFFE0B2.toInt()
        canvas.drawOval(RectF(hcx - headR * 0.38f, hcy + headR * 0.04f,
            hcx + headR * 0.38f, hcy + headR * 0.58f), bodyPaint)
        // Snout highlight
        canvas.drawOval(RectF(hcx - headR * 0.28f, hcy + headR * 0.06f,
            hcx,                  hcy + headR * 0.26f), highlightPaint)
        // Black oval nose
        canvas.drawOval(RectF(hcx - headR * 0.15f, hcy + headR * 0.06f,
            hcx + headR * 0.15f, hcy + headR * 0.26f), eyePaint)
        // Nose shine
        canvas.drawCircle(hcx - headR * 0.06f, hcy + headR * 0.10f, headR * 0.04f, whiteP)
        // Pink tongue
        accentPaint.color = 0xFFFF8A80.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(hcx - headR * 0.15f, hcy + headR * 0.46f,
            hcx + headR * 0.15f, hcy + headR * 0.76f), accentPaint)
        // Tongue crease line
        facePaint.color = 0xFFE91E63.toInt(); facePaint.strokeWidth = 1.5f
        canvas.drawLine(hcx, hcy + headR * 0.46f, hcx, hcy + headR * 0.72f, facePaint)
    }

    // ── RABBIT ────────────────────────────────────────────────────────────────

    private fun drawRabbit(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                           cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFFF5F5F5.toInt()   // off-white
        val dark  = 0xFF9E9E9E.toInt()   // grey
        val headR = width * 0.34f

        // White chest / tummy patch (behind arms)
        accentPaint.color = Color.WHITE; accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(sx + width * 0.25f, sy + height * 0.40f,
            sx + width * 0.75f, sy + height * 0.78f), accentPaint)

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Slightly rounder / wider body for chubby look
        bodyPaint.color = body
        outlinePaint.color = dark; outlinePaint.strokeWidth = 3f
        val bodyTop = sy + height * 0.36f; val bodyBot = sy + height * 0.81f
        val bodyRect = RectF(sx + width * 0.10f, bodyTop, sx + width * 0.90f, bodyBot)
        canvas.drawRoundRect(bodyRect, 18f, 18f, bodyPaint)
        canvas.drawRoundRect(bodyRect, 18f, 18f, outlinePaint)
        canvas.drawRoundRect(RectF(sx + width * 0.16f, bodyTop + 4f, sx + width * 0.64f,
            bodyTop + (bodyBot - bodyTop) * 0.44f), 8f, 8f, highlightPaint)

        // Head
        canvas.drawCircle(cx, sy + height * 0.26f, headR, bodyPaint)
        canvas.drawCircle(cx, sy + height * 0.26f, headR, outlinePaint)
        canvas.drawCircle(cx - headR * 0.20f, sy + height * 0.26f - headR * 0.26f, headR * 0.34f, highlightPaint)
        val hcx = cx; val hcy = sy + height * 0.26f

        // Cotton-ball tail
        canvas.drawCircle(sx + width * 0.92f, sy + height * 0.60f, 17f, whiteP)
        outlinePaint.color = dark; outlinePaint.strokeWidth = 2f
        canvas.drawCircle(sx + width * 0.92f, sy + height * 0.60f, 17f, outlinePaint)

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Large oval feet (rabbit-style)
        bodyPaint.color = body
        val swing = if (isMoving) legSwing(legPhase) else 0f
        canvas.drawOval(RectF(sx + width * 0.32f - swing - 12f, sy + height - 8f,
            sx + width * 0.32f - swing + 12f, sy + height + 6f), bodyPaint)
        canvas.drawOval(RectF(sx + width * 0.68f + swing - 12f, sy + height - 8f,
            sx + width * 0.68f + swing + 12f, sy + height + 6f), bodyPaint)
        canvas.drawOval(RectF(sx + width * 0.32f - swing - 12f, sy + height - 8f,
            sx + width * 0.32f - swing + 12f, sy + height + 6f), outlinePaint)
        canvas.drawOval(RectF(sx + width * 0.68f + swing - 12f, sy + height - 8f,
            sx + width * 0.68f + swing + 12f, sy + height + 6f), outlinePaint)

        // Tall oval ears with pink inner and vein
        val earW = headR * 0.32f; val earH = headR * 1.32f
        val earY = hcy - headR * 0.90f - earH
        for (sign in floatArrayOf(-1f, 1f)) {
            val eX = hcx + sign * headR * 0.52f
            bodyPaint.color = body
            canvas.drawOval(RectF(eX - earW, earY, eX + earW, earY + earH * 2f), bodyPaint)
            outlinePaint.color = dark; outlinePaint.strokeWidth = 2f
            canvas.drawOval(RectF(eX - earW, earY, eX + earW, earY + earH * 2f), outlinePaint)
            // Pink inner ear
            accentPaint.color = 0xFFFF8A80.toInt(); accentPaint.style = Paint.Style.FILL
            val iW = earW * 0.46f; val iH = earH * 0.76f
            canvas.drawOval(RectF(eX - iW, earY + earH * 0.24f, eX + iW, earY + earH * 0.24f + iH * 2f), accentPaint)
            // Ear centre vein
            facePaint.color = 0xFFE57373.toInt(); facePaint.strokeWidth = 1.5f
            canvas.drawLine(eX, earY + earH * 0.30f, eX, earY + earH * 2f - earH * 0.20f, facePaint)
        }

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFFEC407A.toInt())

        // Pink dot nose
        accentPaint.color = 0xFFFF8A80.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawCircle(hcx, hcy + headR * 0.22f, headR * 0.13f, accentPaint)

        // Buck teeth (two square white teeth)
        whiteP.color = Color.WHITE
        canvas.drawRoundRect(RectF(hcx - headR * 0.20f, hcy + headR * 0.34f, hcx - headR * 0.03f, hcy + headR * 0.62f), 3f, 3f, whiteP)
        canvas.drawRoundRect(RectF(hcx + headR * 0.03f, hcy + headR * 0.34f, hcx + headR * 0.20f, hcy + headR * 0.62f), 3f, 3f, whiteP)
        facePaint.color = dark; facePaint.strokeWidth = 1.5f
        canvas.drawLine(hcx, hcy + headR * 0.34f, hcx, hcy + headR * 0.62f, facePaint)
        outlinePaint.color = dark; outlinePaint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(hcx - headR * 0.20f, hcy + headR * 0.34f, hcx + headR * 0.20f, hcy + headR * 0.62f), 3f, 3f, outlinePaint)
    }

    // ── FOX ───────────────────────────────────────────────────────────────────

    private fun drawFox(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                        cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFFEF6C00.toInt()   // deep orange
        val dark  = 0xFFBF360C.toInt()   // dark red-orange
        val headR = width * 0.34f

        // Large fluffy tail with multiple layers
        tailPaint.color = body; tailPaint.strokeWidth = 22f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.78f, sy + height * 0.64f)
            quadTo(sx + width * 1.70f, sy + height * 0.44f, sx + width * 1.46f, sy + height * 0.10f)
        }, tailPaint)
        tailPaint.color = 0xFFFFCC02.toInt(); tailPaint.strokeWidth = 14f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.80f, sy + height * 0.62f)
            quadTo(sx + width * 1.62f, sy + height * 0.44f, sx + width * 1.42f, sy + height * 0.14f)
        }, tailPaint)
        // Cream tail tip
        tailPaint.color = 0xFFFFF9C4.toInt(); tailPaint.strokeWidth = 11f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.50f, sy + height * 0.16f)
            quadTo(sx + width * 1.46f, sy + height * 0.10f, sx + width * 1.38f, sy + height * 0.06f)
        }, tailPaint)

        // White underbelly
        accentPaint.color = 0xFFFFF9C4.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(sx + width * 0.22f, sy + height * 0.42f,
            sx + width * 0.72f, sy + height * 0.78f), accentPaint)

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)
        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        drawPaws(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Pointy ears with cream inner
        for (sign in floatArrayOf(-1f, 1f)) {
            val eX = hcx + sign * headR * 0.56f
            val eY = hcy - headR * 0.84f
            val eW = headR * 0.30f; val eH = headR * 0.76f
            accentPaint.color = body; accentPaint.style = Paint.Style.FILL
            canvas.drawPath(Path().apply { moveTo(eX - eW, eY + eH); lineTo(eX, eY); lineTo(eX + eW, eY + eH); close() }, accentPaint)
            // Dark outer edge of ear
            accentPaint.color = dark
            canvas.drawPath(Path().apply { moveTo(eX - eW, eY + eH); lineTo(eX - eW + 4f, eY + 6f); lineTo(eX, eY); close() }, accentPaint)
            canvas.drawPath(Path().apply { moveTo(eX + eW, eY + eH); lineTo(eX + eW - 4f, eY + 6f); lineTo(eX, eY); close() }, accentPaint)
            accentPaint.color = 0xFFFFF9C4.toInt()
            canvas.drawPath(Path().apply { moveTo(eX - eW * 0.44f, eY + eH * 0.88f); lineTo(eX, eY + eH * 0.10f); lineTo(eX + eW * 0.44f, eY + eH * 0.88f); close() }, accentPaint)
        }

        // Dark eye-mask markings (teardrop streaks below each eye)
        accentPaint.color = dark; accentPaint.style = Paint.Style.FILL
        for (sign in floatArrayOf(-1f, 1f)) {
            val ex = hcx + sign * headR * 0.33f
            val ey = hcy - headR * 0.06f
            canvas.drawPath(Path().apply {
                moveTo(ex - headR * 0.18f, ey + headR * 0.15f)
                quadTo(ex - headR * 0.12f * sign, ey + headR * 0.44f, ex, ey + headR * 0.38f)
                quadTo(ex + headR * 0.12f * sign, ey + headR * 0.44f, ex + headR * 0.18f, ey + headR * 0.15f)
            }, accentPaint)
        }

        // Cream muzzle
        accentPaint.color = 0xFFFFF9C4.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawOval(RectF(hcx - headR * 0.36f, hcy + headR * 0.04f,
            hcx + headR * 0.36f, hcy + headR * 0.58f), accentPaint)
        // Black nose with shine
        canvas.drawOval(RectF(hcx - headR * 0.13f, hcy + headR * 0.04f,
            hcx + headR * 0.13f, hcy + headR * 0.24f), eyePaint)
        canvas.drawCircle(hcx - headR * 0.06f, hcy + headR * 0.08f, headR * 0.04f, whiteP)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFF43A047.toInt())
        drawSmile(canvas, hcx, hcy, headR)
    }

    // ── T-REX ─────────────────────────────────────────────────────────────────

    private fun drawTRex(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                         cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFF388E3C.toInt()   // mid green
        val dark  = 0xFF1B5E20.toInt()   // forest green
        val headR = width * 0.34f

        // Thick balancing tail (extends right, counterbalancing the forward-leaning body)
        tailPaint.color = body; tailPaint.strokeWidth = 16f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.86f, sy + height * 0.58f)
            quadTo(sx + width * 1.60f, sy + height * 0.70f, sx + width * 1.90f, sy + height * 0.90f)
        }, tailPaint)
        tailPaint.strokeWidth = 9f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.52f, sy + height * 0.68f)
            quadTo(sx + width * 1.80f, sy + height * 0.76f, sx + width * 1.90f, sy + height * 0.90f)
        }, tailPaint)
        // Tail spikes
        accentPaint.color = dark; accentPaint.style = Paint.Style.FILL
        for (i in 0 until 3) {
            val tx = sx + width * (1.00f + i * 0.28f)
            val ty = sy + height * (0.60f + i * 0.08f)
            canvas.drawPath(Path().apply {
                moveTo(tx, ty); lineTo(tx + 8f, ty - 12f); lineTo(tx + 16f, ty + 4f); close()
            }, accentPaint)
        }

        // Tiny T-Rex arms close to chest
        armPaint.color = dark; armPaint.strokeWidth = 7f
        val armY = sy + height * 0.46f
        canvas.drawLine(cx - width * 0.06f, armY, cx + width * 0.24f, armY + height * 0.10f, armPaint)
        canvas.drawLine(cx + width * 0.06f, armY, cx - width * 0.24f, armY + height * 0.10f, armPaint)
        // Tiny claws
        armPaint.strokeWidth = 4f
        canvas.drawLine(cx + width * 0.24f, armY + height * 0.10f, cx + width * 0.30f, armY + height * 0.08f, armPaint)
        canvas.drawLine(cx + width * 0.24f, armY + height * 0.10f, cx + width * 0.28f, armY + height * 0.14f, armPaint)

        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)

        // Yellow belly
        bodyPaint.color = 0xFFFFF176.toInt()
        canvas.drawOval(RectF(sx + width * 0.22f, sy + height * 0.44f,
            sx + width * 0.78f, sy + height * 0.78f), bodyPaint)
        // Belly texture lines
        facePaint.color = 0xFFE6D44C.toInt(); facePaint.strokeWidth = 1.5f
        for (i in 0 until 3) {
            val ly = sy + height * (0.50f + i * 0.09f)
            canvas.drawLine(sx + width * 0.28f, ly, sx + width * 0.72f, ly, facePaint)
        }

        // Scale dots pattern on body
        accentPaint.color = dark; accentPaint.style = Paint.Style.FILL
        for (row in 0 until 3) {
            for (col in 0 until 4) {
                val dotX = sx + width * (0.24f + col * 0.155f) + (if (row % 2 == 1) width * 0.075f else 0f)
                val dotY = sy + height * (0.42f + row * 0.12f)
                canvas.drawCircle(dotX, dotY, 3.5f, accentPaint)
            }
        }

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Clawed feet
        legPaint.color = dark; legPaint.strokeWidth = 7f
        val swing = if (isMoving) legSwing(legPhase) else 0f
        for (sign in floatArrayOf(-1f, 1f)) {
            val fx = cx + sign * width * 0.18f + (if (sign < 0) -swing else swing)
            val fy = sy + height
            canvas.drawLine(fx, fy, fx - 10f, fy - 4f, legPaint)
            canvas.drawLine(fx, fy, fx + 2f, fy - 8f, legPaint)
            canvas.drawLine(fx, fy, fx + 11f, fy - 2f, legPaint)
        }

        // Head ridge spikes
        accentPaint.color = dark; accentPaint.style = Paint.Style.FILL
        for (i in 0 until 4) {
            val px = hcx - headR * 0.42f + i * headR * 0.28f
            val py = hcy - headR * 0.92f
            canvas.drawPath(Path().apply {
                moveTo(px - headR * 0.09f, py + headR * 0.16f)
                lineTo(px, py - headR * 0.26f)
                lineTo(px + headR * 0.09f, py + headR * 0.16f)
                close()
            }, accentPaint)
        }

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFFFFF176.toInt())

        // Nostrils
        canvas.drawCircle(hcx + headR * 0.20f, hcy + headR * 0.10f, headR * 0.09f, eyePaint)
        canvas.drawCircle(hcx + headR * 0.44f, hcy + headR * 0.10f, headR * 0.09f, eyePaint)

        // Open toothy grin — individual white teeth
        val mouthRect = RectF(hcx - headR * 0.38f, hcy + headR * 0.22f,
            hcx + headR * 0.58f, hcy + headR * 0.64f)
        // Dark open mouth
        accentPaint.color = 0xFF1A1A1A.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawArc(mouthRect, 0f, 180f, true, accentPaint)
        // White upper teeth
        whiteP.color = Color.WHITE
        val toothW = (mouthRect.right - mouthRect.left) / 5f
        for (t in 0 until 5) {
            val tx = mouthRect.left + t * toothW
            canvas.drawRect(tx + 1f, mouthRect.top, tx + toothW - 1f, mouthRect.top + 10f, whiteP)
        }
        facePaint.color = dark; facePaint.strokeWidth = 2.5f
        canvas.drawArc(mouthRect, 0f, 180f, false, facePaint)
    }

    // ── TRICERATOPS ───────────────────────────────────────────────────────────

    private fun drawTriceratops(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                                cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFF1565C0.toInt()   // royal blue
        val dark  = 0xFF0D47A1.toInt()   // deep blue
        val headR = width * 0.34f
        val preFrill = sy + height * 0.26f

        // Bony tail
        tailPaint.color = body; tailPaint.strokeWidth = 12f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.86f, sy + height * 0.54f)
            quadTo(sx + width * 1.48f, sy + height * 0.60f, sx + width * 1.62f, sy + height * 0.80f)
        }, tailPaint)
        tailPaint.color = dark; tailPaint.strokeWidth = 6f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 1.40f, sy + height * 0.60f)
            quadTo(sx + width * 1.55f, sy + height * 0.66f, sx + width * 1.62f, sy + height * 0.80f)
        }, tailPaint)

        // Detailed frill — drawn first so body/head sit in front
        // Base frill arc
        accentPaint.color = 0xFF1976D2.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawArc(
            RectF(cx - headR * 1.32f, preFrill - headR * 1.34f, cx + headR * 1.32f, preFrill + headR * 0.36f),
            180f, 180f, true, accentPaint
        )
        // Frill outer ridge (slightly darker arc outline)
        accentPaint.color = dark; accentPaint.style = Paint.Style.STROKE; accentPaint.strokeWidth = 5f
        canvas.drawArc(
            RectF(cx - headR * 1.32f, preFrill - headR * 1.34f, cx + headR * 1.32f, preFrill + headR * 0.36f),
            180f, 180f, false, accentPaint
        )
        accentPaint.style = Paint.Style.FILL

        // Frill vein lines radiating outward
        facePaint.color = 0xFF1565C0.toInt(); facePaint.strokeWidth = 2f
        for (i in 0..4) {
            val angle = Math.PI * (0.15 + i * 0.175)
            val x0 = cx.toDouble(); val y0 = preFrill.toDouble()
            canvas.drawLine(
                x0.toFloat(), y0.toFloat(),
                (x0 + cos(angle) * headR * 1.20).toFloat(),
                (y0 - sin(angle) * headR * 1.20).toFloat(),
                facePaint
            )
        }
        // Frill spots (scalloped edges)
        accentPaint.color = 0xFF0D47A1.toInt(); accentPaint.style = Paint.Style.FILL
        for (a in doubleArrayOf(Math.PI * 0.22, Math.PI * 0.50, Math.PI * 0.78)) {
            canvas.drawCircle(
                (cx + cos(a) * headR * 1.08f).toFloat(),
                (preFrill - headR * 0.46f + sin(a) * headR * 0.78f).toFloat(),
                headR * 0.13f, accentPaint
            )
        }
        // Inner frill glow
        accentPaint.color = 0x441E88E5.toInt()
        canvas.drawArc(
            RectF(cx - headR * 0.90f, preFrill - headR * 0.90f, cx + headR * 0.90f, preFrill + headR * 0.20f),
            180f, 180f, true, accentPaint
        )
        accentPaint.style = Paint.Style.FILL

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)

        // Armour dots on body
        accentPaint.color = dark
        for (row in 0 until 2) {
            for (col in 0 until 4) {
                val dotX = sx + width * (0.22f + col * 0.155f)
                val dotY = sy + height * (0.48f + row * 0.14f)
                canvas.drawCircle(dotX, dotY, 4f, accentPaint)
            }
        }

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        drawPaws(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Three ivory horns (large main + two brow)
        accentPaint.color = 0xFFFFF9C4.toInt(); accentPaint.style = Paint.Style.FILL
        drawHorn(canvas, hcx, hcy - headR * 1.12f, headR * 0.18f, headR * 0.64f, accentPaint)
        drawHorn(canvas, hcx - headR * 0.52f, hcy - headR * 0.78f, headR * 0.12f, headR * 0.40f, accentPaint)
        drawHorn(canvas, hcx + headR * 0.52f, hcy - headR * 0.78f, headR * 0.12f, headR * 0.40f, accentPaint)
        // Horn shading
        accentPaint.color = 0xFFEFE0A0.toInt()
        drawHorn(canvas, hcx - headR * 0.04f, hcy - headR * 1.12f, headR * 0.07f, headR * 0.64f, accentPaint)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFF42A5F5.toInt())
        drawSmile(canvas, hcx, hcy, headR)
    }

    // ── STEGOSAURUS ───────────────────────────────────────────────────────────

    private fun drawStegosaurus(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                                cx: Float, legPhase: Float, isMoving: Boolean) {
        val body  = 0xFF00695C.toInt()   // deep teal
        val dark  = 0xFF004D40.toInt()   // darkest teal
        val headR = width * 0.34f

        // Spiked thagomizer tail (4 spikes at the tail tip)
        tailPaint.color = body; tailPaint.strokeWidth = 13f
        canvas.drawPath(Path().apply {
            moveTo(sx + width * 0.86f, sy + height * 0.58f)
            quadTo(sx + width * 1.52f, sy + height * 0.64f, sx + width * 1.70f, sy + height * 0.72f)
        }, tailPaint)
        // Tail spikes
        accentPaint.color = 0xFF26A69A.toInt(); accentPaint.style = Paint.Style.FILL
        val spikeAngles = floatArrayOf(-30f, -10f, 10f, 30f)
        for (angle in spikeAngles) {
            val rad = Math.toRadians(angle.toDouble())
            val bX = sx + width * 1.68f; val bY = sy + height * 0.72f
            canvas.drawPath(Path().apply {
                moveTo(bX - 6f, bY)
                lineTo((bX + cos(rad) * 22f).toFloat(), (bY - sin(rad) * 22f).toFloat())
                lineTo(bX + 6f, bY)
                close()
            }, accentPaint)
        }

        // Five back plates with colour gradient (teal centre, orange tips)
        val plateCxs    = floatArrayOf(
            cx - width * 0.30f, cx - width * 0.15f, cx, cx + width * 0.15f, cx + width * 0.30f
        )
        val plateColors = intArrayOf(
            0xFF26A69A.toInt(), 0xFF4DB6AC.toInt(), 0xFF80CBC4.toInt(),
            0xFF4DB6AC.toInt(), 0xFF26A69A.toInt()
        )
        val plateAccent = intArrayOf(
            0xFFFF7043.toInt(), 0xFFFF8A65.toInt(), 0xFFFFAB91.toInt(),
            0xFFFF8A65.toInt(), 0xFFFF7043.toInt()
        )
        for (i in plateCxs.indices) {
            val pCX = plateCxs[i]; val pBaseY = sy + height * 0.44f
            val pW = width * (0.10f - (0.016f * kotlin.math.abs(i - 2)))
            val pHt = height * (0.28f - (0.04f * kotlin.math.abs(i - 2)))
            // Plate fill
            accentPaint.color = plateColors[i]; accentPaint.style = Paint.Style.FILL
            val pp = Path().apply {
                moveTo(pCX, pBaseY - pHt)
                lineTo(pCX + pW, pBaseY)
                lineTo(pCX, pBaseY + height * 0.04f)
                lineTo(pCX - pW, pBaseY)
                close()
            }
            canvas.drawPath(pp, accentPaint)
            // Plate tip accent colour
            accentPaint.color = plateAccent[i]
            val tp = Path().apply {
                moveTo(pCX, pBaseY - pHt)
                lineTo(pCX + pW * 0.5f, pBaseY - pHt * 0.55f)
                lineTo(pCX - pW * 0.5f, pBaseY - pHt * 0.55f)
                close()
            }
            canvas.drawPath(tp, accentPaint)
            outlinePaint.color = dark; outlinePaint.strokeWidth = 2f
            canvas.drawPath(pp, outlinePaint)
        }

        drawArms(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)

        // Stripe markings along body
        facePaint.color = dark; facePaint.strokeWidth = 3f
        for (i in 0 until 3) {
            val ly = sy + height * (0.46f + i * 0.11f)
            canvas.drawLine(sx + width * 0.18f, ly, sx + width * 0.82f, ly, facePaint)
        }

        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)
        drawPaws(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Long flat snout with detail
        bodyPaint.color = body
        canvas.drawRoundRect(RectF(hcx + headR * 0.58f, hcy + headR * 0.04f,
            hcx + headR * 1.58f, hcy + headR * 0.46f), 8f, 8f, bodyPaint)
        outlinePaint.color = dark; outlinePaint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(hcx + headR * 0.58f, hcy + headR * 0.04f,
            hcx + headR * 1.58f, hcy + headR * 0.46f), 8f, 8f, outlinePaint)
        // Snout highlight
        canvas.drawRoundRect(RectF(hcx + headR * 0.62f, hcy + headR * 0.06f,
            hcx + headR * 1.20f, hcy + headR * 0.22f), 5f, 5f, highlightPaint)
        // Nostril
        canvas.drawCircle(hcx + headR * 1.34f, hcy + headR * 0.18f, headR * 0.09f, eyePaint)
        canvas.drawCircle(hcx + headR * 1.34f - 2f, hcy + headR * 0.16f, headR * 0.04f, whiteP)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFF80CBC4.toInt())
    }

    // ── PTERODACTYL ───────────────────────────────────────────────────────────

    private fun drawPterodactyl(canvas: Canvas, sx: Float, sy: Float, width: Float, height: Float,
                                cx: Float, legPhase: Float, isMoving: Boolean) {
        val body     = 0xFF7B1FA2.toInt()   // deep purple
        val dark     = 0xFF4A148C.toInt()   // darkest purple
        val headR    = width * 0.34f
        val wingSpan = width * 1.18f
        val wingY    = sy + height * 0.50f

        // Filled membrane wings — left and right with primary + secondary structure
        // Left wing
        accentPaint.color = 0xFF9C27B0.toInt(); accentPaint.style = Paint.Style.FILL
        val lWing = Path().apply {
            moveTo(cx, wingY - height * 0.06f)
            quadTo(cx - wingSpan * 0.68f, wingY - height * 0.30f, cx - wingSpan * 0.62f, wingY + height * 0.16f)
            quadTo(cx - wingSpan * 0.28f, wingY + height * 0.10f, cx, wingY + height * 0.06f)
            close()
        }
        canvas.drawPath(lWing, accentPaint)

        // Right wing
        val rWing = Path().apply {
            moveTo(cx, wingY - height * 0.06f)
            quadTo(cx + wingSpan * 0.68f, wingY - height * 0.30f, cx + wingSpan * 0.62f, wingY + height * 0.16f)
            quadTo(cx + wingSpan * 0.28f, wingY + height * 0.10f, cx, wingY + height * 0.06f)
            close()
        }
        canvas.drawPath(rWing, accentPaint)

        // Primary vein on each wing
        facePaint.color = dark; facePaint.strokeWidth = 2.5f
        canvas.drawLine(cx, wingY, cx - wingSpan * 0.60f, wingY - height * 0.14f, facePaint)
        canvas.drawLine(cx, wingY, cx + wingSpan * 0.60f, wingY - height * 0.14f, facePaint)

        // Secondary veins (4 per wing)
        facePaint.strokeWidth = 1.5f
        for (i in 1..4) {
            val t = i * 0.14f
            // Left
            val lWx = cx - wingSpan * t * 0.62f
            val lWy = wingY - height * 0.14f * t + height * 0.04f
            canvas.drawLine(lWx, lWy, lWx - wingSpan * 0.04f, lWy + height * 0.18f, facePaint)
            // Right
            val rWx = cx + wingSpan * t * 0.62f
            canvas.drawLine(rWx, lWy, rWx + wingSpan * 0.04f, lWy + height * 0.18f, facePaint)
        }

        // Dark edge outline
        accentPaint.color = dark; accentPaint.style = Paint.Style.STROKE; accentPaint.strokeWidth = 2.5f
        canvas.drawPath(lWing, accentPaint); canvas.drawPath(rWing, accentPaint)
        accentPaint.style = Paint.Style.FILL

        val (hcx, hcy) = drawBodyAndHead(canvas, sx, sy, width, height, cx, body, dark)
        drawLegs(canvas, sx, sy, width, height, dark, legPhase, isMoving)

        // Talons (two curved claws per foot)
        legPaint.color = dark; legPaint.strokeWidth = 5f
        val feetY = sy + height
        val swing = if (isMoving) legSwing(legPhase) else 0f
        for (sign in floatArrayOf(-1f, 1f)) {
            val fx = cx + sign * width * 0.18f + (if (sign < 0) -swing else swing)
            canvas.drawLine(fx, feetY, fx - 8f, feetY - 6f, legPaint)
            canvas.drawLine(fx, feetY, fx + 8f, feetY - 4f, legPaint)
            canvas.drawLine(fx, feetY, fx + 2f, feetY + 8f, legPaint)
        }

        // Long swept head crest
        accentPaint.color = 0xFFCE93D8.toInt(); accentPaint.style = Paint.Style.FILL
        canvas.drawPath(Path().apply {
            moveTo(hcx - headR * 0.36f, hcy - headR * 0.94f)
            lineTo(hcx + headR * 0.24f, hcy - headR * 1.92f)
            lineTo(hcx + headR * 0.70f, hcy - headR * 0.90f)
            close()
        }, accentPaint)
        // Crest highlight
        accentPaint.color = 0xFFE1BEE7.toInt()
        canvas.drawPath(Path().apply {
            moveTo(hcx - headR * 0.20f, hcy - headR * 0.94f)
            lineTo(hcx + headR * 0.24f, hcy - headR * 1.92f)
            lineTo(hcx + headR * 0.38f, hcy - headR * 0.90f)
            close()
        }, accentPaint)
        // Crest outline
        accentPaint.color = dark; accentPaint.style = Paint.Style.STROKE; accentPaint.strokeWidth = 2f
        canvas.drawPath(Path().apply {
            moveTo(hcx - headR * 0.36f, hcy - headR * 0.94f)
            lineTo(hcx + headR * 0.24f, hcy - headR * 1.92f)
            lineTo(hcx + headR * 0.70f, hcy - headR * 0.90f)
        }, accentPaint)
        accentPaint.style = Paint.Style.FILL

        // Pointed beak (upper + lower jaw)
        accentPaint.color = 0xFFFFF9C4.toInt(); accentPaint.style = Paint.Style.FILL
        // Upper jaw
        canvas.drawPath(Path().apply {
            moveTo(hcx + headR * 0.80f, hcy + headR * 0.02f)
            lineTo(hcx + headR * 1.68f, hcy + headR * 0.18f)
            lineTo(hcx + headR * 0.80f, hcy + headR * 0.28f)
            close()
        }, accentPaint)
        // Lower jaw (slightly below)
        canvas.drawPath(Path().apply {
            moveTo(hcx + headR * 0.80f, hcy + headR * 0.28f)
            lineTo(hcx + headR * 1.60f, hcy + headR * 0.36f)
            lineTo(hcx + headR * 0.80f, hcy + headR * 0.44f)
            close()
        }, accentPaint)
        // Beak outline
        facePaint.color = 0xFFE6C84A.toInt(); facePaint.strokeWidth = 2f
        canvas.drawLine(hcx + headR * 0.80f, hcy + headR * 0.28f, hcx + headR * 1.68f, hcy + headR * 0.18f, facePaint)

        drawEnhancedEyes(canvas, hcx, hcy, headR, 0xFFCE93D8.toInt())
    }
}
