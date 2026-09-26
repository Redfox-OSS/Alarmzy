# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Alarmzy is a standalone Android alarm app whose differentiating feature is **alarm grouping**: users create named groups (Work, Gym, etc.) and a master toggle on each group enables/disables all member alarms at once — cascading the schedule/cancel down to every alarm in the group. Google Clock lacks this; Samsung's clock has it but is device-locked.

Single-module app (`:app`), package `dev.redfox.alarmzy`, min SDK 31 / target/compile SDK 36.

## Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests (src/test)
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
./gradlew lint                   # Android lint
./gradlew testDebugUnitTest --tests "dev.redfox.alarmzy.ExampleUnitTest.addition_isCorrect"  # single test
```

Requires JDK 17+. Dependencies are managed via the version catalog in `gradle/libs.versions.toml` — add libraries there, not with hardcoded versions in `app/build.gradle.kts`. KSP (not kapt) powers Room and Hilt code generation.

The `release` build type is intentionally signed with the **debug key** (see `app/build.gradle.kts`) so shared APKs install directly; swap in a real keystore before any Play upload.

## Architecture

Clean-architecture-ish layering under `app/src/main/java/dev/redfox/alarmzy/`:

- **`domain/`** — pure Kotlin models (`Alarm`, `AlarmGroup`, `RepeatMode`, `ThemeMode`, `AccentColor`) and repository interfaces. No Android/Room imports.
- **`data/`** — Room `local/` (entities, DAOs, `AlarmzyDatabase`, `Converters`, `GroupWithAlarms` relation) and `repository/` implementations that map entities ↔ domain models.
- **`presentation/`** — one package per screen (`alarms`, `groups`, `alarmedit`, `groupedit`, `settings`, `ringtone`) plus `navigation/`.
- **`alarm/`** — the scheduling/playback engine (see below).
- **`di/`** — Hilt modules (`DatabaseModule`, `RepositoryModule`, `AlarmModule`, `DataStoreModule`), all `@InstallIn(SingletonComponent)`.

### Presentation pattern (hybrid MVVM + MVI — follow this consistently)

Every screen has four files: `XUiState` (immutable data class), `XIntent` (sealed interface of user actions), `XViewModel`, `XScreen`.

- ViewModel exposes a single `val uiState: StateFlow<XUiState>` and a `fun onIntent(intent: XIntent)` that `when`-dispatches. There is no per-action public method.
- State is built by `combine`-ing repository `Flow`s (and any local `MutableStateFlow` such as selection state) inside an `init` block and collecting into `_uiState`. See `AlarmsViewModel` for the canonical example (selection mode is a private `MutableStateFlow` merged into UI state).
- Screens are stateless: they take `uiState` + `onIntent` and render. Navigation is done by the caller in `AlarmzyNavHost`, not the ViewModel.
- **Side effects** (navigate-back after save/delete, return a picked ringtone) use a separate `Channel`/`Flow` `sideEffect` on the ViewModel, collected in the NavHost via `LaunchedEffect` (`AlarmEditSideEffect`, `GroupEditSideEffect`, `RingtonePickerSideEffect`). Cross-screen results (ringtone picker → caller) are passed via `savedStateHandle` on the back-stack entry.

Navigation lives entirely in `presentation/navigation/AlarmzyNavHost.kt` + `Screen.kt` (routes). `MainActivity` hosts the `NavHost` in a `Box` with a floating frosted-glass pill nav (`BottomNavBar`) overlaid at the bottom, shown only on the five `BottomNavScreen` tabs (Alarms, Timer, Stopwatch, Groups, Settings). Tab screens pad their scrolling content by `floatingNavClearance()`. Theme (accent + dark variant) is driven reactively from `SettingsViewModel` at the `MainActivity` level.

### Design system (Alarmzy Obsidian — from the Stitch/Figma redesign)

- **Always dark.** There is no light/system theme; `DarkVariant` (Dark `#131316` / Darker `#0E0E11` / AMOLED) sets the neutrals, `AccentColor` (Crimson, Electric, Solar, Emerald, Luminous) the accent roles.
- Read tokens via `AlarmzyTheme.colors` / `AlarmzyTheme.typography` (`ui/theme/AlarmzyColors.kt`, `AppTypography.kt`), **not** `MaterialTheme`. A matching M3 scheme is still installed only so the few stock M3 pieces (dialogs, `DropdownMenu`, `ModalBottomSheet`) blend in. Font is Inter (single variable TTF, `res/font/inter_variable.ttf`, OFL license in `assets/licenses`).
- Reusable components live in `ui/components/` (`AzCard`, `AlarmzySwitch`, `ChoicePill`, `SegmentedPill`, `AzCheckbox`, `PrimaryButton`, `AzFab`, `LargeHeader`/`DetailHeader`, `AzDropdownMenu`, `AzAlertDialog`…). Prefer these over stock M3 components.
- Icons are vector drawables `res/drawable/ic_*.xml` converted from the Figma (Material Symbols Rounded) and tinted at use. A few glyphs not in the Figma set (delete, select-all, close, lock, play/stop, alarm-off, warning, music library) temporarily use `Icons.Rounded.*`.
- **Blur/glow:** Haze 1.7.x (not 2.x — that needs Compose 1.12/Kotlin 2.4). One `HazeState` is provided via `LocalHazeState`; screens mark scrolling content with `Modifier.glassSource()` and headers/nav use `Modifier.glassEffect(tint)` as *siblings* (never inside the source). Glows use the native `Modifier.glow(...)` wrapper over Compose's `dropShadow`.
- Tactile clicks use `Modifier.pressable { }` (scale-on-press, no ripple).

### Alarm scheduling & playback engine (`alarm/`)

This is the most subtle part of the app. Flow of a firing alarm:

1. **`AlarmSchedulerImpl`** (`AlarmScheduler` interface, DI-bound) wraps `AlarmManager.setAlarmClock()` — exact, user-visible alarms. The alarm's `id.toInt()` is the `PendingIntent` request code, so scheduling the same id replaces its pending intent. `schedule()` is a no-op if the alarm is disabled or has no valid trigger time.
2. At trigger time **`AlarmReceiver`** (BroadcastReceiver) fires and starts **`AlarmService`** as a foreground service (`mediaPlayback` type).
3. **`AlarmService`** builds a high-importance, DND-bypassing, full-screen notification, launches **`AlarmRingingActivity`** (shown over the lock screen), and plays audio via **`AlarmPlayer`** (MediaPlayer + optional vibration + optional gradual volume). Snooze/Dismiss actions route back into the same service via `ACTION_SNOOZE` / `ACTION_DISMISS`. An auto-dismiss fires after `AUTO_DISMISS_MILLIS` (5 min).
4. **Dismiss** reschedules the alarm to its next occurrence for repeating alarms, or disables it for one-time alarms. **Snooze** re-arms via `AlarmScheduler.snooze()`.
5. **`BootReceiver`** re-computes next trigger times and re-schedules all enabled alarms after device reboot (schedules are lost on reboot).

`AlarmService` and `BootReceiver` are `@AndroidEntryPoint` and inject `AlarmDao` + `AlarmScheduler` directly (no repository) — they intentionally call the static helpers `AlarmRepositoryImpl.parseDays(...)` and `AlarmRepositoryImpl.calculateNextTriggerTime(...)` for the day-parsing / next-trigger logic. **Keep that trigger-time math in those companion functions** so every entry point (repository, service, boot) stays consistent.

### Stopwatch & Timer engine (`clock/`)

- `StopwatchEngine` and `TimerEngine` are `@Singleton`s that own the state (`StopwatchState` / `TimerState` in `domain/model`) and persist it to the settings DataStore, so they survive tab switches and process death. ViewModels just forward intents and mirror the engine's `StateFlow`. Engines store only timestamps; screens derive the live reading per frame with `rememberTicker()` (`presentation/common/ClockFormat.kt`).
- Stopwatch uses `SystemClock.elapsedRealtime()` (reset if `BOOT_COUNT` changed). Timer uses wall-clock `endAtMillis` so it can be scheduled with `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP)` (deliberately not `setAlarmClock`, so timers don't show as the system "next alarm") and re-armed by `BootReceiver`.
- All timer side effects (exact alarm, ongoing countdown notification, stopping the ring service) happen in `TimerEngine.applySideEffects` on each state transition — don't schedule/notify from elsewhere. `TimerReceiver` handles the fire + notification actions; `TimerRingService` rings (default ringtone/vibration from Settings) and reuses `AlarmRingingActivity` with `EXTRA_IS_TIMER` (single Stop button).
- Notifications deep-link to a tab via `TimerNotifications.EXTRA_OPEN_TAB` (handled in `MainActivity.onCreate`/`onNewIntent`).

### Data model & the grouping invariant

- `AlarmEntity` has a nullable `groupId` FK to `GroupEntity` with `onDelete = SET_NULL` — **deleting a group keeps its alarms** (they become ungrouped). An alarm belongs to at most one group.
- `repeatDays` is stored as a comma-separated string of `DayOfWeek.value` ints; `RepeatMode` (sealed interface: `OneTime`/`Daily`/`Weekdays`/`Weekends`/`Custom`) is the domain representation. Convert with `RepeatMode.fromDays()` / `toDays()`.
- `nextTriggerTimeMillis` is denormalized onto the alarm row and recomputed on every save/toggle so `getNextEnabledAlarm()` (home-screen "next alarm" indicator) is a cheap `ORDER BY … LIMIT 1` query.
- **Cascade toggle lives in `GroupRepositoryImpl.toggleGroup()`**: it flips the group flag, flips every member alarm's `isEnabled`, recomputes trigger times, and schedules/cancels each alarm through the `AlarmScheduler`. Preserve this behavior — it's the app's headline feature.
- Groups have an optional category `icon` (`GroupIcon` enum; the persisted `key` must never be renamed) and `color` hex string.
- Room schema version is 3 with explicit `MIGRATION_1_2` (adds group `color`) and `MIGRATION_2_3` (adds group `icon`). `exportSchema = true` writes JSON to `app/schemas/` — bump the version and add a `Migration` (never rely on destructive migration) when changing entities.

Settings are stored in **DataStore Preferences** (not Room), read/written directly by `SettingsViewModel` via typed keys in its companion object.

## Permissions & platform notes

Declared in `AndroidManifest.xml`: `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (≤ SDK 32), `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, foreground-service (media playback), `VIBRATE`. `MainActivity` requests POST_NOTIFICATIONS at startup and routes the user to system settings for the overlay (`SYSTEM_ALERT_WINDOW`) permission — the overlay permission is what lets the ringing activity launch reliably while the screen is on (not just from the lock-screen full-screen intent).
