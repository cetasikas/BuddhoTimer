package com.samadhi.meditationtimer.data.wisdom

data class MeditationTip(
    val id: String,
    val phase: String, // "1. Laying the Groundwork", "2. The Natural Breath", "3. Deepening Stillness"
    val phaseNumber: Int,
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val coreWisdom: String,
    val detailedGuidance: List<String>
)

data class WisdomQuote(
    val id: Int,
    val quote: String,
    val author: String,
    val sourceOrTradition: String
)

object MeditationGuides {

    val meditationPhases = listOf(
        "1. Laying Groundwork",
        "2. Natural Breath",
        "3. Deepening Stillness",
        "All Steps"
    )

    val meditationTips: List<MeditationTip> = listOf(
        // PHASE 1: LAYING THE GROUNDWORK (BEFORE WATCHING THE BREATH)
        MeditationTip(
            id = "drop_past",
            phase = "1. Laying Groundwork",
            phaseNumber = 1,
            stepNumber = 1,
            title = "Dropping the Heavy Backpack of the Past",
            subtitle = "You have no history, no baggage, and no past right now",
            coreWisdom = "Imagine putting down a heavy backpack filled with memories, regrets, conversations, and yesterday's problems. Leave them completely outside the room.",
            detailedGuidance = listOf(
                "The past is just a memory—it does not exist in the physical reality of this room.",
                "Give yourself full permission: 'For the next hour, I have no history, no identity to uphold, and no problems to solve.'",
                "If a thought of what happened today or yesterday arises, gently recognize: 'That is the past. It is over.' Let it float away like smoke."
            )
        ),
        MeditationTip(
            id = "abandon_future",
            phase = "1. Laying Groundwork",
            phaseNumber = 1,
            stepNumber = 2,
            title = "Abandoning the Future & Goal-Seeking",
            subtitle = "No clock-watching, no striving, nowhere else to be",
            coreWisdom = "The future is only an imagination. Drop all anticipation, expectations of deep states, and the urge to check the clock.",
            detailedGuidance = listOf(
                "Meditation is not about achieving or producing a result; peace appears only when the making and achieving stop.",
                "Tell your mind: 'I have nowhere to go, no tasks to accomplish, and no future to worry about right now.'",
                "Rest content in the simple reality of being alive in this exact moment."
            )
        ),
        MeditationTip(
            id = "silence_doer",
            phase = "1. Laying Groundwork",
            phaseNumber = 1,
            stepNumber = 3,
            title = "Letting Go of the 'Doer' & Inner Commentary",
            subtitle = "Shift from managing your experience to silent, friendly listening",
            coreWisdom = "Stop narrating and evaluating your meditation. You do not need to judge whether your sit is 'good' or 'bad.' Silence the inner commentator.",
            detailedGuidance = listOf(
                "Notice the voice in the head that likes to criticize, judge, and describe everything you feel.",
                "Rather than being the 'director' of your mind, become a peaceful, silent observer sitting quietly by a calm forest pool.",
                "Relax the jaw, soften the muscles behind the eyes, and allow the body to rest in effortless stillness."
            )
        ),

        // PHASE 2: MEETING THE NATURAL BREATH
        MeditationTip(
            id = "natural_breath",
            phase = "2. Natural Breath",
            phaseNumber = 2,
            stepNumber = 4,
            title = "Knowing the Natural Breath (No Forcing)",
            subtitle = "Allow the body to breathe itself without interference",
            coreWisdom = "Do not force the breath to be long, short, deep, or shallow. Simply know the breath exactly as it naturally is.",
            detailedGuidance = listOf(
                "You are not controlling the breathing—you are merely the witness. If it is shallow, know it as shallow; if deep, know it as deep.",
                "Place gentle attention at the rim of the nostrils or upper lip where you feel the cool incoming touch and warm outgoing touch.",
                "Hold your awareness lightly, like holding a fragile bird in an open palm without squeezing."
            )
        ),
        MeditationTip(
            id = "buddho_anchor",
            phase = "2. Natural Breath",
            phaseNumber = 2,
            stepNumber = 5,
            title = "The 'Buddho' Syllable Anchor",
            subtitle = "Sync 'Bud' with in-breath and 'Dho' with out-breath",
            coreWisdom = "Quiet discursive thinking by gently resting the mind on the calming syllable: 'Bud' on inhalation, 'Dho' on exhalation.",
            detailedGuidance = listOf(
                "As the breath naturally enters the body, softly think: 'Bud' (Awake).",
                "As the breath naturally leaves the body, softly think: 'Dho' (Knowing & Letting Go).",
                "Let the mental sound weave seamlessly with the physical sensation until mind and breath unify."
            )
        ),
        MeditationTip(
            id = "full_breath",
            phase = "2. Natural Breath",
            phaseNumber = 2,
            stepNumber = 6,
            title = "Sustained Awareness of the Whole Breath",
            subtitle = "Knowing the beginning, the middle, and the end without gaps",
            coreWisdom = "Stay with the breath through its entire journey: the start of inhalation, the peak, the turn-around, and the complete exhalation.",
            detailedGuidance = listOf(
                "Observe the subtle pause between the in-breath and the out-breath in complete stillness.",
                "Do not jump ahead to the next breath. Be completely present with the breath happening right now.",
                "As continuity develops, the breath naturally softens and becomes silky, fine, and peaceful."
            )
        ),

        // PHASE 3: DEEPENING STILLNESS & WORKING WITH THE MIND
        MeditationTip(
            id = "befriend_thoughts",
            phase = "3. Deepening Stillness",
            phaseNumber = 3,
            stepNumber = 7,
            title = "Working with Distractions (No Fighting)",
            subtitle = "When the mind wanders, smile kindly and return home",
            coreWisdom = "Never get frustrated or angry when you discover the mind has wandered. The moment you realize you wandered is a moment of pure mindfulness.",
            detailedGuidance = listOf(
                "Fighting thoughts only creates agitation and turbulence in the mind.",
                "Recognize wandering without judgment, give yourself a gentle internal smile, and warmly escort attention back to the breath.",
                "Thoughts are just weather passing through the open sky of your awareness. The sky is never stained by the clouds."
            )
        ),
        MeditationTip(
            id = "beautiful_breath",
            phase = "3. Deepening Stillness",
            phaseNumber = 3,
            stepNumber = 8,
            title = "The Beautiful Breath (Pīti & Sukha)",
            subtitle = "When wanting stops, the breath becomes delightfully blissful",
            coreWisdom = "As mental resistance dissolves, the breath begins to feel extraordinarily pleasant, luminous, and soothing.",
            detailedGuidance = listOf(
                "Allow yourself to enjoy the peaceful, delicate sensation of breathing.",
                "Bathe the entire nervous system in this subtle ease and contentment.",
                "Do not grasp after the pleasant feeling—simply let it nourish your concentration naturally."
            )
        ),
        MeditationTip(
            id = "samadhi_stillness",
            phase = "3. Deepening Stillness",
            phaseNumber = 3,
            stepNumber = 9,
            title = "Resting in Silent Awareness (Samadhi)",
            subtitle = "When the syllable drops away and only pure knowing remains",
            coreWisdom = "As concentration deepens, the mental word 'Buddho' naturally falls silent on its own. Only radiant, unshakeable peace remains.",
            detailedGuidance = listOf(
                "Do not struggle to bring the word back if it fades—rest fully in the wordless, luminous stillness.",
                "The mind is unified, undisturbed by external sounds or body sensations.",
                "Stay rested in this deep absorption for as long as needed. The timer will gently keep going."
            )
        )
    )

    val wisdomQuotes: List<WisdomQuote> = listOf(
        WisdomQuote(
            id = 1,
            quote = "Drop the baggage of the past; don't pick up the burden of the future. When you abandon all wanting, the mind falls into profound, silent peace.",
            author = "Ajahn Brahm",
            sourceOrTradition = "Mindfulness, Bliss, and Beyond"
        ),
        WisdomQuote(
            id = 2,
            quote = "If you let go a little, you will have a little peace. If you let go a lot, you will have a lot of peace. If you let go completely, you will know complete peace.",
            author = "Ajahn Chah",
            sourceOrTradition = "A Still Forest Pool"
        ),
        WisdomQuote(
            id = 3,
            quote = "Give yourself permission to be completely useless for the next hour. No problems to solve, nowhere to go, nothing to accomplish.",
            author = "Ajahn Brahm",
            sourceOrTradition = "The Art of Disappearing"
        ),
        WisdomQuote(
            id = 4,
            quote = "Breathing in, I calm body and mind. Breathing out, I smile. Dwelling in the present moment, I know this is the only moment.",
            author = "Thich Nhat Hanh",
            sourceOrTradition = "The Miracle of Mindfulness"
        ),
        WisdomQuote(
            id = 5,
            quote = "Awareness is like space. Thoughts and emotions are like clouds passing through. The sky is never harmed by storms.",
            author = "Yongey Mingyur Rinpoche",
            sourceOrTradition = "The Joy of Living"
        ),
        WisdomQuote(
            id = 6,
            quote = "Let 'Buddho' become your only refuge. Recite it with the in-breath and out-breath until the mind becomes steady like a deep mountain lake.",
            author = "Luang Pu Mun Bhuridatto",
            sourceOrTradition = "Heart of the Forest Tradition"
        )
    )
}
