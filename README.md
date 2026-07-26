# Noter

[![Latest release](https://img.shields.io/github/v/release/jehan593/noter)](https://github.com/jehan593/noter/releases/latest)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://github.com/jehan593/noter)
[![Min SDK](https://img.shields.io/badge/minSdk-26-blue)](app/build.gradle.kts)
[![Permissions](https://img.shields.io/badge/permissions-INTERNET%20only-informational)](#permissions)

A simple, offline-first Android note + task app. Nord color palette, Martian Mono Nerd Font,
Jetpack Compose UI, two home-screen widgets.

Everything is stored locally on-device. The one exception is **Send** on the Notes tab, an
opt-in action that POSTs the current note to a user-configured [Notesnook](https://notesnook.com)
inbox — nothing else in the app touches the network.

## Download

Grab the latest APK from [Releases](https://github.com/jehan593/noter/releases/latest) — or the
permanent link `https://github.com/jehan593/noter/releases/latest/download/app-release.apk`,
which always resolves to the newest build (works well with [Obtainium](https://github.com/ImranR98/Obtainium)
for update checks). Requires Android 8.0 (API 26) or newer.

## How it works

- **Notes tab**: one free-form text box — type into it and it autosaves as you go (debounced,
  with an immediate flush when the app is backgrounded). **Copy** puts the note text on the
  clipboard; **Clear** empties it immediately; **Send** posts the note to your Notesnook inbox
  (configure the Inbox API key and an optional tag ID via the gear icon — from Notesnook:
  Settings > Inbox > Create Key).
- **Tasks tab**: add tasks with the input at the top, tap a checkbox to mark one done
  (shown with strike-through), and **long-press + drag** a task to reorder the list — order and
  done-state persist across restarts.
- **Widgets**: a Note widget (previews the current note, truncated) and a Task widget (previews
  the first few tasks with their done state). Tapping either widget opens the app directly on the
  matching tab.

## Building

`./gradlew assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`. The Gradle
wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is included, so no
regeneration step is needed.

1. Open this `noter/` folder in Android Studio (it will pick up the existing wrapper and sync
   automatically), **or** from a terminal with JDK 17 and the Android SDK installed:
   ```sh
   # local.properties needs sdk.dir pointed at your Android SDK if Android Studio hasn't
   # already created one for you, e.g.:
   echo "sdk.dir=/path/to/Android/sdk" > local.properties
   ./gradlew assembleDebug
   ```
2. Install/run on a device or emulator running Android 8.0 (API 26) or newer:
   `adb install app/build/outputs/apk/debug/app-debug.apk`.

### Fonts

Martian Mono Nerd Font (`.ttf`, Regular/Medium/Bold) is bundled under `app/src/main/res/font/`,
sourced from the [Nerd Fonts](https://github.com/ryanoasis/nerd-fonts) project releases (license
in `app/src/main/res/font/LICENSE.txt`). Home-screen widget text falls back to the
system monospace font — Glance/RemoteViews custom-font support is inconsistent across launchers,
so the in-app screens are where you'll see the real Martian Mono.

## Permissions

Just `INTERNET`, used solely by the opt-in Send-to-Notesnook action on the Notes tab. There's no
background service, no runtime-grant permission, and no network access anywhere else in the app.

## License

All rights reserved — no license is granted to use, modify, or redistribute this code. The
bundled Martian Mono Nerd Font is separately licensed under the
[SIL Open Font License 1.1](app/src/main/res/font/LICENSE.txt).
