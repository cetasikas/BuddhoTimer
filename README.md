<div align="center">

<img src="app/src/main/res/drawable/ic_buddho_logo.png" alt="Buddho Timer Logo" width="104" height="104">

# Buddho Timer

### Open-source, offline-first meditation timer for Android

[![Release](https://img.shields.io/github/v/release/cetasikas/BuddhoTimer?style=flat-square&color=D4AF37)](https://github.com/cetasikas/BuddhoTimer/releases/latest)
[![License](https://img.shields.io/badge/License-Apache_2.0-1E293B?style=flat-square)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-Offline_First-0E1420?style=flat-square)](#privacy)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#build-from-source)
[![Get it on Obtainium](https://img.shields.io/badge/Get_it_on-Obtainium-7C3AED?style=flat-square)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fcetasikas%2FBuddhoTimer)

Buddho Timer is a quiet meditation app for people who want a reliable timer, local journaling, gentle teachings, and no account, ads, analytics, or cloud dependency.

[Download APK](https://github.com/cetasikas/BuddhoTimer/releases/latest) · [View Landing Page](https://cetasikas.github.io/BuddhoTimer/) · [Build From Source](#build-from-source)

</div>

---

## Screenshots

<!--
Real captures go in docs/screenshots/ (see docs/screenshots/README.md).
Once added, uncomment the block below.

<p align="center">
  <img src="docs/screenshots/timer.png" alt="Meditation timer" width="200">
  <img src="docs/screenshots/session.png" alt="Active session" width="200">
  <img src="docs/screenshots/journal.png" alt="Journal" width="200">
  <img src="docs/screenshots/teachings.png" alt="Teachings" width="200">
</p>
-->

_Screenshots coming soon._

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

## Install

### Obtainium (recommended, auto-updates)

Buddho Timer ships signed APKs on GitHub Releases, so [Obtainium](https://github.com/ImranR98/Obtainium) can track this repository and notify you when a new version is published.

[![Get it on Obtainium](https://img.shields.io/badge/Get_it_on-Obtainium-7C3AED?style=flat-square)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fcetasikas%2FBuddhoTimer)

Already have Obtainium? Add this source URL directly:

`https://github.com/cetasikas/BuddhoTimer`

### Direct APK

Prefer to install manually? Download the latest signed APK from GitHub Releases:

`https://github.com/cetasikas/BuddhoTimer/releases/latest`

Android may warn that the APK was installed outside an app store. Only install releases from this repository if you trust this source.

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
