# Echo — Voice Launcher (Android)

A real Android home-screen launcher with voice commands and a local Room
database for command history.

## What it does
- Replaces your Android home screen (shows up as an option when you press Home)
- Lists every installed app in a grid, tap to open
- Tap the mic orb, say "open spotify" / "search for pizza near me" — it launches
  the matching app or falls back to a web search
- Asks for microphone permission explicitly, with a clear banner if you deny it
- Stores your command history in a local SQLite database via Room — nothing
  leaves the device

## How to build it
1. Install **Android Studio** (free, from developer.android.com) if you don't have it.
2. Open this folder (`EchoLauncher/`) as a project — "Open" → select the folder.
3. Let Gradle sync (first sync downloads dependencies, needs internet).
4. Connect your phone via USB with USB debugging on (Settings → Developer
   options), or use an emulator.
5. Click **Run ▶**. The app installs and launches.

## How to set it as your home screen
1. Press the Home button on your phone.
2. Android will show a chooser ("Select Home app") — pick **Echo**.
3. To make it permanent: Settings → Apps → Default apps → Home app → Echo.
4. To switch back any time: same menu, pick your original launcher.

## Project structure
```
app/src/main/java/com/echo/launcher/
  MainActivity.kt         — UI (Jetpack Compose) + voice recognition + permission flow
  AppRepository.kt        — lists installed apps, launches them
  VoiceCommandHandler.kt  — parses "open X" / "search for X" into actions
  data/
    Entities.kt            — Room tables: command history, pinned apps
    AppDao.kt               — database queries
    AppDatabase.kt           — Room database setup
  ui/theme/                — colors/theme matching the original web prototype
```

## Notes
- Minimum Android version: 8.0 (API 26)
- No internet permission is requested — web search commands hand off to your
  default browser/search app rather than calling any API directly
- This is a starting point, not a polished consumer app — no app-drawer
  search/filter, no drag-to-reorder, no widgets. Those are natural next steps
  if you want to keep building on it.
