# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Noter: a fully offline Android note + task app. Nord color palette, Martian Mono Nerd Font, Jetpack
Compose UI, two home-screen widgets (Note + Task). The only `<uses-permission>` is `INTERNET`, and
only for the opt-in "Send to Notesnook" action on the Notes tab — no background service, no
runtime-grant permissions. Keep it that way unless the user explicitly asks for a feature that
requires a new permission.

## Commands

```sh
./gradlew assembleRelease     # build app/build/outputs/apk/release/app-release.apk (R8-minified, resource-shrunk — what CI ships)
./gradlew assembleDebug       # build app/build/outputs/apk/debug/app-debug.apk (local iteration only)
./gradlew build               # full build incl. lint/checks
```

- No test suite exists in this repo currently.
- `local.properties` needs `sdk.dir` pointing at an Android SDK if Android Studio hasn't already
  created one (`echo "sdk.dir=/path/to/Android/sdk" > local.properties`).
- `versionCode`/`versionName` are overridable via `-PappVersionCode=`/`-PappVersionName=` — CI
  (`.github/workflows/build-apk.yml`) passes `github.run_number` so every push gets a strictly
  increasing versionCode (required for update checkers like Obtainium to see a new build) and its
  own tag/release rather than overwriting one shared release.
- The `release` build type reuses the same pinned `debug.keystore` signing key as `debug` (see
  `app/build.gradle.kts`) so in-place updates via Obtainium keep working even though the shipped
  variant changed from debug to release.
- Build the APK and hand it off rather than installing to an emulator/adb yourself — the user
  tests on their own device.

## Architecture

Manual DI, no framework: `NoterApplication` owns a single `AppContainer`
(`di/AppContainer.kt`), lazily building the Room `AppDatabase`, `NotesRepository`, and
`TasksRepository`. Compose screens reach it via `rememberAppContainer()`
(`ui/AppContainerAccess.kt`), which casts `LocalContext.current.applicationContext`.
ViewModels are constructed directly from the repository (no `ViewModelProvider.Factory`
boilerplate beyond what's needed to pass the repo in).

**Two independent data stores, one per tab:**
- **Notes** — a single free-form string in DataStore Preferences (`NotesRepository`,
  key `note_text`). Autosaves debounced via `collectLatest` on a `MutableSharedFlow` of keystrokes
  (`NotesViewModel`), but since Android can freeze a backgrounded app's coroutines mid-flight the
  instant it's minimized, `flushPendingSaveBlocking()` is called synchronously from `ON_PAUSE` as
  the guaranteed-complete fallback save path.
- **Tasks** — Room (`AppDatabase` → `TaskDao` → `TaskEntity`), ordered by `orderIndex`.
  `TasksViewModel` deliberately uses `runBlocking` (not `viewModelScope.launch`) for every mutation
  — same freeze-on-background risk as notes, but here it's solved by blocking the calling (main)
  thread until the Room write + widget refresh both complete, rather than a lifecycle hook.
  Completed tasks sort to the bottom in the UI via a stable sort on `isDone` (order within each
  group still reflects drag position) so the in-app list and widget preview agree.

**Every repository mutation pushes a widget refresh** (`NoteWidgetProvider.requestUpdate` /
`TaskWidgetProvider.requestUpdate`) as its last step — this is the primary widget-refresh path.
`WidgetRefreshAlarmReceiver` (AlarmManager, ~15 min inexact repeating) is only a fallback for
pushes an OEM launcher's throttling dropped — deliberately AlarmManager over WorkManager, since
WorkManager's own manifest unconditionally adds four permissions (`WAKE_LOCK`,
`ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) this app doesn't otherwise
need for what's just a periodic broadcast.

**Widgets are classic `AppWidgetProvider` + `RemoteViews`, not Jetpack Glance.** Glance's own
`GlanceAppWidget.updateAll` update path has no protection against the cached-app freezer
suspending mid-flight right after the app backgrounds; routing every refresh through a broadcast
into the provider's `onReceive`/`goAsync()` gives it a proper background-execution grace period
instead (same pattern as Notally). Content is a `ListView` backed by a `RemoteViewsFactory`
(`NoteWidgetService`/`TaskWidgetService`) since that's the only way to get real scrolling in a
widget. Each tappable region (root, title, empty state) needs its own explicit
`setOnClickPendingIntent` — RemoteViews click regions don't reliably bubble up from a child to a
container's own click handler when the ListView claims touches for its own scroll-gesture
detection. `WidgetSizing.computeFillerRowCount` pads short lists with blank filler rows so the
widget's content height matches its actual current size (from `AppWidgetManager` options reported
in `onAppWidgetOptionsChanged`) without introducing dead space or spurious scroll; it deliberately
falls back to a small filler count rather than a generous one when size can't be read yet, since
underfilling is the less-bad failure mode.

Widget taps launch `MainActivity` with `EXTRA_START_TAB` (see `widget/WidgetIntents.kt`) to open
directly on the matching tab.

## Fonts

Martian Mono Nerd Font ships as bundled `.ttf`s under `res/font/` (license in
`MARTIAN_MONO_LICENSE.txt` at repo root). Glance/RemoteViews custom-font support is inconsistent
across launchers, so widgets fall back to the system monospace font — only the in-app screens
render the real Martian Mono.
