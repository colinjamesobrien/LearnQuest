package com.learnquest.game

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.learnquest.game.game.questions.QuestionBank

/**
 * TV-friendly main menu.
 *
 * Three mode buttons (Mixed / Math / Reading) are navigable with the D-pad.
 * Each button launches [GameActivity] with the selected mode.
 *
 * Focus highlight: the focused button turns gold (#FFD54F) to make it
 * clearly visible on a TV from across the room.
 */
class MainActivity : Activity() {

    private lateinit var btnMixed:   Button
    private lateinit var btnMath:    Button
    private lateinit var btnReading: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMixed   = findViewById(R.id.btn_mixed)
        btnMath    = findViewById(R.id.btn_math)
        btnReading = findViewById(R.id.btn_reading)

        setupButton(btnMixed,   QuestionBank.GameMode.MIXED)
        setupButton(btnMath,    QuestionBank.GameMode.MATH)
        setupButton(btnReading, QuestionBank.GameMode.READING)

        // Apply focus-change highlighting to all buttons
        listOf(btnMixed, btnMath, btnReading).forEach { btn ->
            btn.setBackgroundResource(R.drawable.btn_menu_selector)
            btn.setOnFocusChangeListener { v, hasFocus ->
                (v as Button).setTextColor(
                    if (hasFocus) Color.BLACK else Color.WHITE
                )
            }
        }

        // Give initial focus to the first button
        btnMixed.requestFocus()
    }

    private fun setupButton(button: Button, mode: QuestionBank.GameMode) {
        button.setOnClickListener { launchCharacterSelect(mode) }
    }

    private fun launchCharacterSelect(mode: QuestionBank.GameMode) {
        val intent = Intent(this, CharacterSelectActivity::class.java).apply {
            putExtra(CharacterSelectActivity.EXTRA_MODE,  mode.name)
            putExtra(CharacterSelectActivity.EXTRA_LEVEL, 1)
        }
        startActivity(intent)
    }

    // Forward D-pad enter/center key to focused button
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            currentFocus?.performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
