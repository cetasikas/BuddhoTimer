package com.samadhi.meditationtimer.domain.model

/**
 * Curated collection of gentle, shame-free mindfulness prompts.
 * Inspired by TimeAware's non-judgmental awareness philosophy.
 * These rotate on each app open to encourage intentional phone use.
 */
object MindfulPrompts {

    val prompts = listOf(
        "Why are you reaching for your phone right now?",
        "Is this the moment you intended to be here?",
        "Take one slow breath before you scroll.",
        "What would you do if the app you want wasn't there?",
        "Notice how you feel before you open anything.",
        "Are you bored, or are you truly curious?",
        "The present moment doesn't need a filter.",
        "What was the last thing you did offline?",
        "Before you scroll — what do you actually need?",
        "You're here. That's already something.",
        "The phone is a tool. What are you building?",
        "Stillness is available to you right now.",
        "Intention is the difference between use and habit.",
        "What if you put it down in ten minutes?",
        "The world is still turning without your check-in.",
        "Notice: is your thumb already moving?",
        "Peace isn't on the other side of the screen.",
        "What did you want to do today that still hasn't happened?",
        "Pick it up with purpose, or put it down with peace.",
        "Even a minute of stillness is a minute well spent."
    )

    /**
     * Returns a prompt that changes daily but stays consistent within the same day.
     */
    fun getDailyPrompt(): String {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val index = ((year * 365) + dayOfYear) % prompts.size
        return prompts[index]
    }

    /**
     * Returns a prompt that changes each time the app is foregrounded.
     */
    fun getSessionPrompt(sessionCount: Int): String {
        return prompts[sessionCount % prompts.size]
    }
}
