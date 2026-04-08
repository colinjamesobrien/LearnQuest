package com.learnquest.game.game

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Dedicated game-loop thread that drives [GameEngine.update] and [GameEngine.draw]
 * at a target of 60 frames per second.
 *
 * Uses a fixed-timestep approach: if a frame takes longer than expected the
 * delta is clamped to 100 ms to avoid large physics jumps after a hitch.
 */
class GameThread(
    private val holder: SurfaceHolder,
    private val engine: GameEngine
) : Thread("GameThread") {

    @Volatile var running = false

    private val targetFps      = 60
    private val targetFrameMs  = 1000L / targetFps
    private val maxDeltaMs     = 100L

    override fun run() {
        var lastTimeMs = System.currentTimeMillis()

        while (running) {
            val now      = System.currentTimeMillis()
            val elapsed  = (now - lastTimeMs).coerceAtMost(maxDeltaMs)
            lastTimeMs   = now
            val delta    = elapsed / 1000f

            engine.update(delta)

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        engine.draw(canvas)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            // Sleep for remaining frame time to hit the target FPS
            val frameTime = System.currentTimeMillis() - now
            val sleepMs   = targetFrameMs - frameTime
            if (sleepMs > 0) {
                try { sleep(sleepMs) } catch (_: InterruptedException) { }
            }
        }
    }
}
