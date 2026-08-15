package com.samadhi.meditationtimer.data.wisdom

data class TeacherGuide(
    val id: String,
    val teacherName: String,
    val title: String,
    val tradition: String,
    val level: String, // "Beginner", "Intermediate", "Advanced"
    val summary: String,
    val keyConcept: String,
    val steps: List<PracticeStep>,
    val coreQuote: String
)

data class PracticeStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String
)

data class WisdomQuote(
    val id: Int,
    val quote: String,
    val author: String,
    val sourceOrTradition: String
)

object MeditationGuides {

    val teachers: List<TeacherGuide> = listOf(
        TeacherGuide(
            id = "buddho",
            teacherName = "Thai Forest Masters",
            title = "The 'Buddho' Breath Mantra",
            tradition = "Thai Forest Tradition (Luang Pu Mun Lineage)",
            level = "Beginner",
            summary = "The classic anchor technique of the Thai Forest Tradition. Syncing the sacred name of the Awakened One with the in-and-out breath to unify awareness and calm discursive thinking.",
            keyConcept = "Bud (Awake) on the in-breath, Dho (Knowing) on the out-breath. Merge awareness with the syllable until silent stillness remains.",
            coreQuote = "Recite 'Buddho' continuously with each breath. Let the mind stick to 'Buddho' until 'Buddho' and the mind become one single knowing quality.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Establish Posture & Intent",
                    instruction = "Sit upright in a balanced, relaxed posture. Soften the shoulders and close your eyes gently. Take three deep, calming breaths to release physical tension."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Sync with Inhalation",
                    instruction = "As the breath naturally enters the body, mentally recite the syllable 'Bud' (pronounced 'Bood'). Feel the cool touch of the breath at the nostril tip or upper lip."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Sync with Exhalation",
                    instruction = "As the breath naturally leaves the body, mentally recite the syllable 'Dho' (pronounced 'Dhoh'). Feel the warm release and let go of all physical strain."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Refining to Subtle Stillness",
                    instruction = "Do not force the breath to be long or short; simply know it as it is. As the breath becomes softer and finer, 'Buddho' becomes a delicate, steady vibration of pure awareness."
                ),
                PracticeStep(
                    stepNumber = 5,
                    title = "Dropping into Pure Knowing",
                    instruction = "When the mind becomes completely calm, the mental word 'Buddho' may fade away on its own, leaving only luminous, steady awareness (Samadhi). Rest there peacefully."
                )
            )
        ),
        TeacherGuide(
            id = "thich_nhat_hanh",
            teacherName = "Thich Nhat Hanh",
            title = "Mindful Breathing & Smiling to the Moment",
            tradition = "Plum Village / Engaged Buddhism",
            level = "Beginner",
            summary = "Gentle, compassionate mindfulness practices using poetic gathas (recitations) to return home to the body, release tension, and embrace the wonder of life in the present moment.",
            keyConcept = "There is no way to peace; peace is the way. Every breath is a celebration of life.",
            coreQuote = "Breathing in, I calm body and mind. Breathing out, I smile. Dwelling in the present moment, I know this is the only moment.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "In, Out / Deep, Slow",
                    instruction = "Breathing in: 'In'. Breathing out: 'Out'. Next breath: 'Deep... Slow'. Let the breath find its natural gentle depth without strain."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Calm, Ease",
                    instruction = "Breathing in: 'I calm my whole body'. Breathing out: 'I feel at ease'. Release any tension held in your face, jaw, eyes, and shoulders."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Smile, Release",
                    instruction = "Breathing in: 'I smile'. Breathing out: 'I release'. Allow a gentle, half-smile to illuminate your lips. Smiling relaxes hundreds of facial nerves."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Present Moment, Wonderful Moment",
                    instruction = "Breathing in: 'Present moment'. Breathing out: 'Wonderful moment'. Know that the conditions for happiness are already present right here, right now."
                )
            )
        ),
        TeacherGuide(
            id = "ajahn_lee",
            teacherName = "Ajahn Lee Dhammadharo",
            title = "Refining the Breath Energy (7 Steps)",
            tradition = "Thai Forest Tradition (Wat Asokaram)",
            level = "Intermediate",
            summary = "The definitive practical manual from 'Keeping the Breath in Mind'. Transforming the in-and-out breath into whole-body healing energy that pacifies the nervous system and deepens concentration.",
            keyConcept = "Spread the breath energy to circulate through the whole body, dissolving blockages and soothing all tension.",
            coreQuote = "When the breath is comfortable, adjust and expand it through every blood vessel and nerve until the entire body is filled with light and ease.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Deep Clearing Breaths",
                    instruction = "Start with 3 to 7 long, deep in-and-out breaths. Think 'Bud' with the in-breath, 'Dho' with the out-breath to sweep out stale air and settle the body."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Observe Breath Quality",
                    instruction = "Notice whether the breath feels short, long, tight, heavy, warm, or cool. Is it pleasant or irritating? Find the most comfortable rhythm."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Choose a Breath Anchor Base",
                    instruction = "Rest your attention at one primary base: the tip of the nose, the middle of the chest (solar plexus), the throat, or the base of the skull."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Maintain Steady Contact",
                    instruction = "Keep your awareness tethered to this single spot continuously. Do not let the mind wander into past or future concerns."
                ),
                PracticeStep(
                    stepNumber = 5,
                    title = "Spread the Breath Energy",
                    instruction = "Let the pleasant, comfortable breath sensation spread from your anchor down the spine, across the chest, and through both arms and legs to the tips of the toes and fingers."
                ),
                PracticeStep(
                    stepNumber = 6,
                    title = "Unify Breath & Body",
                    instruction = "Let all separate breath channels merge into one unified, harmonious energy field surrounding the whole body like a cocoon of stillness."
                ),
                PracticeStep(
                    stepNumber = 7,
                    title = "Merge Mind and Energy",
                    instruction = "Unite awareness with this calm, refined energy. Discursive thoughts quiet down completely as one-pointed absorption (Jhana) arises."
                )
            )
        ),
        TeacherGuide(
            id = "bhante_gunaratana",
            teacherName = "Bhante Henepola Gunaratana",
            title = "Bare Attention & The 5 Hindrances",
            tradition = "Theravada / Vipassana (Bhavana Society)",
            level = "Intermediate",
            summary = "Clear, practical, and direct instructions on developing non-judgmental bare attention, overcoming the five hindrances, and noting sensations without reacting.",
            keyConcept = "Mindfulness is non-judgmental observation. It sees things as they truly are without adding concepts, opinions, or resistance.",
            coreQuote = "Mindfulness is like a gentle light shining upon reality. It does not judge, it does not criticize; it simply observes with patience and clarity.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Bare Attention at the Nostril Tip",
                    instruction = "Place attention precisely at the spot where the incoming air touches the rim of the nostrils or upper lip. Notice the subtle tingling, temperature change, and rhythm."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Cultivating Non-Judgmental Watching",
                    instruction = "When the mind wanders, do not become frustrated or angry. Notice where it went without judgment, then gently and kindly escort your attention back to the breath."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Recognizing the 5 Hindrances",
                    instruction = "Observe desire, aversion, sluggishness, restlessness, or doubt when they arise. Label them clearly ('desire, desire' or 'restlessness, restlessness') and watch them dissolve on their own."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Continuous, Patient Investigation",
                    instruction = "Observe the micro-fluctuations in sensation. Sensation arises, stays briefly, and vanishes. See the truth of impermanence in every single breath."
                )
            )
        ),
        TeacherGuide(
            id = "mingyur_rinpoche",
            teacherName = "Yongey Mingyur Rinpoche",
            title = "Open Awareness: Meditation Anywhere",
            tradition = "Tibetan Mahamudra & Dzogchen (Tergar)",
            level = "Intermediate",
            summary = "Accessible, joy-filled guidance on resting in effortless awareness. Making friends with emotions, thoughts, and sounds rather than fighting them.",
            keyConcept = "Short moments, many times. You don't need to stop thoughts to meditate—simply be aware of them.",
            coreQuote = "Awareness is like space: thoughts and emotions are like clouds. Clouds pass, but space is never harmed or stained.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Simply Settle & Rest",
                    instruction = "Sit comfortably. Let your body relax completely. For a few seconds, do not meditate at all. Just rest the mind in its natural state."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Object-Supported Awareness",
                    instruction = "Rest your gentle attention on the breath, on a sound in the room, or on the feeling of your hands. Don't grip it tightly; hold it like a butterfly in an open palm."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Welcoming Distractions as Friends",
                    instruction = "When thoughts, memories, or restless feelings arise, don't push them away. Say 'Hello thought!' and simply know that thinking is happening. Awareness is the observer, not the thought."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Open Awareness (Objectless Meditation)",
                    instruction = "Drop the focus on the breath and sound. Rest in pure, wide-open, boundless awareness like looking up into an open blue sky."
                )
            )
        ),
        TeacherGuide(
            id = "ajahn_brahm",
            teacherName = "Ajahn Brahm",
            title = "Dropping Past & Future: Silent Awareness",
            tradition = "Theravada / Forest Tradition (Bodhinyana Monastery)",
            level = "Advanced",
            summary = "The art of letting go of the heavy baggage of the past and the anxiety of the future, abandoning all wanting and the 'doer', and resting in effortless silent awareness of the beautiful breath.",
            keyConcept = "Meditation is not about achieving or getting something; it is the art of letting go, abandoning the 'doer', and allowing stillness to unfold naturally.",
            coreQuote = "Drop the baggage of the past; don't pick up the burden of the future. When you abandon all wanting, the mind falls into profound, silent peace.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Dropping Past & Future (Present Moment)",
                    instruction = "Imagine taking off a heavy backpack filled with memories, regrets, and to-do lists. Leave behind the past completely—it is just a memory. Abandon the future—it is just an imagination. Give yourself total permission to only be here right now."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Letting Go of the 'Doer' (Silent Awareness)",
                    instruction = "Abandon all inner commentary and evaluation. You don't need to describe your experience or judge it as good or bad. Abandon the wanting for things to be different. Silence the inner voice and rest as a peaceful, silent observer."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Silent Awareness of the Breath",
                    instruction = "Allow the breath to come into awareness effortlessly. You are not controlling, counting, or breathing deeply; you are just watching the body breathe itself with kindness and total acceptance."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "Full Sustained Attention on the Whole Breath",
                    instruction = "Stay with the breath continuously from the very beginning of the in-breath, through the middle, to the end, and through the pause to the out-breath without a gap."
                ),
                PracticeStep(
                    stepNumber = 5,
                    title = "The Beautiful Breath (Pīti & Sukha)",
                    instruction = "As mental struggle ceases, the breath begins to feel extraordinarily peaceful, soothing, and beautiful. Delight in this blissful stillness and let it deepen without grasping."
                )
            )
        ),
        TeacherGuide(
            id = "ajahn_chah",
            teacherName = "Ajahn Chah",
            title = "The Middle Way & The Art of Letting Go",
            tradition = "Thai Forest Tradition (Wat Nong Pah Pong)",
            level = "Advanced",
            summary = "Direct, unpretentious wisdom on observing the mind without attachment, neither grasping after pleasant sensations nor pushing away unpleasant thoughts.",
            keyConcept = "If you let go a little, you will have a little peace. If you let go a lot, you will have a lot of peace. If you let go completely, you will know complete peace.",
            coreQuote = "Do not try to become anything. Do not make yourself into anything. Do not be a meditator. Just see whatever arises and let it go.",
            steps = listOf(
                PracticeStep(
                    stepNumber = 1,
                    title = "Natural Breath, No Straining",
                    instruction = "Sit comfortably. Do not breathe too strongly or too weakly. Breathe naturally. Just know the in-breath and know the out-breath."
                ),
                PracticeStep(
                    stepNumber = 2,
                    title = "Watching Like a Still Forest Pool",
                    instruction = "Think of your mind like a secluded forest pool. All kinds of wonderful and strange animals (thoughts, feelings, sensations) will come to drink. Just sit quietly and watch them come and go without chasing them."
                ),
                PracticeStep(
                    stepNumber = 3,
                    title = "Applying 'Mai Nae' (It's Uncertain)",
                    instruction = "Whenever a thought, fear, or profound insight arises, recognize: 'It's uncertain, impermanent (Aniccha).' Do not cling to good states or fight bad ones."
                ),
                PracticeStep(
                    stepNumber = 4,
                    title = "The Peace of Non-Clinging",
                    instruction = "When you stop wanting states to last or wanting distractions to disappear, the mind naturally settles into its true, unshakeable home."
                )
            )
        )
    )

    val wisdomQuotes: List<WisdomQuote> = listOf(
        WisdomQuote(
            id = 1,
            quote = "If you let go a little, you will have a little peace. If you let go a lot, you will have a lot of peace. If you let go completely, you will know complete peace.",
            author = "Ajahn Chah",
            sourceOrTradition = "A Still Forest Pool"
        ),
        WisdomQuote(
            id = 2,
            quote = "Drop the baggage of the past; don't pick up the burden of the future. When you abandon all wanting, the mind falls into profound, silent peace.",
            author = "Ajahn Brahm",
            sourceOrTradition = "Mindfulness, Bliss, and Beyond"
        ),
        WisdomQuote(
            id = 3,
            quote = "Breathing in, I calm body and mind. Breathing out, I smile. Dwelling in the present moment, I know this is the only moment.",
            author = "Thich Nhat Hanh",
            sourceOrTradition = "The Miracle of Mindfulness"
        ),
        WisdomQuote(
            id = 4,
            quote = "When the breath is comfortable, spread it through every nerve and cell until the whole body feels bathed in gentle, soothing light.",
            author = "Ajahn Lee Dhammadharo",
            sourceOrTradition = "Keeping the Breath in Mind"
        ),
        WisdomQuote(
            id = 5,
            quote = "Awareness is like space. Thoughts and emotions are like clouds passing through. The sky is never harmed by storms.",
            author = "Yongey Mingyur Rinpoche",
            sourceOrTradition = "The Joy of Living"
        ),
        WisdomQuote(
            id = 6,
            quote = "Mindfulness does not judge, reject, or criticize. It simply observes with total patience and clarity.",
            author = "Bhante Gunaratana",
            sourceOrTradition = "Mindfulness in Plain English"
        ),
        WisdomQuote(
            id = 7,
            quote = "Let 'Buddho' become your only refuge. Recite it with the in-breath and out-breath until the mind becomes steady like a deep mountain lake.",
            author = "Luang Pu Mun Bhuridatto",
            sourceOrTradition = "Heart of the Forest Tradition"
        ),
        WisdomQuote(
            id = 8,
            quote = "Do not try to become anything. Do not make yourself into anything. Just see whatever arises and let it pass away.",
            author = "Ajahn Chah",
            sourceOrTradition = "Food for the Heart"
        ),
        WisdomQuote(
            id = 9,
            quote = "Meditation is not about getting something new; it is about letting go of what is false until truth stands alone.",
            author = "Ajahn Sumedho",
            sourceOrTradition = "The Mind and the Way"
        ),
        WisdomQuote(
            id = 10,
            quote = "Smile, breathe, and go slowly. There is no destination other than this very breath.",
            author = "Thich Nhat Hanh",
            sourceOrTradition = "Peace Is Every Step"
        ),
        WisdomQuote(
            id = 11,
            quote = "Give yourself permission to be completely useless for the next hour. No problems to solve, nowhere to go, nothing to accomplish.",
            author = "Ajahn Brahm",
            sourceOrTradition = "The Art of Disappearing"
        ),
        WisdomQuote(
            id = 12,
            quote = "Short moments, many times. That is the secret to making meditation a natural part of your life.",
            author = "Yongey Mingyur Rinpoche",
            sourceOrTradition = "In Love with the World"
        ),
        WisdomQuote(
            id = 13,
            quote = "Do not fight with your wandering mind. Simply notice that you wandered, smile warmly, and return to the touch of the breath.",
            author = "Bhante Gunaratana",
            sourceOrTradition = "Eight Mindful Steps to Happiness"
        ),
        WisdomQuote(
            id = 14,
            quote = "The breath is your anchor in every storm. Stay anchored, and no wave can overturn you.",
            author = "Ajahn Lee Dhammadharo",
            sourceOrTradition = "Craft of the Heart"
        ),
        WisdomQuote(
            id = 15,
            quote = "Peace is not the absence of sound or thought; it is the absence of resistance to whatever is.",
            author = "Ajahn Amaro",
            sourceOrTradition = "Small Boat, Great Mountain"
        ),
        WisdomQuote(
            id = 16,
            quote = "Whenever the mind wanders, remember: the breath is always waiting for you, without frustration and without reproach.",
            author = "Bhante Gunaratana",
            sourceOrTradition = "Mindfulness in Plain English"
        ),
        WisdomQuote(
            id = 17,
            quote = "Silence is not empty; it is full of answers.",
            author = "Thai Forest Tradition",
            sourceOrTradition = "Monastery Proverb"
        ),
        WisdomQuote(
            id = 18,
            quote = "Allow the meditation to happen. You don't make peace; peace appears when the making stops.",
            author = "Ajahn Brahm",
            sourceOrTradition = "Opening the Door of Your Heart"
        )
    )
}
