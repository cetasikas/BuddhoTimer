package com.samadhi.meditationtimer.domain.model

enum class BreathPhase(val displayName: String, val instruction: String) {
    INHALE("Inhale", "Inhale smoothly\nthrough the nose"),
    HOLD_FULL("Hold", "Pause & rest gently\nwith full lungs"),
    EXHALE("Exhale", "Exhale slowly\nand let go"),
    HOLD_EMPTY("Hold", "Rest peacefully\nin stillness")
}

data class BreathingPattern(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val inhaleSec: Int,
    val holdFullSec: Int,
    val exhaleSec: Int,
    val holdEmptySec: Int
) {
    val totalCycleSec: Int
        get() = inhaleSec + holdFullSec + exhaleSec + holdEmptySec

    companion object {
        val BOX_BREATHING = BreathingPattern(
            id = "box",
            name = "Box Breathing (4-4-4-4)",
            subtitle = "Acute Stress & Panic Reset",
            description = "Clinical and tactical technique to down-regulate the sympathetic nervous system and rapidly alleviate acute panic, anxiety, and mental overwhelm.",
            inhaleSec = 4,
            holdFullSec = 4,
            exhaleSec = 4,
            holdEmptySec = 4
        )

        val RELAX_4_7_8 = BreathingPattern(
            id = "478",
            name = "4-7-8 Deep Calm",
            subtitle = "Vagal Brake for Sleep & Relaxation",
            description = "Extended exhale stimulates the vagus nerve, slowing heart rate and inducing deep bodily relaxation.",
            inhaleSec = 4,
            holdFullSec = 7,
            exhaleSec = 8,
            holdEmptySec = 0
        )

        val RESONANT_BREATH = BreathingPattern(
            id = "resonant",
            name = "Coherence Breathing (5-5)",
            subtitle = "Heart Rate Variability (HRV)",
            description = "Balances emotional regulation and optimizes cardiovascular rhythm with smooth, rhythmic breath waves.",
            inhaleSec = 5,
            holdFullSec = 0,
            exhaleSec = 5,
            holdEmptySec = 0
        )

        val TRANQUIL_3_3_3_3 = BreathingPattern(
            id = "gentle_box",
            name = "Gentle Box (3-3-3-3)",
            subtitle = "Easy Settling for Beginners",
            description = "A gentler rhythm for tight chests and beginners to ease into breath control without strain.",
            inhaleSec = 3,
            holdFullSec = 3,
            exhaleSec = 3,
            holdEmptySec = 3
        )

        val VARYING_COUNT = BreathingPattern(
            id = "varying_count",
            name = "Varying-Count Breath (3-4-5)",
            subtitle = "Anti-Autopilot & Gentle Settling",
            description = "Scales breath cadence across 4 comfortable stages (3s Grounding -> 4s Centering -> 5s Coherence -> Return) to eliminate mental drift without lung strain.",
            inhaleSec = 3,
            holdFullSec = 0,
            exhaleSec = 3,
            holdEmptySec = 0
        )

        val allPatterns = listOf(BOX_BREATHING, VARYING_COUNT, RELAX_4_7_8, RESONANT_BREATH, TRANQUIL_3_3_3_3)
    }
}
