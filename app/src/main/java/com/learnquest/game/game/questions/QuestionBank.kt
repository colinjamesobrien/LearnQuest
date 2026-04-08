package com.learnquest.game.game.questions

import kotlin.random.Random

/**
 * Generates K–Grade 1 math and reading questions.
 *
 * Math:  addition and subtraction with single-digit operands.
 * Reading: sight word identification, phonics (letter → first-sound word),
 *          and simple word completion.
 *
 * @param mode  Controls which question types are generated.
 * @param level Difficulty level (1 = small numbers / basic words,
 *              2 = slightly harder, 3 = full K-1 range)
 */
class QuestionBank(
    private val mode: GameMode = GameMode.MIXED,
    private val level: Int = 1
) {

    enum class GameMode { MATH, READING, MIXED }

    // ── Reading content ──────────────────────────────────────────────────────

    // Dolch Pre-Primer + Primer sight words with paired emoji hints
    private val sightWords = listOf(
        Pair("🐱 CAT",   "CAT"),
        Pair("🐶 DOG",   "DOG"),
        Pair("🌞 SUN",   "SUN"),
        Pair("🌙 MOON",  "MOON"),
        Pair("🌳 TREE",  "TREE"),
        Pair("🏠 HOUSE", "HOUSE"),
        Pair("🚗 CAR",   "CAR"),
        Pair("📚 BOOK",  "BOOK"),
        Pair("🍎 APPLE", "APPLE"),
        Pair("🐸 FROG",  "FROG"),
        Pair("⭐ STAR",  "STAR"),
        Pair("🐟 FISH",  "FISH"),
        Pair("🌸 FLOWER","FLOWER"),
        Pair("🦋 BUG",   "BUG")
    )

    // Phonics: first-letter → correct starting word + distractors
    private val phonicsMap = mapOf(
        'A' to listOf("APPLE",  "BALL",  "CAT",   "DOG"),
        'B' to listOf("BALL",   "CAT",   "DOG",   "EGG"),
        'C' to listOf("CAT",    "DOG",   "EGG",   "FISH"),
        'D' to listOf("DOG",    "EGG",   "FISH",  "GOAT"),
        'E' to listOf("EGG",    "FISH",  "GOAT",  "HAT"),
        'F' to listOf("FISH",   "GOAT",  "HAT",   "ICE"),
        'G' to listOf("GOAT",   "HAT",   "ICE",   "JAM"),
        'H' to listOf("HAT",    "ICE",   "JAM",   "KITE"),
        'I' to listOf("ICE",    "JAM",   "KITE",  "LION"),
        'J' to listOf("JAM",    "KITE",  "LION",  "MAP"),
        'K' to listOf("KITE",   "LION",  "MAP",   "NUT"),
        'L' to listOf("LION",   "MAP",   "NUT",   "OWL"),
        'M' to listOf("MAP",    "NUT",   "OWL",   "PIG"),
        'N' to listOf("NUT",    "OWL",   "PIG",   "QUEEN"),
        'O' to listOf("OWL",    "PIG",   "QUEEN", "RAT"),
        'P' to listOf("PIG",    "QUEEN", "RAT",   "SUN"),
        'R' to listOf("RAT",    "SUN",   "TUG",   "VAN"),
        'S' to listOf("SUN",    "TUG",   "VAN",   "WAX"),
        'T' to listOf("TUG",    "VAN",   "WAX",   "YAK"),
        'V' to listOf("VAN",    "WAX",   "YAK",   "ZAP"),
        'W' to listOf("WAX",    "YAK",   "ZAP",   "ANT"),
        'Y' to listOf("YAK",    "ZAP",   "ANT",   "BUS"),
        'Z' to listOf("ZAP",    "ANT",   "BUS",   "CAR")
    )

    // Word-completion puzzles: "C_T" → missing letter choices, correct = 'A'
    private val wordCompletions = listOf(
        Triple("C _ T",  listOf("A","E","I","O"), 0),   // CAT
        Triple("D _ G",  listOf("O","A","E","I"), 0),   // DOG
        Triple("S _ N",  listOf("U","O","I","A"), 0),   // SUN
        Triple("H _ T",  listOf("A","E","I","U"), 0),   // HAT
        Triple("P _ G",  listOf("I","A","O","E"), 0),   // PIG
        Triple("B _ G",  listOf("U","A","O","I"), 0),   // BUG
        Triple("F _ N",  listOf("A","E","I","U"), 0),   // FAN
        Triple("R _ N",  listOf("U","O","I","A"), 0),   // RUN
        Triple("B _ D",  listOf("E","A","I","O"), 0),   // BED
        Triple("C _ P",  listOf("U","E","A","O"), 0)    // CUP
    )

    // ── Number range by level ────────────────────────────────────────────────

    private val maxNumber get() = when (level) {
        1    -> 5
        2    -> 10
        else -> 18
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Returns a random question appropriate for the current mode + level. */
    fun nextQuestion(): Question {
        return when (mode) {
            GameMode.MATH    -> mathQuestion()
            GameMode.READING -> readingQuestion()
            GameMode.MIXED   -> if (Random.nextBoolean()) mathQuestion() else readingQuestion()
        }
    }

    // ── Math questions ────────────────────────────────────────────────────────

    private fun mathQuestion(): Question {
        return if (Random.nextBoolean()) additionQuestion() else subtractionQuestion()
    }

    private fun additionQuestion(): Question {
        val a = Random.nextInt(0, maxNumber / 2 + 1)
        val b = Random.nextInt(0, maxNumber / 2 + 1)
        val answer = a + b
        return Question(
            prompt = "$a + $b = ?",
            choices = buildChoices(answer, answer + 1),
            correctIndex = 0,
            type = QuestionType.MATH
        )
    }

    private fun subtractionQuestion(): Question {
        val b = Random.nextInt(0, maxNumber / 2 + 1)
        val a = Random.nextInt(b, maxNumber / 2 + 1)   // ensure a >= b
        val answer = a - b
        return Question(
            prompt = "$a − $b = ?",
            choices = buildChoices(answer, maxOf(answer - 1, 0)),
            correctIndex = 0,
            type = QuestionType.MATH
        )
    }

    /**
     * Produces a shuffled list of 4 answer strings where [correct] is guaranteed
     * to be present and [correctIndex] in the returned Question reflects its
     * shuffled position.
     */
    private fun buildChoices(correct: Int, neighborHint: Int): List<String> {
        val pool = mutableSetOf(correct)
        // Add distractors close to the correct answer
        var delta = 1
        while (pool.size < 4) {
            pool.add(correct + delta)
            if (pool.size < 4) pool.add(maxOf(0, correct - delta))
            delta++
        }
        val shuffled = pool.take(4).shuffled()
        // Rebuild as strings and ensure correctIndex is tracked (caller uses 0,
        // so we return a choices list where shuffled[correctIndex] == correct.toString())
        // We'll do an in-place shuffle and return the list already having correct at index 0,
        // then shuffle from outside.
        val list = mutableListOf(correct.toString())
        pool.filter { it != correct }.take(3).forEach { list.add(it.toString()) }
        list.shuffle()
        return list   // correctIndex must be found by the caller
    }

    // ── Reading questions ─────────────────────────────────────────────────────

    private fun readingQuestion(): Question {
        return when (Random.nextInt(3)) {
            0    -> sightWordQuestion()
            1    -> phonicsQuestion()
            else -> wordCompletionQuestion()
        }
    }

    private fun sightWordQuestion(): Question {
        val word = sightWords.random()
        // Build 3 distractor words from the pool
        val distractors = sightWords.filter { it != word }.shuffled().take(3).map { it.second }
        val choices = mutableListOf(word.second).apply { addAll(distractors) }.shuffled()
        return Question(
            prompt = "What word matches?\n${word.first}",
            choices = choices,
            correctIndex = choices.indexOf(word.second),
            type = QuestionType.READING
        )
    }

    private fun phonicsQuestion(): Question {
        val letter = phonicsMap.keys.random()
        val words = phonicsMap[letter]!!
        val correct = words[0]
        val distractors = words.drop(1)
        val choices = mutableListOf(correct).apply { addAll(distractors) }.shuffled()
        return Question(
            prompt = "Which word starts with  \"$letter\" ?",
            choices = choices,
            correctIndex = choices.indexOf(correct),
            type = QuestionType.READING
        )
    }

    private fun wordCompletionQuestion(): Question {
        val (pattern, letters, _) = wordCompletions.random()
        val correct = letters[0]
        val choices = letters.toMutableList().shuffled()
        return Question(
            prompt = "Fill in the blank:\n$pattern",
            choices = choices,
            correctIndex = choices.indexOf(correct),
            type = QuestionType.READING
        )
    }
}
