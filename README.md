# Noter

A simple, offline-first Android note + task app. Nord color palette, Martian Mono Nerd Font,
Jetpack Compose UI, two home-screen widgets.

Everything is stored locally on-device. The one exception is **Send** on the Notes tab, an
opt-in action that POSTs the current note to a user-configured [Notesnook](https://notesnook.com)
inbox — nothing else in the app touches the network.

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
in `MARTIAN_MONO_LICENSE.txt` at the project root). Home-screen widget text falls back to the
system monospace font — Glance/RemoteViews custom-font support is inconsistent across launchers,
so the in-app screens are where you'll see the real Martian Mono.

## Permissions

Just `INTERNET`, used solely by the opt-in Send-to-Notesnook action on the Notes tab. There's no
background service, no runtime-grant permission, and no network access anywhere else in the app.
