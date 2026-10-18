# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Alarmzy is a standalone Android alarm app whose differentiating feature is **alarm grouping**: users create named groups (Work, Gym, etc.) and a master toggle on each group enables/disables all member alarms at once — cascading the schedule/cancel down to every alarm in the group. Google Clock lacks this; Samsung's clock has it but is device-locked.

One module, `:app` (package `dev.redfox.alarmzy`, min SDK 31 / target/compile SDK 36). The design system comes from the Obsidian library (`dev.redfox:obsidian`, github.com/Redfox-OSS/obsidian).

Alarmzy is free and open source (GPL-3.0): every feature is available to everyone. There is no billing, Pro tier or paywall — don't add feature gates.

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

Release signing: when the gitignored `keystore.properties` (project root) exists, `release` is signed with the Play **upload key** (`~/Documents/Alarmzy Signing/alarmzy-upload.jks`); without it, it falls back to the debug key so sideload APKs still build. Release builds run **R8** (`isMinifyEnabled` + `isShrinkResources`; APK ~51 MB → ~4 MB) — add keep rules to `app/proguard-rules.pro` if a release-only crash appears (reflection/`valueOf` on renamed classes). `./gradlew assembleRelease -PdebugSigned` builds the shrunk release signed with the debug key so it installs over a debug build for testing. Play uploads use `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

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

### Design system (Obsidian)

The look comes from **Obsidian**, a separate open-source Compose library: `dev.redfox:obsidian` from the Maven repository at `https://redfox-oss.github.io/obsidian/maven` (version in `gradle/libs.versions.toml`, source at github.com/Redfox-OSS/obsidian). Its `docs/GUIDE.md` is the full spec, and its catalog app shows every token, component and transition. To change Obsidian itself, work in that repository (or `includeBuild("../obsidian")` locally) — not here. App-specific visuals (group icons/swatches in `ui/components/GroupVisuals.kt`) stay in `:app`.

- **Always dark.** There is no light/system theme. `ui/theme/Theme.kt`'s `AlarmzyTheme(accentColor, darkVariant)` is a thin adapter mapping the persisted domain `AccentColor` / `DarkVariant` onto `ObsidianTheme(ObsidianAccent, ObsidianNeutrals)` — a new accent/ramp needs an entry in both.
- Read tokens via `ObsidianTheme.colors` / `ObsidianTheme.typography`, **not** `MaterialTheme`. Numbers use `numericLarge`/`numericTile`/`numericMedium`/`numericSmall` with `unit` for AM/PM. Shapes/spacing: `ObShapes`, `ObSpacing`, `ScreenGutter`.
- Prefer Obsidian components (`ObCard`, `ObSwitch`, `ChoicePill`, `SegmentedPill`, `ObCheckbox`, `PrimaryButton`, `ActionPillButton`, `ObFab`, `LargeHeader`/`DetailHeader`, `ObDropdownMenu`, `ObAlertDialog`, `ObSheet`…) over stock M3.
- Icons are vector drawables `res/drawable/ic_*.xml` (Material Symbols Rounded), tinted at use. A few glyphs (delete, select-all, close, play/stop, alarm-off, warning, music library) temporarily use `Icons.Rounded.*`.
- **Glass and glow:** one `HazeState` via `LocalHazeState`; scrolling content gets `Modifier.glassSource()`, headers/nav use `Modifier.glassEffect(tint)` as *siblings*. Glows use the `ObGlows` presets, before `clip`.
- Tactile clicks use `Modifier.pressable { }` (no ripple).
- **Motion:** `ObMotion` springs, `revealOnEnter(index)` / `RevealColumn`, `popOnSelect`, `popIn`, `RollingText`, `HorizontalReveal`, `ExpandEnter`/`CollapseExit`, `HeaderActionsSwap`. Every nav destination is wrapped in a `RevealScope` by the `screen(...)` helper in `AlarmzyNavHost`; entrances replay on arrival but not when popping back (`RevealPolicy`). Nested sections that re-animate on expand (group members, custom days) get their own `RevealScope`. Keep per-frame work in `graphicsLayer`/`placeWithLayer` lambdas.
- `BottomNavBar` wraps Obsidian's `FloatingNavBar`: a shared pill halo and a per-tab `NavIconMotion`. Pad tab content by `floatingNavClearance()`.

### Alarm scheduling & playback engine (`alarm/`)

This is the most subtle part of the app. Flow of a firing alarm:

1. **`AlarmSchedulerImpl`** (`AlarmScheduler` interface, DI-bound) wraps `AlarmManager.setAlarmClock()` — exact, user-visible alarms. The alarm's `id.toInt()` is the `PendingIntent` request code, so scheduling the same id replaces its pending intent. `schedule()` is a no-op if the alarm is disabled or has no valid trigger time.
2. At trigger time **`AlarmReceiver`** (BroadcastReceiver) fires and starts **`AlarmService`** as a foreground service (`mediaPlayback` type).
3. **`AlarmService`** builds a high-importance, DND-bypassing, full-screen notification, launches **`AlarmRingingActivity`** (shown over the lock screen), and plays audio via **`AlarmPlayer`** (MediaPlayer + optional vibration + optional gradual volume). Snooze/Dismiss actions route back into the same service via `ACTION_SNOOZE` / `ACTION_DISMISS`. An auto-dismiss fires after `AUTO_DISMISS_MILLIS` (5 min).
   The ringing notification must always be reachable: it is ongoing + `FLAG_NO_CLEAR`, promoted as an Android 16 Live Update (indeterminate `ProgressStyle`), taps reopen the ringer, and its `deleteIntent` (`ACTION_REPOST`) re-posts it if the user swipes it away (Android 14+ allows swiping ongoing notifications). `RingingAlarmTracker` (`@Singleton` `StateFlow<RingingAlarm?>`, owned by `AlarmService`) is the single source of truth: `AlarmRingingActivity` finishes when it no longer matches its alarm, and the Alarms tab shows a "Ringing now" card with Snooze/Dismiss (routed back to the service via `RingingAlarmTracker.send`).
   Ringtone, vibration, gradual volume and snooze length are read from **Settings at ring time** (`AlarmService.currentSettings()`), not from the alarm row: the editor has no per-alarm controls, and the `ringtoneUri`/`vibrationEnabled`/… columns are only creation-time snapshots (using them made older alarms ignore a later Settings change).
4. **Dismiss** reschedules the alarm to its next occurrence for repeating alarms, or disables it for one-time alarms. **Snooze** re-arms via `AlarmScheduler.snooze()`.
5. **`BootReceiver`** re-computes next trigger times and re-schedules all enabled alarms after device reboot (schedules are lost on reboot).

`AlarmService` and `BootReceiver` are `@AndroidEntryPoint` and inject `AlarmDao` + `AlarmScheduler` directly (no repository) — they intentionally call the static helpers `AlarmRepositoryImpl.parseDays(...)` and `AlarmRepositoryImpl.calculateNextTriggerTime(...)` for the day-parsing / next-trigger logic. **Keep that trigger-time math in those companion functions** so every entry point (repository, service, boot) stays consistent.

### Stopwatch & Timer engine (`clock/`)

- `StopwatchEngine` and `TimerEngine` are `@Singleton`s that own the state (`StopwatchState` / `TimerState` in `domain/model`) and persist it to the settings DataStore, so they survive tab switches and process death. ViewModels just forward intents and mirror the engine's `StateFlow`. Engines store only timestamps; screens derive the live reading per frame with `rememberTicker()` (`presentation/common/ClockFormat.kt`).
- Both store wall-clock instants (`System.currentTimeMillis()`), so they survive process death and reboot (`BootReceiver` calls `timerEngine.rearmAfterBoot()` + `stopwatchEngine.refresh()` — re-arm **and** re-show). The timer's finish uses `setAlarmClock` (Doze-exempt; it will show as the system "next alarm"); a stale fire is ignored by `TimerEngine.finish`'s due-check.
- **Live Updates (Android 16 promoted notifications)**: see `clock/LiveUpdates.kt`. Promotion fails *silently* unless the notification is ongoing, titled, on the HIGH `clock_live` channel, never `setColorized`, and has both `setRequestPromotedOngoing(true)` and a `ProgressStyle` with `setStyledByProgress(true)`. Verify with `adb shell dumpsys notification --noredact | grep PROMOTED_ONGOING`. Never set `setShortCriticalText` while running: it overrides the ticking chronometer in the status chip / OnePlus Live Alerts pill. Running = chronometer only (count-up for the stopwatch, `setChronometerCountDown` for the timer); paused = `LiveUpdates.frozenChipText`. A 60s TICK per engine still re-posts to advance the progress bar. The Settings action is `Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`. Notification action buttons use the padded 24dp `ic_notif_*` icons — the Figma in-app glyphs are cropped to their bounds and get clipped on OxygenOS Live Alerts.
- Engine mutations with `reapplySideEffects = true` (refresh/boot/permission change) must never skip on "nothing changed". `ClockLiveAlerts.restoreAll()` re-posts both after permission changes. The permission UI (one-time home prompt on the Alarms tab, Settings → Live Alerts) lives in `presentation/livealerts/` and `clock/LiveAlertPermissions.kt`; BBK phones (OnePlus/Oppo/Vivo/Realme/iQOO) also need the unreadable "Show Live Updates on Live Alerts" toggle, shown as a "Check" step.
- All timer side effects (exact alarm, ongoing countdown notification, stopping the ring service) happen in `TimerEngine.applySideEffects` on each state transition — don't schedule/notify from elsewhere. `TimerReceiver` handles the fire + notification actions; `TimerRingService` rings (default ringtone/vibration from Settings) and reuses `AlarmRingingActivity` with `EXTRA_IS_TIMER` (single Stop button).
- The stopwatch has an ongoing notification too (`StopwatchNotifications`, actions via `StopwatchReceiver`), shown/cancelled by `StopwatchEngine` on each transition — same rule: don't post it from elsewhere.
- Notifications deep-link to a tab via `TimerNotifications.EXTRA_OPEN_TAB` (handled in `MainActivity.onCreate`/`onNewIntent`).

### Data model & the grouping invariant

- `AlarmEntity` has a nullable `groupId` FK to `GroupEntity` with `onDelete = SET_NULL` — **deleting a group keeps its alarms** (they become ungrouped). An alarm belongs to at most one group.
- `repeatDays` is stored as a comma-separated string of `DayOfWeek.value` ints; `RepeatMode` (sealed interface: `OneTime`/`Daily`/`Weekdays`/`Weekends`/`Custom`) is the domain representation. Convert with `RepeatMode.fromDays()` / `toDays()`.
- `nextTriggerTimeMillis` is denormalized onto the alarm row and recomputed on every save/toggle so `getNextEnabledAlarm()` (home-screen "next alarm" indicator) is a cheap `ORDER BY … LIMIT 1` query.
- **Cascade toggle lives in `GroupRepositoryImpl.toggleGroup()`**: it flips the group flag, flips every member alarm's `isEnabled`, recomputes trigger times, and schedules/cancels each alarm through the `AlarmScheduler`. Preserve this behavior — it's the app's headline feature.
- Groups have an optional category `icon` (`GroupIcon` enum; the persisted `key` must never be renamed) and `color` hex string.
- Room schema version is 3 with explicit `MIGRATION_1_2` (adds group `color`) and `MIGRATION_2_3` (adds group `icon`). `exportSchema = true` writes JSON to `app/schemas/` — bump the version and add a `Migration` (never rely on destructive migration) when changing entities.

**In-app updates** (`presentation/update/InAppUpdater`, created in `MainActivity.onCreate`): Play Core app-update. Releases with in-app update priority ≥ 4 (set via the Play Developer API, not the Console UI) force an IMMEDIATE update; anything else offers a FLEXIBLE update at most once a day and shows `UpdateReadyBanner` ("Update ready · Restart") when downloaded. Only works for Play-installed builds (internal testing counts); sideloaded builds silently get no update info.

Settings are stored in **DataStore Preferences** (not Room), read/written directly by `SettingsViewModel` via typed keys in its companion object.

## Permissions & platform notes

Declared in `AndroidManifest.xml`: `USE_FULL_SCREEN_INTENT` (Android 14+: without it the ringing alarm silently degrades to a banner — this was why OnePlus never showed the full-screen alarm; status via `LiveAlertPermissions.canUseFullScreen`, surfaced in the home prompt, Settings → Live Alerts, and a persistent Alarms-tab warning), `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (≤ SDK 32), `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, foreground-service (media playback), `VIBRATE`. `MainActivity` requests POST_NOTIFICATIONS at startup and routes the user to system settings for the overlay (`SYSTEM_ALERT_WINDOW`) permission — the overlay permission is what lets the ringing activity launch reliably while the screen is on (not just from the lock-screen full-screen intent).
