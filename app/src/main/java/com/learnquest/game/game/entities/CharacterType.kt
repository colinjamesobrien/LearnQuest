package com.learnquest.game.game.entities

/**
 * Playable character types available for selection.
 *
 * [widthScale] / [heightScale] are multiplied against the base size
 * (64 × 80 px) in [Player] so each character occupies a visually
 * appropriate footprint on-screen.
 *
 * Animals are generally smaller than dinosaurs. Within each group,
 * sizes reflect real-world relative proportions — e.g. the T-Rex and
 * Stegosaurus are noticeably larger than the Pterodactyl.
 */
enum class CharacterType(
    val displayName: String,
    val widthScale:  Float,
    val heightScale: Float
) {
    // ── Animals ───────────────────────────────────────────────────────────────
    CAT         ("Cat",          0.78f, 0.82f),   // small & compact
    DOG         ("Dog",          0.88f, 0.90f),   // medium build
    RABBIT      ("Rabbit",       0.80f, 0.92f),   // small but tall ears
    FOX         ("Fox",          0.86f, 0.90f),   // slim medium

    // ── Dinosaurs ─────────────────────────────────────────────────────────────
    T_REX       ("T-Rex",        1.22f, 1.32f),   // large & imposing
    TRICERATOPS ("Triceratops",  1.32f, 1.18f),   // wide & stocky
    STEGOSAURUS ("Stegosaurus",  1.38f, 1.14f),   // longest body
    PTERODACTYL ("Pterodactyl",  1.12f, 1.06f);   // large wing-frame

    companion object {
        val ANIMALS   = listOf(CAT, DOG, RABBIT, FOX)
        val DINOSAURS = listOf(T_REX, TRICERATOPS, STEGOSAURUS, PTERODACTYL)
    }
}
