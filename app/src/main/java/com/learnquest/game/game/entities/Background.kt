package com.learnquest.game.game.entities

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

/**
 * Draws the multi-layered parallax background.
 *
 * Sky section (0 → groundTop):
 *   1. 4-stop deep-blue sky gradient
 *   2. Sun core + radial glow halo
 *   3. Far mountains silhouette with snow caps  (5%  parallax)
 *   4. Near mountains silhouette               (12% parallax)
 *   5. Far hills – cyan                        (22% parallax)
 *   6. Near hills – bright green               (50% parallax)
 *   7. Mid-ground trees                        (65% parallax)
 *   8. Fluffy clouds with shade layer          (8%  parallax)
 *   9. Horizon shadow blend
 *
 * Floor section (groundTop → screenH):
 *  10.  Warm-stone checker tiles (scrolls with camera)
 *  11.  Perspective radiating lines (vanish point tracks camera slightly)
 *  12.  Horizontal perspective lines (t² distribution = denser near horizon)
 *  13.  Grass strip + dark edge at top of floor
 *  14.  Near-ground bushes & rocks              (85% parallax)
 */
class Background {

    // ── Paints ────────────────────────────────────────────────────────────────

    private val skyPaint         = Paint()
    private val sunCorePaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF59D.toInt() }
    private val sunHaloPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val cloudShadePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDEECFF.toInt() }
    private val mtnFarPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5C6BC0.toInt() }
    private val mtnNearPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7986CB.toInt() }
    private val snowCapPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDDFFFFFF.toInt() }
    private val farHillPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF80DEEA.toInt() }
    private val nearHillPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF66BB6A.toInt() }
    private val horizonPaint     = Paint()
    private val grassPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4CAF50.toInt() }
    private val grassDarkPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF388E3C.toInt() }
    private val floorBaseA       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBCAAA4.toInt() }
    private val floorBaseB       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA1887F.toInt() }
    private val perspPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = 0x28000000.toInt()
        strokeWidth = 1.5f
        style       = Paint.Style.STROKE
    }

    // ── Mid-ground tree paints ────────────────────────────────────────────────
    private val treeTrunkPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5D4037.toInt() }
    private val treeCanopyPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF388E3C.toInt() }
    private val treeCanopyDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2E7D32.toInt() }

    // ── Near-ground element paints ────────────────────────────────────────────
    private val bushPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF33691E.toInt() }
    private val bushLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF558B2F.toInt() }
    private val rockPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF90A4AE.toInt() }
    private val rockDarkPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF546E7A.toInt() }

    // ── Shader cache (avoid per-frame allocation for fixed gradients) ─────────

    private var lastGTop        = -1f
    private var lastSunX        = -1f
    private var lastSunY        = -1f
    private var lastHorizonGTop = -1f

    // ── Cloud definitions: (worldX in [0, period), yFraction, scale) ─────────

    private val cloudPeriod = 2800f
    private val cloudDefs   = listOf(
        Triple(   0f, 0.14f, 1.00f),
        Triple( 540f, 0.32f, 0.78f),
        Triple(1080f, 0.10f, 1.28f),
        Triple(1580f, 0.26f, 0.90f),
        Triple(2100f, 0.18f, 1.12f),
        Triple(2600f, 0.36f, 0.70f)
    )

    // ── Draw ──────────────────────────────────────────────────────────────────

    fun draw(canvas: Canvas, cameraX: Float, screenW: Int, screenH: Int, groundTop: Int) {
        val gTop = groundTop.toFloat()
        val sw   = screenW.toFloat()
        val sh   = screenH.toFloat()

        // ── 1. Sky gradient ──────────────────────────────────────────────────
        if (lastGTop != gTop) {
            skyPaint.shader = LinearGradient(
                0f, 0f, 0f, gTop,
                intArrayOf(
                    0xFF0D47A1.toInt(),   // deep blue at zenith
                    0xFF1976D2.toInt(),   // royal blue
                    0xFF42A5F5.toInt(),   // sky blue
                    0xFFBBDEFB.toInt()    // pale at horizon
                ),
                floatArrayOf(0f, 0.25f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            lastGTop = gTop
        }
        canvas.drawRect(0f, 0f, sw, gTop, skyPaint)

        // ── 2. Sun + radial glow ─────────────────────────────────────────────
        val sunX = sw * 0.82f
        val sunY = gTop * 0.22f
        if (lastSunX != sunX || lastSunY != sunY) {
            sunHaloPaint.shader = RadialGradient(
                sunX, sunY, 108f,
                intArrayOf(0xCCFFF9C4.toInt(), 0x66FFD740.toInt(), 0x00FFD740.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            lastSunX = sunX; lastSunY = sunY
        }
        canvas.drawCircle(sunX, sunY, 108f, sunHaloPaint)
        canvas.drawCircle(sunX, sunY,  52f, sunCorePaint)

        // ── 3. Far mountains (5% parallax, jagged pairs per period) ─────────
        drawRepeating(canvas, cameraX * 0.05f, 680f, screenW) { c, ox ->
            drawMountain(c, ox + 20f,  gTop, 310f, 145f, mtnFarPaint)
            drawMountain(c, ox + 270f, gTop, 230f,  96f, mtnFarPaint)
        }

        // ── 4. Near mountains (12% parallax) ────────────────────────────────
        drawRepeating(canvas, cameraX * 0.12f, 720f, screenW) { c, ox ->
            drawMountain(c, ox,        gTop, 250f,  92f, mtnNearPaint)
            drawMountain(c, ox + 310f, gTop, 185f,  70f, mtnNearPaint)
        }

        // ── 5. Far hills (22% parallax) ─────────────────────────────────────
        drawRepeating(canvas, cameraX * 0.22f, 520f, screenW) { c, ox ->
            drawHill(c, ox, gTop - 16f, 340f, 118f, farHillPaint)
        }

        // ── 6. Near hills (50% parallax) ────────────────────────────────────
        drawRepeating(canvas, cameraX * 0.50f, 380f, screenW) { c, ox ->
            drawHill(c, ox, gTop - 6f, 220f, 68f, nearHillPaint)
        }

        // ── 7. Mid-ground trees (65% parallax) ──────────────────────────────
        drawRepeating(canvas, cameraX * 0.65f, 620f, screenW) { c, ox ->
            drawTree(c, ox + 70f,  gTop, 14f, 58f, 42f)
            drawTree(c, ox + 270f, gTop, 10f, 40f, 30f)
            drawTree(c, ox + 450f, gTop, 16f, 66f, 48f)
        }

        // ── 8. Clouds (8% parallax, wrap at cloudPeriod) ────────────────────
        val cloudOff = cameraX * 0.08f
        cloudDefs.forEach { (wX, yFrac, sc) ->
            val cx = ((wX - cloudOff) % cloudPeriod + cloudPeriod) % cloudPeriod
            if (cx < sw + 360f)             drawCloud(canvas, cx,               gTop * yFrac, sc)
            if (cx - cloudPeriod > -360f)   drawCloud(canvas, cx - cloudPeriod, gTop * yFrac, sc)
        }

        // ── 9. Horizon shadow blend ──────────────────────────────────────────
        if (lastHorizonGTop != gTop) {
            horizonPaint.shader = LinearGradient(
                0f, gTop - 22f, 0f, gTop + 18f,
                0x00000000, 0x44000000, Shader.TileMode.CLAMP
            )
            lastHorizonGTop = gTop
        }
        canvas.drawRect(0f, gTop - 22f, sw, gTop + 18f, horizonPaint)

        // ── 10–13. Floor ─────────────────────────────────────────────────────
        drawFloor(canvas, cameraX, sw, sh, gTop)

        // ── 14. Near-ground bushes & rocks (85% parallax) ───────────────────
        drawNearGround(canvas, cameraX, sw, gTop)
    }

    // ── Floor ─────────────────────────────────────────────────────────────────

    private fun drawFloor(canvas: Canvas, cameraX: Float, sw: Float, sh: Float, gTop: Float) {
        // Base stone colour
        canvas.drawRect(0f, gTop, sw, sh, floorBaseA)

        // Checker tiles (scrolling)
        val tileW  = 140f
        val tileH  = 90f
        val xOff   = -(cameraX % tileW)
        val colOff = (cameraX / tileW).toInt()
        val rows   = ((sh - gTop) / tileH).toInt() + 2
        val cols   = (sw / tileW).toInt() + 3
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if ((row + col + colOff) % 2 == 1) {
                    val l = xOff + col * tileW
                    val t = gTop + row * tileH
                    canvas.drawRect(l, t, l + tileW, t + tileH, floorBaseB)
                }
            }
        }

        // Perspective lines — vanishing point tracks camera slightly so floor feels 3-D
        val vX = (sw / 2f - cameraX * 0.03f).coerceIn(sw * 0.15f, sw * 0.85f)
        for (i in 0..24) {
            canvas.drawLine(vX, gTop, sw * i / 24f, sh, perspPaint)
        }

        // Horizontal perspective lines — t² gives denser spacing near the top
        val floorH = sh - gTop
        for (i in 1..10) {
            val t = i / 10f
            canvas.drawLine(0f, gTop + floorH * (t * t), sw, gTop + floorH * (t * t), perspPaint)
        }

        // Grass strip at top of floor (drawn last so it sits on top of tiles/lines)
        canvas.drawRect(0f, gTop,        sw, gTop + 22f, grassPaint)
        canvas.drawRect(0f, gTop + 20f, sw, gTop + 26f, grassDarkPaint)
    }

    // ── Near-ground elements ──────────────────────────────────────────────────

    private fun drawNearGround(canvas: Canvas, cameraX: Float, sw: Float, gTop: Float) {
        drawRepeating(canvas, cameraX * 0.85f, 500f, sw.toInt()) { c, ox ->
            drawBush(c, ox + 55f,  gTop + 18f, 32f, 20f)
            drawBush(c, ox + 210f, gTop + 14f, 20f, 14f)
            drawRock(c, ox + 155f, gTop + 22f, 17f, 11f)
            drawBush(c, ox + 360f, gTop + 20f, 40f, 24f)
            drawRock(c, ox + 440f, gTop + 18f, 12f,  8f)
        }
    }

    // ── Reusable draw helpers ─────────────────────────────────────────────────

    /** Repeats [drawer] at [period] intervals covering the full screen width. */
    private fun drawRepeating(
        canvas: Canvas, cameraOffset: Float, period: Float, screenW: Int,
        drawer: (Canvas, Float) -> Unit
    ) {
        val offset = cameraOffset % period
        var sx = -offset - period
        while (sx < screenW + period) {
            drawer(canvas, sx)
            sx += period
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        val r = 42f * scale
        // Grey-blue shade layer (slightly below the white layer)
        canvas.drawCircle(cx,             cy + r * 0.18f, r * 0.88f, cloudShadePaint)
        canvas.drawCircle(cx + r,         cy + r * 0.15f, r * 0.68f, cloudShadePaint)
        canvas.drawCircle(cx - r,         cy + r * 0.15f, r * 0.60f, cloudShadePaint)
        canvas.drawCircle(cx + r * 1.85f, cy + r * 0.12f, r * 0.48f, cloudShadePaint)
        // White body (5 overlapping circles)
        canvas.drawCircle(cx,             cy,             r,          cloudPaint)
        canvas.drawCircle(cx + r,         cy,             r * 0.72f,  cloudPaint)
        canvas.drawCircle(cx - r,         cy,             r * 0.64f,  cloudPaint)
        canvas.drawCircle(cx + r * 1.9f,  cy,             r * 0.52f,  cloudPaint)
        canvas.drawCircle(cx - r * 1.7f,  cy,             r * 0.44f,  cloudPaint)
    }

    /** Jagged mountain with a snow cap at the peak. */
    private fun drawMountain(canvas: Canvas, x: Float, baseY: Float, w: Float, h: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(x, baseY)
            lineTo(x + w * 0.40f, baseY - h)
            lineTo(x + w * 0.56f, baseY - h * 0.82f)
            lineTo(x + w * 0.74f, baseY - h * 0.94f)
            lineTo(x + w, baseY)
            close()
        }
        canvas.drawPath(path, paint)
        // Snow cap triangle
        val snow = Path().apply {
            moveTo(x + w * 0.40f, baseY - h)
            lineTo(x + w * 0.26f, baseY - h * 0.68f)
            lineTo(x + w * 0.54f, baseY - h * 0.68f)
            close()
        }
        canvas.drawPath(snow, snowCapPaint)
    }

    /** Smooth rounded hill using a cubic bezier arc. */
    private fun drawHill(canvas: Canvas, x: Float, baseY: Float, w: Float, h: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(x, baseY)
            cubicTo(x, baseY - h, x + w, baseY - h, x + w, baseY)
            close()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * Mid-ground tree: rounded canopy over a slim trunk, sitting on the horizon.
     * [trunkW] and [trunkH] are trunk dimensions; [canopyR] is canopy radius.
     */
    private fun drawTree(canvas: Canvas, x: Float, baseY: Float, trunkW: Float, trunkH: Float, canopyR: Float) {
        // Trunk
        canvas.drawRect(x - trunkW / 2f, baseY - trunkH, x + trunkW / 2f, baseY, treeTrunkPaint)
        val canopyY = baseY - trunkH - canopyR * 0.55f
        // Shadow layer (offset down + slightly larger, drawn first)
        canvas.drawCircle(x + 3f, canopyY + 5f, canopyR * 0.92f, treeCanopyDarkPaint)
        // Main canopy
        canvas.drawCircle(x, canopyY, canopyR, treeCanopyPaint)
    }

    /** Organic bush cluster (two overlapping ovals). */
    private fun drawBush(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawOval(RectF(x - w,          y - h,          x + w,          y), bushPaint)
        canvas.drawOval(RectF(x - w * 0.55f,  y - h * 1.25f,  x + w * 0.55f,  y - h * 0.15f), bushLightPaint)
    }

    /** Small rounded rock with a highlight. */
    private fun drawRock(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawOval(RectF(x - w,          y - h,        x + w,          y),           rockDarkPaint)
        canvas.drawOval(RectF(x - w * 0.75f,  y - h * 0.9f, x + w * 0.55f,  y - h * 0.2f), rockPaint)
    }
}
