package com.samadhi.meditationtimer.data.wisdom

data class TopicCardData(
    val id: String,
    val topicNumber: Int,
    val iconName: String, // "start", "breath", "buddho", "walking", "restlessness", "metta"
    val title: String,
    val subtitle: String,
    val quote: String,
    val practicalSteps: List<String>
)

object MeditationGuides {

    val topicCards: List<TopicCardData> = listOf(
        TopicCardData(
            id = "start_here",
            topicNumber = 1,
            iconName = "start",
            title = "Start Here: Ajahn Brahm's Method",
            subtitle = "4 stages from letting go of the world to joyful stillness",
            quote = "“Meditation is the way of letting go. You don't try to achieve or accumulate anything; you abandon the burden of doing to discover the radiant stillness within.” — Ajahn Brahm",
            practicalSteps = listOf(
                "Present-Moment Awareness: Abandon memories of the past and plans for the future. Give yourself full permission to have nowhere to go and nothing to fix.",
                "Silent Present Moment: Quiet the inner commentator. Stop evaluating or critiquing your sit. Experience this moment in wordless peace.",
                "Silent Awareness of the Breath: Feel the body naturally breathing itself. Do not control or manipulate the airflow — simply enjoy the rhythm.",
                "Sustained Full Attention: Follow each breath from its gentle inception, through the middle, to the quiet turning pause at the end. Rest in that stillness."
            )
        ),
        TopicCardData(
            id = "breath_awareness",
            topicNumber = 2,
            iconName = "breath",
            title = "Mindfulness of Breathing (Ānāpānasati)",
            subtitle = "Natural breath awareness without control or force",
            quote = "“Breathing in, I calm body and mind. Breathing out, I smile. Dwelling in the present moment, I know this is the only moment.” — Thich Nhat Hanh",
            practicalSteps = listOf(
                "Locate the Anchor: Rest your attention at the nostrils or upper lip where the physical touch of air is felt most clearly.",
                "Witness, Don't Manage: Let the body breathe entirely on its own. You are the peaceful witness, not the controller of the breath.",
                "Notice Long & Short: When the breath is long, know it is long. When it is short, know it is short. Stay with whatever is naturally present.",
                "Settling into Subtle Stillness: As awareness rests patiently, the breath naturally softens, becoming delicate, calm, and soothing."
            )
        ),
        TopicCardData(
            id = "buddho_mantra",
            topicNumber = 3,
            iconName = "buddho",
            title = "The 'Buddho' Syllable Anchor",
            subtitle = "Settle a busy mind with this traditional Thai Forest anchor",
            quote = "“Let 'Buddho' become your refuge with every breath until the mind becomes steady and clear like a deep mountain lake.” — Luang Pu Mun Bhuridatto",
            practicalSteps = listOf(
                "Inhale with 'Bud': As the breath flows inward naturally, silently and gently whisper 'Bud' (The one who is Awake).",
                "Exhale with 'Dho': As the breath releases softly, silently whisper 'Dho' (The one who Knows & Lets Go).",
                "Keep It Effortless: Let the mental sound synchronize softly with your natural breath speed without forcing the breath.",
                "Dropping the Syllable: When concentration deepens and the mind is unified, the word will naturally fall away into pure, wordless presence."
            )
        ),
        TopicCardData(
            id = "varying_count",
            topicNumber = 4,
            iconName = "varying",
            title = "The Varying Breath & Counting Method (Gaṇanā)",
            subtitle = "Authentic Buddhist methods to overcome autopilot without strain",
            quote = "“Meditation is letting go. Never force or strain the breath. Counting is just a temporary tool to anchor a restless mind until it settles into silence.” — Ajahn Brahm",
            practicalSteps = listOf(
                "Ajahn Brahm's Core Rule — No Forcing: The breath must always remain natural and comfortable. Never stretch or hold the breath beyond what feels pleasant and effortless.",
                "The 3-4-5 Progressive Cadence: Inhale & exhale for 3s (Grounding), expand to 4s (Centering), then 5s (Heart Coherence) before gently returning down.",
                "The Traditional Breath-Cycle Pyramid (Visuddhimagga): Alternatively, count whole breaths in ascending sets: count 1…5, then 1…6, up to 1…10, then step back down (10→5).",
                "Dropping the Numbers: Once the mind feels unified, grounded, and tranquil, let go of all counting and rest in the silent, beautiful breath."
            )
        ),
        TopicCardData(
            id = "walking_meditation",
            topicNumber = 5,
            iconName = "walking",
            title = "Walking Meditation (Caṅkama)",
            subtitle = "Mindfulness in physical motion — peace in every step",
            quote = "“Walk as if you are kissing the Earth with your feet. Peace is present in every conscious step.” — Thich Nhat Hanh",
            practicalSteps = listOf(
                "Establish a Clear Path: Choose a straight path of 15–25 paces. Stand in stillness for a few breaths to ground physical awareness.",
                "Unhurried Natural Pace: Hold hands comfortably in front or behind. Lower your gaze softly a few paces ahead without staring.",
                "Track the Sensations: Feel the lifting, moving, and placing of each foot, or gently synchronize your footsteps with the in-and-out breath.",
                "Mindful Pauses & Turns: At the end of the path, pause completely, take a mindful breath, turn with awareness, and begin again."
            )
        ),
        TopicCardData(
            id = "working_with_hindrances",
            topicNumber = 6,
            iconName = "restlessness",
            title = "Working with Hindrances & Restlessness",
            subtitle = "Transforming obstacles with curiosity and kindness",
            quote = "“The sky is never stained by the clouds passing through. Awareness itself is never harmed by thoughts.” — Yongey Mingyur Rinpoche",
            practicalSteps = listOf(
                "The 3-Second Awakening: When you notice the mind has wandered, celebrate! That moment of noticing is pure mindfulness. Never criticize yourself.",
                "Soften and Smile: Release physical tension in your brow and jaw, generate a gentle inner smile, and softly return attention to the breath.",
                "For Sloth & Sleepiness: Straighten the spine, lift your chin slightly, take 3 full revitalizing breaths, or meditate with eyes half-open.",
                "For Restlessness & Worry: Acknowledge the thinking energy without fighting: 'Thinking, thinking.' Re-anchor in the physical sensations of the body."
            )
        ),
        TopicCardData(
            id = "metta_kindness",
            topicNumber = 7,
            iconName = "metta",
            title = "Mettā (Loving-Kindness Meditation)",
            subtitle = "Cultivate boundless warmth and goodwill from the inside out",
            quote = "“Radiate boundless love towards the entire world — above, below, and all around — unhindered, without hostility.” — The Buddha (Karaṇīya Mettā Sutta)",
            practicalSteps = listOf(
                "Begin with Yourself: Rest a hand gently over your heart. Silently repeat: 'May I be peaceful. May I be safe. May I live with ease and joy.'",
                "Extend to a Loved One: Picture someone who easily brings warmth to your heart (a benefactor, friend, or pet) and send them the same genuine wish.",
                "Widen the Circle: Gradually extend this feeling of goodwill to neutral people, difficult people, and finally all beings across the world.",
                "Rest in the Radiant Heart: Let go of words and dwell in the warm, open space of unconditional benevolence."
            )
        )
    )

    val wisdomQuotes: List<WisdomQuote> = listOf(
        WisdomQuote(
            id = 1,
            quote = "If you let go a little, you will have a little peace. If you let go a lot, you will have a lot of peace. If you let go completely, you will know complete peace.",
            author = "Ajahn Chah",
            sourceOrTradition = "Thai Forest Tradition"
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
            quote = "Don't try to get anywhere in meditation. Just be where you are, knowing what is happening right now with kindness and patience.",
            author = "Sayadaw U Tejaniya",
            sourceOrTradition = "When Awareness Becomes Natural"
        ),
        WisdomQuote(
            id = 5,
            quote = "In the beginner's mind there are many possibilities, but in the expert's mind there are few.",
            author = "Shunryu Suzuki",
            sourceOrTradition = "Zen Mind, Beginner's Mind"
        ),
        WisdomQuote(
            id = 6,
            quote = "Let 'Buddho' become your only refuge. Recite it with the in-breath and out-breath until the mind becomes steady like a deep mountain lake.",
            author = "Luang Pu Mun Bhuridatto",
            sourceOrTradition = "Thai Forest Tradition"
        ),
        WisdomQuote(
            id = 7,
            quote = "Mindfulness is the observant mind. It doesn't judge, it doesn't choose sides, it simply witnesses what is true.",
            author = "Bhante Henepola Gunaratana",
            sourceOrTradition = "Mindfulness in Plain English"
        ),
        WisdomQuote(
            id = 8,
            quote = "To study the Buddha Way is to study the self. To study the self is to forget the self. To forget the self is to be actualized by myriad things.",
            author = "Eihei Dogen",
            sourceOrTradition = "Genjokoan (Soto Zen)"
        ),
        WisdomQuote(
            id = 9,
            quote = "Give yourself permission to be completely useless for the next hour. No problems to solve, nowhere to go, nothing to accomplish.",
            author = "Ajahn Brahm",
            sourceOrTradition = "The Art of Disappearing"
        ),
        WisdomQuote(
            id = 10,
            quote = "Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment.",
            author = "The Buddha",
            sourceOrTradition = "Bhaddekaratta Sutta"
        ),
        WisdomQuote(
            id = 11,
            quote = "Awareness is like space. Thoughts and emotions are like clouds passing through. The sky is never harmed by the storm.",
            author = "Yongey Mingyur Rinpoche",
            sourceOrTradition = "The Joy of Living"
        ),
        WisdomQuote(
            id = 12,
            quote = "Meditation is not about stopping thoughts. It is about understanding that thoughts are not who you are.",
            author = "Ajahn Sumedho",
            sourceOrTradition = "The Sound of Silence"
        ),
        WisdomQuote(
            id = 13,
            quote = "Only when you are quiet enough can you hear the silence that is the true ground of being.",
            author = "Ajahn Amaro",
            sourceOrTradition = "Amaravati Buddhist Monastery"
        ),
        WisdomQuote(
            id = 14,
            quote = "Peace comes from within. Do not seek it without.",
            author = "The Buddha",
            sourceOrTradition = "Dhammapada"
        ),
        WisdomQuote(
            id = 15,
            quote = "The thought manifests as the word; The word manifests as the deed; The deed develops into habit; And habit hardens into character.",
            author = "The Buddha",
            sourceOrTradition = "Dhammapada"
        ),
        WisdomQuote(
            id = 16,
            quote = "Smile, breathe and go slowly. There is no need to hurry. Everything unfolds in its own true time.",
            author = "Thich Nhat Hanh",
            sourceOrTradition = "Peace Is Every Step"
        ),
        WisdomQuote(
            id = 17,
            quote = "When you sit, just sit. When you walk, just walk. Above all, don't wobble.",
            author = "Yunmen Wenyan",
            sourceOrTradition = "Zen Teaching"
        ),
        WisdomQuote(
            id = 18,
            quote = "You cannot stop the waves, but you can learn to surf.",
            author = "Jon Kabat-Zinn",
            sourceOrTradition = "Wherever You Go, There You Are"
        ),
        WisdomQuote(
            id = 19,
            quote = "Love is the ground of our practice. When we cultivate loving-kindness, fear and clinging dissolve naturally.",
            author = "Sharon Salzberg",
            sourceOrTradition = "Lovingkindness: The Revolutionary Art of Happiness"
        ),
        WisdomQuote(
            id = 20,
            quote = "If your mind is quiet, whatever you do becomes meditation. If your mind is scattered, even sitting in silence is worldly chatter.",
            author = "Buddhadasa Bhikkhu",
            sourceOrTradition = "Mindfulness with Breathing"
        ),
        WisdomQuote(
            id = 21,
            quote = "Whatever arises in the mind is merely weather. The watcher remains untouched.",
            author = "Jack Kornfield",
            sourceOrTradition = "A Path with Heart"
        ),
        WisdomQuote(
            id = 22,
            quote = "Calmness is the cradle of power. When stillness arrives, clarity and wisdom follow without effort.",
            author = "Mahasi Sayadaw",
            sourceOrTradition = "Manual of Insight"
        ),
        WisdomQuote(
            id = 23,
            quote = "Nothing whatever is to be clung to as 'I' or 'mine'. This single understanding is the heart of liberation.",
            author = "The Buddha",
            sourceOrTradition = "Majjhima Nikaya 37"
        ),
        WisdomQuote(
            id = 24,
            quote = "Silence is not the absence of sound, but the stillness of the heart.",
            author = "Ajahn Pasanno",
            sourceOrTradition = "Abhayagiri Monastery"
        ),
        WisdomQuote(
            id = 25,
            quote = "When we touch the stillness within, we touch the source of boundless healing and compassion.",
            author = "His Holiness the 14th Dalai Lama",
            sourceOrTradition = "The Art of Happiness"
        ),
        WisdomQuote(
            id = 26,
            quote = "Let go of all concepts. Rest the mind in its natural clarity, like water when the mud settles.",
            author = "Dilgo Khyentse Rinpoche",
            sourceOrTradition = "The Heart of Compassion"
        ),
        WisdomQuote(
            id = 27,
            quote = "Just observe with curiosity: 'What is this?' Do not push away unpleasant sensations, nor cling to pleasant ones.",
            author = "Joseph Goldstein",
            sourceOrTradition = "Mindfulness: A Practical Guide"
        ),
        WisdomQuote(
            id = 28,
            quote = "The mind that does not cling to anything is the mind of the Buddha.",
            author = "Bodhidharma",
            sourceOrTradition = "Bloodstream Sermon"
        ),
        WisdomQuote(
            id = 29,
            quote = "Everything that arises passes away. Seeing this with clear comprehension brings peace beyond words.",
            author = "S.N. Goenka",
            sourceOrTradition = "Vipassana Research Institute"
        ),
        WisdomQuote(
            id = 30,
            quote = "Drink your tea slowly and reverently, as if it is the axis on which the world earth revolves.",
            author = "Thich Nhat Hanh",
            sourceOrTradition = "The Miracle of Mindfulness"
        )
    )
}

data class WisdomQuote(
    val id: Int,
    val quote: String,
    val author: String,
    val sourceOrTradition: String
)


