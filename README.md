# Noter

[![Latest release](https://img.shields.io/github/v/release/jehan593/noter)](https://github.com/jehan593/noter/releases/latest)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://github.com/jehan593/noter)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue)](app/build.gradle.kts)
[![Permissions](https://img.shields.io/badge/permissions-none-informational)](#privacy)

A simple, fully offline note + task app for Android. Nord colors, a monospace font, and two
home-screen widgets.

> **FYI:** this project is fully vibe-coded.

## Features

- **Notes** — one free-form text box that saves as you type, plus Copy and Clear buttons.
- **Tasks** — add tasks, check them off, drag to reorder, rename, or delete.
- **Widgets** — a Note widget and a Task widget on your home screen. Tap either to open the app
  on the matching tab.

## Download

Grab the latest APK from [Releases](https://github.com/jehan593/noter/releases/latest) — or the
permanent link `https://github.com/jehan593/noter/releases/latest/download/app-release.apk`, which
works well with [Obtainium](https://github.com/ImranR98/Obtainium). Requires Android 8.0
(API 26) or newer.

## Building

```sh
./gradlew assembleDebug   # → app/build/outputs/apk/debug/app-debug.apk
```

You'll need the Android SDK (`local.properties` with `sdk.dir` pointing at it) and either Android
Studio or JDK 17 from a terminal.

## Privacy

The app requests no permissions and makes no network calls. Everything stays on your device.

## License

All rights reserved — the app code is not licensed for reuse. The bundled Martian Mono Nerd Font
is covered by its own [SIL Open Font License 1.1](app/licenses/LICENSE.txt).