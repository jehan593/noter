# Noter

A simple, fully offline Android note + task app. Nord color palette, Martian Mono Nerd Font,
Jetpack Compose UI, two home-screen widgets.

Noter requests **no permissions at all** — everything is stored locally on-device.

## How it works

- **Notes tab**: one free-form text box — type into it and it autosaves as you go (debounced,
  with an immediate flush when the app is backgrounded). **Copy** puts the note text on the
  clipboard; **Clear** empties it immediately.
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

None. Noter has no `<uses-permission>` entries at all — no internet access, no background
service, nothing that needs a runtime or special grant.
