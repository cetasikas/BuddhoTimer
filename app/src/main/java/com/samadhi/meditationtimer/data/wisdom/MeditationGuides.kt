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
            subtitle = "The 4 foundational stages from letting go of the world to joyful stillness",
            quote = "“Meditation is the way of letting go. In meditation you let go of the complex world outside in order to reach the powerful peace within.” — Ajahn Brahm",
            practicalSteps = listOf(
                "Stage 1: Present Moment Awareness — Put down the heavy luggage of past and future. Give yourself permission to be completely useless for this hour—nowhere to go, no problems to solve.",
                "Stage 2: Silent Present Moment Awareness — Turn down the inner narrator. Stop commentary and opinions. Experience the vivid beauty of the 'here and now' in total silence.",
                "Stage 3: Silent Present Moment Awareness of the Breath — Gently invite the breath into silent awareness. Do not control or force the breath; allow the body to breathe itself as you enjoy its natural rhythm.",
                "Stage 4: Full Sustained Attention on the Breath — Delight in following the whole breath from beginning, to middle, to the quiet turning point at the end, resting in deep stillness."
            )
        ),
        TopicCardData(
            id = "breath_awareness",
            topicNumber = 2,
            iconName = "breath",
            title = "Mindfulness of Breathing (Ānāpānasati)",
            subtitle = "Knowing the natural flow without controlling or forcing",
            quote = "“Breathing in, I calm body and mind. Breathing out, I smile.” — Thich Nhat Hanh",
            practicalSteps = listOf(
                "Allow the body to breathe itself. Do not force the breath to be long, short, deep, or shallow.",
                "Feel the gentle touch of air at the nostrils or upper lip as it naturally enters and leaves.",
                "Stay with the whole breath from beginning, to middle, to the quiet turning point.",
                "As continuity develops, the breath becomes delightfully soft, peaceful, and luminous."
            )
        ),
        TopicCardData(
            id = "buddho_mantra",
            topicNumber = 3,
            iconName = "buddho",
            title = "The 'Buddho' Syllable Anchor",
            subtitle = "Quiet inner thoughts with the traditional forest meditation anchor",
            quote = "“Let 'Buddho' become your refuge until the mind becomes steady like a deep mountain lake.” — Luang Pu Mun",
            practicalSteps = listOf(
                "As the breath naturally enters, mentally whisper: 'Bud' (Awake).",
                "As the breath naturally leaves, mentally whisper: 'Dho' (Knowing & Letting Go).",
                "Sync the mental sound smoothly with the natural movement of the breath.",
                "When concentration settles deeply, the syllable will naturally fall silent—rest in the wordless peace."
            )
        ),
        TopicCardData(
            id = "walking_meditation",
            topicNumber = 4,
            iconName = "walking",
            title = "Walking Meditation",
            subtitle = "Mindfulness in motion: peace in every single step",
            quote = "“Walk as if you are kissing the Earth with your feet.” — Thich Nhat Hanh",
            practicalSteps = listOf(
                "Find a quiet path of 15–25 paces. Stand upright and feel your feet making contact with the earth.",
                "Walk slowly and naturally. Coordinate your breath: 'In' with one step, 'Out' with the next.",
                "Feel the lifting, moving, and placing of each foot with relaxed awareness.",
                "When you reach the end of the path, pause, take a breath, turn gently, and begin again."
            )
        ),
        TopicCardData(
            id = "working_with_hindrances",
            topicNumber = 5,
            iconName = "restlessness",
            title = "Calming Restlessness & Sleepiness",
            subtitle = "Friendly, compassionate ways to handle wanderings and fatigue",
            quote = "“The sky is never stained by the clouds passing through.” — Yongey Mingyur Rinpoche",
            practicalSteps = listOf(
                "When the mind wanders, never get angry. The moment you notice wandering is pure mindfulness.",
                "Give yourself a gentle inner smile and warmly invite attention back home to the breath.",
                "If feeling sleepy: sit up straight, take 3 deep mindful breaths, or open your eyes slightly.",
                "Treat thoughts like weather—let them come, let them be, let them go without engaging."
            )
        ),
        TopicCardData(
            id = "metta_kindness",
            topicNumber = 6,
            iconName = "metta",
            title = "Mettā (Loving-Kindness Meditation)",
            subtitle = "Cultivating warmth, goodwill, and gentle self-compassion",
            quote = "“If you let go a little, you will have a little peace. If you let go completely, you will know complete peace.” — Ajahn Chah",
            practicalSteps = listOf(
                "Place a hand over your heart or rest gently in your center. Wish yourself genuine peace and safety.",
                "Silently repeat: 'May I be peaceful. May I be safe. May I be free from suffering.'",
                "Extend this warm feeling outward to loved ones, neutral people, and all living beings everywhere.",
                "End your meditation resting in the gentle glow of goodwill and shared humanity."
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
            sourceOrTradition = "Zen Wisdom"
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


