package com.learnquest.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import com.learnquest.game.game.questions.QuestionBank
import kotlin.math.cos
import kotlin.math.sin

/**
 * World Map — shows 5 level nodes connected by a winding path.
 *
 * D-pad left/right navigates between unlocked nodes.
 * OK/Enter launches the selected level.
 * Back returns to character selection.
 *
 * Receives [EXTRA_MODE] and [EXTRA_CHARACTER] from [CharacterSelectActivity];
 * forwards them, plus the chosen level number, to [GameActivity].
 */
class WorldMapActivity : Activity() {

    companion object {
        const val EXTRA_MODE      = "mode"
        const val EXTRA_CHARACTER = "character"
    }

    private var modeName  = QuestionBank.GameMode.MIXED.name
    private var character = "CAT"

    private var selectedLevel = 1
    private var maxUnlocked   = 1

    private lateinit var mapView: WorldMapView
    private val pulseHandler = Handler(Looper.getMainLooper())
    private val pulseRunnable = object : Runnable {
        override fun run() {
            mapView.advancePulse()
            mapView.invalidate()
            pulseHandler.postDelayed(this, 40L)   // ~25 fps pulse animation
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modeName  = intent.getStringExtra(EXTRA_MODE)      ?: QuestionBank.GameMode.MIXED.name
        character = intent.getStringExtra(EXTRA_CHARACTER) ?: "CAT"

        maxUnlocked   = LevelProgress.getMaxUnlocked(this)
        selectedLevel = maxUnlocked.coerceAtMost(LevelProgress.TOTAL_LEVELS)

        mapView = WorldMapView(this)
        val container = FrameLayout(this)
        container.addView(mapView)
        setContentView(container)

        mapView.isFocusable            = true
        mapView.isFocusableInTouchMode = true
        mapView.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        maxUnlocked   = LevelProgress.getMaxUnlocked(this)
        selectedLevel = selectedLevel.coerceAtMost(maxUnlocked).coerceAtMost(LevelProgress.TOTAL_LEVELS)
        mapView.invalidate()
        pulseHandler.post(pulseRunnable)
    }

    override fun onPause() {
        super.onPause()
        pulseHandler.removeCallbacks(pulseRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val next = selectedLevel + 1
                if (next <= LevelProgress.TOTAL_LEVELS && LevelProgress.isUnlocked(this, next)) {
                    selectedLevel = next
                    mapView.invalidate()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedLevel > 1) { selectedLevel--; mapView.invalidate() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                launchLevel(selectedLevel)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun launchLevel(level: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_MODE,      modeName)
            putExtra(GameActivity.EXTRA_LEVEL,     level)
            putExtra(GameActivity.EXTRA_CHARACTER, character)
        }
        startActivity(intent)
    }

    // ── Inner View ────────────────────────────────────────────────────────────

    inner class WorldMapView(context: Context) : View(context) {

        private var pulseTick = 0f

        fun advancePulse() { pulseTick += 0.08f }

        // ── Paints ────────────────────────────────────────────────────────────

        // Background sky gradient (recreated when height is known)
        private val skyPaint    = Paint()
        private val hillFarP    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#388E3C") }
        private val hillNearP   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4CAF50") }
        private val hillDarkP   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
        private val cloudP      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
        private val sunP        = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF176") }
        private val sunGlowP    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFEB3B.toInt() }

        // Path connecting nodes
        private val pathPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 22f
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
            color       = Color.parseColor("#795548")   // dirt path
        }
        private val pathEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 26f
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
            color       = Color.parseColor("#4E342E")
        }
        private val pathDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap   = Paint.Cap.ROUND
            color       = Color.parseColor("#BCAAA4")
        }

        // Node states
        private val nodeLockedP    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#616161") }
        private val nodeUnlockP    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0") }
        private val nodeSelP       = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1976D2") }
        private val nodeGlowP      = Paint(Paint.ANTI_ALIAS_FLAG)
        private val nodeOutlineP   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 5f
            color       = Color.WHITE
        }
        private val nodeLockOutP   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 4f
            color       = Color.parseColor("#9E9E9E")
        }
        private val nodeSelOutP    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            color       = Color.parseColor("#FFD54F")
        }

        private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color          = Color.WHITE
            textAlign      = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color          = Color.WHITE
            textAlign      = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color          = Color.WHITE
            textAlign      = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = Color.parseColor("#90CAF9")
            textAlign = Paint.Align.CENTER
        }
        private val lockIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = Color.parseColor("#9E9E9E")
            style       = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val starP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
        private val starOutP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = Color.parseColor("#FF8F00")
            style       = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        private var lastH = 0

        // ── Draw entry ────────────────────────────────────────────────────────

        override fun onDraw(canvas: Canvas) {
            val W = width.toFloat()
            val H = height.toFloat()
            if (W == 0f || H == 0f) return

            // Rebuild sky gradient if height changed
            if (height != lastH) {
                lastH = height
                skyPaint.shader = LinearGradient(
                    0f, 0f, 0f, H * 0.68f,
                    intArrayOf(
                        Color.parseColor("#0D47A1"),
                        Color.parseColor("#1565C0"),
                        Color.parseColor("#42A5F5"),
                        Color.parseColor("#B3E5FC")
                    ),
                    floatArrayOf(0f, 0.30f, 0.70f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

            drawBackground(canvas, W, H)

            // Node world positions (as fraction of W, H)
            val nodePos = listOf(
                Pair(0.13f, 0.70f),   // level 1
                Pair(0.31f, 0.35f),   // level 2
                Pair(0.50f, 0.66f),   // level 3
                Pair(0.69f, 0.32f),   // level 4
                Pair(0.87f, 0.60f)    // level 5
            )
            val nodeR = (W * 0.045f).coerceAtMost(50f)

            // Draw winding path (back edge, then fill, then dashes)
            drawPath(canvas, nodePos, W, H, nodeR)

            // Draw each level node
            nodePos.forEachIndexed { i, (fx, fy) ->
                val level   = i + 1
                val nx      = fx * W
                val ny      = fy * H
                drawNode(canvas, nx, ny, nodeR, level, W, H)
            }

            // Title + subtitle
            titlePaint.textSize    = (H * 0.072f).coerceAtLeast(28f)
            subtitlePaint.textSize = (H * 0.038f).coerceAtLeast(14f)
            canvas.drawText("Choose a Level!", W / 2f, H * 0.087f, titlePaint)
            canvas.drawText("D-pad to select  •  OK to play", W / 2f, H * 0.138f, subtitlePaint)
        }

        // ── Background ────────────────────────────────────────────────────────

        private fun drawBackground(canvas: Canvas, W: Float, H: Float) {
            // Sky
            canvas.drawRect(0f, 0f, W, H * 0.68f, skyPaint)

            // Sun
            val sunX = W * 0.88f; val sunY = H * 0.12f
            canvas.drawCircle(sunX, sunY, 52f, sunGlowP)
            canvas.drawCircle(sunX, sunY, 44f, sunP)

            // Clouds (3 simple clouds)
            drawCloud(canvas, W * 0.18f, H * 0.13f, 1.0f)
            drawCloud(canvas, W * 0.52f, H * 0.09f, 0.75f)
            drawCloud(canvas, W * 0.73f, H * 0.19f, 0.85f)

            // Far hills
            val hillH = H * 0.50f
            val farHillPath = Path().apply {
                moveTo(0f, H)
                lineTo(0f, hillH * 1.05f)
                cubicTo(W * 0.10f, hillH * 0.75f, W * 0.18f, hillH * 0.80f, W * 0.28f, hillH * 0.70f)
                cubicTo(W * 0.38f, hillH * 0.60f, W * 0.45f, hillH * 0.78f, W * 0.55f, hillH * 0.65f)
                cubicTo(W * 0.65f, hillH * 0.52f, W * 0.72f, hillH * 0.72f, W * 0.82f, hillH * 0.62f)
                cubicTo(W * 0.90f, hillH * 0.55f, W * 0.96f, hillH * 0.75f, W, hillH * 0.70f)
                lineTo(W, H)
                close()
            }
            canvas.drawPath(farHillPath, hillFarP)

            // Near hills / ground
            val nearHillPath = Path().apply {
                moveTo(0f, H)
                lineTo(0f, H * 0.70f)
                cubicTo(W * 0.12f, H * 0.62f, W * 0.22f, H * 0.72f, W * 0.35f, H * 0.64f)
                cubicTo(W * 0.48f, H * 0.56f, W * 0.55f, H * 0.70f, W * 0.68f, H * 0.62f)
                cubicTo(W * 0.80f, H * 0.54f, W * 0.90f, H * 0.68f, W, H * 0.62f)
                lineTo(W, H)
                close()
            }
            canvas.drawPath(nearHillPath, hillNearP)

            // Dark ground strip at bottom
            val groundPath = Path().apply {
                moveTo(0f, H)
                lineTo(0f, H * 0.78f)
                cubicTo(W * 0.20f, H * 0.74f, W * 0.50f, H * 0.82f, W * 0.80f, H * 0.75f)
                cubicTo(W * 0.90f, H * 0.73f, W * 0.96f, H * 0.78f, W, H * 0.76f)
                lineTo(W, H)
                close()
            }
            canvas.drawPath(groundPath, hillDarkP)
        }

        private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
            val r = 22f * scale
            canvas.drawCircle(cx,        cy,       r,        cloudP)
            canvas.drawCircle(cx + r,    cy + r * 0.3f, r * 0.80f, cloudP)
            canvas.drawCircle(cx - r,    cy + r * 0.3f, r * 0.70f, cloudP)
            canvas.drawCircle(cx + r * 1.8f, cy + r * 0.5f, r * 0.65f, cloudP)
            canvas.drawCircle(cx - r * 1.6f, cy + r * 0.6f, r * 0.55f, cloudP)
        }

        // ── Winding path between nodes ────────────────────────────────────────

        private fun drawPath(
            canvas: Canvas,
            nodePos: List<Pair<Float, Float>>,
            W: Float, H: Float,
            nodeR: Float
        ) {
            // Build a smooth cubic path through all nodes
            val pts = nodePos.map { (fx, fy) -> Pair(fx * W, fy * H) }
            val pathObj = buildCubicPath(pts)

            canvas.drawPath(pathObj, pathEdgePaint)
            canvas.drawPath(pathObj, pathPaint)

            // Centre dashes along a simpler polyline approximation
            pathDashPaint.strokeWidth = (W * 0.003f).coerceAtLeast(3f)
            for (i in 0 until pts.size - 1) {
                val (x0, y0) = pts[i]
                val (x1, y1) = pts[i + 1]
                val steps = 6
                for (s in 0 until steps) {
                    val t0 = s.toFloat() / steps
                    val t1 = (s + 0.4f) / steps
                    canvas.drawLine(
                        x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0,
                        x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1,
                        pathDashPaint
                    )
                }
            }
        }

        private fun buildCubicPath(pts: List<Pair<Float, Float>>): Path {
            val path = Path()
            if (pts.isEmpty()) return path
            path.moveTo(pts[0].first, pts[0].second)
            for (i in 0 until pts.size - 1) {
                val (x0, y0) = pts[i]
                val (x1, y1) = pts[i + 1]
                val cx0 = x0 + (x1 - x0) * 0.5f
                val cx1 = x0 + (x1 - x0) * 0.5f
                path.cubicTo(cx0, y0, cx1, y1, x1, y1)
            }
            return path
        }

        // ── Individual node ───────────────────────────────────────────────────

        private fun drawNode(
            canvas: Canvas,
            nx: Float, ny: Float,
            nodeR: Float,
            level: Int,
            W: Float, H: Float
        ) {
            val isUnlocked = LevelProgress.isUnlocked(this@WorldMapActivity, level)
            val isSelected = level == selectedLevel
            val isCompleted = level < LevelProgress.getMaxUnlocked(this@WorldMapActivity) ||
                    (level == LevelProgress.TOTAL_LEVELS &&
                            LevelProgress.isUnlocked(this@WorldMapActivity, LevelProgress.TOTAL_LEVELS) &&
                            LevelProgress.getMaxUnlocked(this@WorldMapActivity) > LevelProgress.TOTAL_LEVELS - 1)

            // Pulsing glow halo for selected node
            if (isSelected && isUnlocked) {
                val pulse   = 0.55f + sin(pulseTick.toDouble()).toFloat() * 0.45f
                val haloR   = nodeR * (1.5f + pulse * 0.35f)
                val haloAlpha = (140 + (pulse * 80).toInt()).coerceIn(0, 255)
                nodeGlowP.color = (haloAlpha shl 24) or 0x00FFD54F
                canvas.drawCircle(nx, ny, haloR, nodeGlowP)
            }

            // Node fill circle
            val fillPaint = when {
                !isUnlocked -> nodeLockedP
                isSelected  -> nodeSelP
                else        -> nodeUnlockP
            }
            canvas.drawCircle(nx, ny, nodeR, fillPaint)

            // Inner slightly lighter circle
            val innerColor = when {
                !isUnlocked -> 0xFF757575.toInt()
                isSelected  -> 0xFF1E88E5.toInt()
                else        -> 0xFF1976D2.toInt()
            }
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerColor }
            canvas.drawCircle(nx, ny, nodeR * 0.80f, innerPaint)

            // Outline
            val outP = if (!isUnlocked) nodeLockOutP else {
                nodeSelOutP.also {
                    it.strokeWidth = if (isSelected) nodeR * 0.18f else 4f
                }
            }
            if (isUnlocked) canvas.drawCircle(nx, ny, nodeR, outP)
            else canvas.drawCircle(nx, ny, nodeR, nodeLockOutP)

            if (!isUnlocked) {
                // Lock icon
                drawLockIcon(canvas, nx, ny, nodeR * 0.55f)
            } else {
                // Level number
                numPaint.textSize = nodeR * 0.90f
                canvas.drawText(level.toString(), nx, ny + numPaint.textSize * 0.35f, numPaint)
            }

            // Stars above completed nodes
            if (isCompleted) {
                val starY = ny - nodeR * 1.55f
                for (si in -1..1) {
                    val starX  = nx + si * nodeR * 0.65f
                    val starRr = nodeR * (if (si == 0) 0.32f else 0.22f)
                    drawStar(canvas, starX, starY, starRr)
                }
            }

            // Level label below node
            labelPaint.textSize = (H * 0.036f).coerceAtLeast(12f)
            labelPaint.color    = if (isUnlocked) Color.WHITE else Color.parseColor("#9E9E9E")
            canvas.drawText("Level $level", nx, ny + nodeR * 1.55f, labelPaint)
        }

        private fun drawLockIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
            // Body of lock (rounded rect)
            val bodyRect = RectF(cx - size * 0.55f, cy - size * 0.15f, cx + size * 0.55f, cy + size * 0.70f)
            val bodyFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#757575") }
            canvas.drawRoundRect(bodyRect, size * 0.15f, size * 0.15f, bodyFillP)
            canvas.drawRoundRect(bodyRect, size * 0.15f, size * 0.15f, lockIconPaint)
            // Shackle (arc)
            val shackleRect = RectF(cx - size * 0.40f, cy - size * 0.85f, cx + size * 0.40f, cy + size * 0.10f)
            canvas.drawArc(shackleRect, 180f, 180f, false, lockIconPaint)
            // Keyhole dot
            val khP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9E9E9E") }
            canvas.drawCircle(cx, cy + size * 0.22f, size * 0.14f, khP)
        }

        private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
            val innerR = r * 0.42f
            val path   = Path()
            for (i in 0 until 10) {
                val angle  = (i * 36.0 - 90.0) * Math.PI / 180.0
                val radius = if (i % 2 == 0) r else innerR
                val x = cx + cos(angle).toFloat() * radius
                val y = cy + sin(angle).toFloat() * radius
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, starP)
            canvas.drawPath(path, starOutP)
        }
    }
}
