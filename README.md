<div align="center">

<img src="app/src/main/res/drawable/ic_buddho_logo.png" alt="Buddho Timer Logo" width="104" height="104">

# Buddho Timer

### Open-source, offline-first meditation timer for Android

[![Release](https://img.shields.io/github/v/release/cetasikas/BuddhoTimer?style=flat-square&color=D4AF37)](https://github.com/cetasikas/BuddhoTimer/releases/latest)
[![License](https://img.shields.io/badge/License-Apache_2.0-1E293B?style=flat-square)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-Offline_First-0E1420?style=flat-square)](#privacy)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#build-from-source)

Buddho Timer is a quiet meditation app for people who want a reliable timer, local journaling, gentle teachings, and no account, ads, analytics, or cloud dependency.

[Download APK](https://github.com/cetasikas/BuddhoTimer/releases/latest) · [View Landing Page](https://cetasikas.github.io/BuddhoTimer/) · [Build From Source](#build-from-source)

</div>

---

## What It Does

Buddho Timer is designed around practice rather than productivity. It keeps the phone calm during meditation, supports open-ended sitting, saves reflections locally, and gives you enough history to notice patterns without turning meditation into a scoreboard.

Core features:

- Configurable meditation timer with preparation countdown and open-ended mode.
- Continuous overrun mode when a timed session naturally continues past the target.
- Foreground service, wake lock, and persistent notification for reliable background timing.
- Optional automatic Do Not Disturb during practice, with restoration afterward.
- Acoustic chimes, ambient soundscapes, and bundled guided meditations.
- Box breathing and Buddho breath practice support.
- Markdown journal entries with mood/reflection tracking.
- Journal search, filters, JSON export, and Markdown export.
- Practice overview, heatmap, insights, and gentle intention tracking.
- Home screen widget for today's practice rhythm.
- Teachings section with simple meditation guidance.
- Local-first storage with Room database and Android backup support.

## Privacy

Buddho Timer is offline-first by design.

- No user accounts.
- No ads.
- No analytics SDKs.
- No telemetry.
- No social login.
- No required server.
- Your journal entries stay on your device unless you export or back them up yourself.

## Download

The latest public APK is available on the GitHub Releases page:

`https://github.com/cetasikas/BuddhoTimer/releases/latest`

Android may warn that the APK was downloaded outside an app store. Only install releases from this repository if you trust this source.

## Build From Source

Requirements:

- Android Studio
- JDK 17+
- Android SDK with API 36 installed

Build debug APK:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Build release APK:

```powershell
.\gradlew.bat :app:assembleRelease --console=plain
```

Release signing keys are intentionally not included. If you want to distribute your own release builds, use your own signing key and keep it outside version control.

## Project Structure

```text
app/src/main/java/com/samadhi/meditationtimer/
├── data/        Room database, backup/export, repositories, teachings data
├── domain/      Session models, analytics, repository contracts
├── service/     Timer service, notifications, audio, DND, wake locks
├── ui/          Compose screens, navigation, shared components, theme
└── widget/      Home screen widget provider
```

## License

The source code is released under the Apache License 2.0. You may use, modify, and redistribute it as long as you preserve the license notice and provide appropriate attribution to Buddho Timer and its contributors.

Bundled third-party assets, including meditation audio and ambient sounds, are not relicensed by this repository. They remain under their original licenses and attribution requirements. Check the source comments near the audio loading code before redistributing those files.

## Contributing

Issues and pull requests are welcome. Changes should preserve the app's basic philosophy: quiet, local-first, non-gamified, and respectful of meditation practice.
