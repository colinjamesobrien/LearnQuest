package com.learnquest.game.game.questions

/**
 * Represents a single multiple-choice question shown at a gate.
 *
 * @param prompt      Text shown above the answer choices (e.g. "3 + 4 = ?")
 * @param choices     Exactly 4 answer strings shown as buttons
 * @param correctIndex  Index into [choices] that is the correct answer (0–3)
 * @param type        Whether this is a math or reading question
 */
data class Question(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val type: QuestionType
)

enum class QuestionType { MATH, READING }
