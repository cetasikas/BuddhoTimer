# Buddho Meditation Timer

A standalone Android meditation timer written in **Kotlin** and **Jetpack Compose**.

Designed specifically for deep mindfulness and meditation practice sessions with background persistence, overrun tracking ("Samadhi mode"), automatic Do Not Disturb (DND) integration, Room database storage, and Markdown journal reflections.

---

## Key Features

1. **Background-Immune Foreground Timer Service:**
   - Runs as an ongoing Android Foreground Service (`foregroundServiceType="specialUse"`).
   - Acquires a CPU `WakeLock` to ensure precision timing when the display turns off.
   - Ongoing system notification with live countdown, overrun indicator, and Quick Action buttons (**Pause**, **Resume**, **Finish**).
   - Requests `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for marathon sessions.

2. **Precision Timing & Continuous "Samadhi" Overrun Mode:**
   - Configurable target session time (5m, 10m, 15m, 20m, 30m, 45m, 60m, custom) and open-ended stopwatch mode.
   - Optional warm-up preparation countdown (0–15s).
   - **Continuous Overrun Feature:** When target duration hits `00:00`, it plays a synthesized Tibetan Singing Bowl chime (with gentle haptics) and seamlessly transitions into a positive count-up timer (`+mm:ss`) without interrupting your deep state.

3. **Automatic Do Not Disturb (DND) Management:**
   - Seamlessly integrates with `NotificationManager.setInterruptionFilter()`.
   - Captures original system filter state, engages `INTERRUPTION_FILTER_PRIORITY` upon session start, and faithfully restores previous state upon finish or cancellation.
   - One-tap banner navigation to Android's DND Policy Access Settings when permissions are needed.

4. **Practice Logging & Markdown Journaling:**
   - **Room Database:** Stores timestamps, target duration, actual duration, overrun time, focus/depth mood ratings (1–5), and Markdown reflection notes.
   - **Rich Markdown Input Toolbar:** Fast insertion for Headings (`#`), Bold (`**`), Italic (`*`), Quotes (`>`), Lists (`-`), and Code (`` ` ``).
   - **Native Compose Markdown Renderer:** Cleanly renders Markdown text in past session logs.
   - **Practice Journey Stats:** Dynamic tracking of Total Sessions, Total Practice Time, Current Day Streak, and Longest Streak.

5. **Data Portability & Backup (SAF):**
   - **JSON / CSV Export:** Uses Android Storage Access Framework (SAF) to save backups locally or to cloud storage (e.g. Google Drive).
   - **Import & Restore:** Parses JSON backups and merges non-duplicate records.
   - **Android Auto Backup:** Configured with `@xml/data_extraction_rules` and `@xml/backup_rules`.

---

## Architectural Layers

```
meditation-timer-app/app/src/main/java/com/samadhi/meditationtimer/
├── data/
│   ├── backup/                # SAF JSON/CSV export & restore manager
│   ├── local/                 # Room database, DAO, & SessionEntity
│   └── repository/            # Repository implementation & streak analytics
├── domain/
│   ├── model/                 # MeditationSession, SessionStats, TimerState
│   └── repository/            # SessionRepository interface
├── service/
│   ├── MeditationTimerService.kt  # Foreground timer service
│   ├── NotificationHelper.kt      # Ongoing notification builder
│   ├── WakeLockHelper.kt          # Partial WakeLock lifecycle
│   ├── SoundHelper.kt             # Real-time acoustic Tibetan chime & haptics
│   └── DndManager.kt              # Do Not Disturb state machine
└── ui/
    ├── components/            # CircularTimerProgress, MarkdownRenderer, MoodSelector
    ├── navigation/            # Navigation routes
    ├── screens/
    │   ├── timer/             # Practice setup & duration selector
    │   ├── session/           # Active countdown & Samadhi overrun screen
    │   ├── summary/           # Post-practice reflection & journal editor
    │   ├── history/           # Streak stats & rendered markdown session log
    │   └── settings/          # Permissions, Sound test, and SAF Backup/Restore
    ├── theme/                 # Dark obsidian zen color palette & typography
    └── MainAppScaffold.kt     # App scaffold with bottom navigation bar
```

---

## Build & Run

1. Open this repository in **Android Studio**.
2. Ensure JDK 17+ is configured.
3. Sync Gradle and run on a physical device or emulator running **Android 8.0+ (API 26+)**.

Release signing keys are intentionally not included. Use your own local signing configuration for release builds.

---

## License

The source code is released under the Apache License 2.0. You may use,
modify, and redistribute it, including in other open source projects, as long
as you preserve the license notice and provide appropriate attribution to
Buddho Timer and its contributors.

Bundled third-party assets, including meditation audio and ambient sounds, are
not relicensed by this repository. They remain under their original licenses
and attribution requirements. Check the source comments near the audio loading
code before redistributing those files.
