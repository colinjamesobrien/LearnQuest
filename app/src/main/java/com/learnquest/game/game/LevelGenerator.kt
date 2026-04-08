package com.learnquest.game.game

import com.learnquest.game.game.entities.Enemy
import com.learnquest.game.game.questions.QuestionBank
import kotlin.random.Random

/**
 * Generates the list of [Enemy] objects for a level.
 *
 * Higher levels spawn enemies more frequently (tighter spacing) and with a
 * slightly longer level length, making each world harder to clear.
 *
 * Level  Spacing  Jitter  Level length
 * ─────  ───────  ──────  ────────────
 *   1    1 200 px  400 px   9 000 px
 *   2    1 050 px  380 px   9 500 px
 *   3      900 px  360 px  10 000 px
 *   4      750 px  340 px  10 500 px
 *   5      620 px  320 px  11 000 px
 */
object LevelGenerator {

    private const val FIRST_ENEMY_X = 1200f

    fun generate(
        questionBank:  QuestionBank,
        groundTop:    Float,
        groundBottom: Float,
        levelLength:  Float,
        level:        Int = 1
    ): List<Enemy> {
        val baseSpacing = (1200f - (level - 1) * 145f).coerceAtLeast(620f)
        val jitter      = (400f - (level - 1) * 20f).coerceAtLeast(280f)
        val depthRange  = groundBottom - groundTop

        val enemies = mutableListOf<Enemy>()
        var x = FIRST_ENEMY_X

        while (x < levelLength - 500f) {
            val y        = groundTop + Random.nextFloat() * depthRange
            val question = questionBank.nextQuestion()
            enemies.add(Enemy(worldX = x, worldY = y, question = question))
            x += baseSpacing + Random.nextFloat() * jitter
        }
        return enemies
    }

    /** Effective level length grows with the level number. */
    fun levelLength(level: Int): Float = 9000f + (level - 1) * 500f
}
