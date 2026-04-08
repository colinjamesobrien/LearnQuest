package com.learnquest.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.learnquest.game.game.entities.CharacterRenderer
import com.learnquest.game.game.entities.CharacterType
import com.learnquest.game.game.questions.QuestionBank

/**
 * Character selection screen shown between the main menu and [GameActivity].
 *
 * Displays two rows of [CharacterCardView]s — animals on top, dinosaurs below.
 * D-pad left/right navigates within a row; up/down jumps between rows.
 * Pressing OK/Center/Enter confirms the selection and starts the game.
 *
 * Receives [EXTRA_MODE] and [EXTRA_LEVEL] from [MainActivity] and forwards them,
 * along with the chosen [CharacterType], to [GameActivity].
 */
class CharacterSelectActivity : Activity() {

    companion object {
        const val EXTRA_MODE      = GameActivity.EXTRA_MODE
        const val EXTRA_LEVEL     = GameActivity.EXTRA_LEVEL
    }

    private val cardsByType = LinkedHashMap<CharacterType, CharacterCardView>()
    private val allCards    = mutableListOf<CharacterCardView>()
    private val animalCards = mutableListOf<CharacterCardView>()
    private val dinoCards   = mutableListOf<CharacterCardView>()

    private lateinit var rowAnimals:   LinearLayout
    private lateinit var rowDinosaurs: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)

        rowAnimals   = findViewById(R.id.row_animals)
        rowDinosaurs = findViewById(R.id.row_dinosaurs)

        buildRow(CharacterType.ANIMALS,   rowAnimals,   animalCards)
        buildRow(CharacterType.DINOSAURS, rowDinosaurs, dinoCards)

        // Wire cross-row focus navigation (down from animals → dinos, up from dinos → animals)
        wireCrossRowFocus(animalCards, dinoCards)

        // Give initial focus to the first card
        allCards.firstOrNull()?.requestFocus()
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private fun buildRow(
        types:  List<CharacterType>,
        parent: LinearLayout,
        bucket: MutableList<CharacterCardView>
    ) {
        val cardSizeDp = 148
        val marginDp   = 14
        val density    = resources.displayMetrics.density

        types.forEach { type ->
            val card = CharacterCardView(this, type)
            val params = LinearLayout.LayoutParams(
                (cardSizeDp * density).toInt(),
                (cardSizeDp * density).toInt()
            ).apply {
                marginEnd = (marginDp * density).toInt()
            }
            card.layoutParams = params
            card.isFocusable             = true
            card.isFocusableInTouchMode  = true

            card.setOnFocusChangeListener { v, hasFocus ->
                v.scaleX = if (hasFocus) 1.10f else 1.00f
                v.scaleY = if (hasFocus) 1.10f else 1.00f
                (v as CharacterCardView).setHighlighted(hasFocus)
            }

            card.setOnClickListener { selectCharacter(type) }

            parent.addView(card)
            cardsByType[type] = card
            bucket.add(card)
            allCards.add(card)
        }

        // Wire left/right within the row
        for (i in bucket.indices) {
            if (i > 0)               bucket[i].nextFocusLeftId  = bucket[i - 1].id.takeIf { it != View.NO_ID } ?: generateId(bucket[i - 1])
            if (i < bucket.size - 1) bucket[i].nextFocusRightId = bucket[i + 1].id.takeIf { it != View.NO_ID } ?: generateId(bucket[i + 1])
        }
    }

    private fun wireCrossRowFocus(top: List<CharacterCardView>, bottom: List<CharacterCardView>) {
        top.forEachIndexed { i, card ->
            val targetIndex = i.coerceIn(0, bottom.size - 1)
            card.nextFocusDownId = generateId(bottom[targetIndex])
        }
        bottom.forEachIndexed { i, card ->
            val targetIndex = i.coerceIn(0, top.size - 1)
            card.nextFocusUpId = generateId(top[targetIndex])
        }
    }

    private fun generateId(view: View): Int {
        if (view.id == View.NO_ID) view.id = View.generateViewId()
        return view.id
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    private fun selectCharacter(type: CharacterType) {
        val modeName = intent.getStringExtra(EXTRA_MODE)
            ?: QuestionBank.GameMode.MIXED.name

        // Go to world map so the player can pick a level
        val mapIntent = Intent(this, WorldMapActivity::class.java).apply {
            putExtra(WorldMapActivity.EXTRA_MODE,      modeName)
            putExtra(WorldMapActivity.EXTRA_CHARACTER, type.name)
        }
        startActivity(mapIntent)
    }

    // ── Key events ────────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            currentFocus?.performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

// ── CharacterCardView ─────────────────────────────────────────────────────────

/**
 * A focusable card that renders a [CharacterType] using [CharacterRenderer]
 * plus a name label below the drawing.
 */
private class CharacterCardView(context: Context, private val type: CharacterType) : View(context) {

    private val bgPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2D3B8C.toInt() }
    private val bgPaintFocus  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD54F.toInt() }
    private val borderPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize  = 30f
        typeface  = Typeface.DEFAULT_BOLD
    }

    private var highlighted = false

    fun setHighlighted(on: Boolean) {
        highlighted = on
        labelPaint.color = if (on) Color.BLACK else Color.WHITE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Background card
        canvas.drawRoundRect(4f, 4f, w - 4f, h - 4f, 18f, 18f,
            if (highlighted) bgPaintFocus else bgPaintNormal)
        canvas.drawRoundRect(4f, 4f, w - 4f, h - 4f, 18f, 18f, borderPaint)

        // Character drawing occupies the upper ~75% of the card
        val charH   = h * 0.68f
        val charW   = charH * 0.75f
        val charSX  = (w - charW) / 2f
        val charSY  = h * 0.06f

        CharacterRenderer.draw(
            canvas      = canvas,
            sx          = charSX,
            sy          = charSY,
            width       = charW,
            height      = charH,
            type        = type,
            facingRight = true,
            legPhase    = 0f,
            isMoving    = false
        )

        // Name label at the bottom
        canvas.drawText(type.displayName, w / 2f, h - 14f, labelPaint)
    }
}
