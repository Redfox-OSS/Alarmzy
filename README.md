# Alarmzy

A standalone Android alarm app with **alarm grouping** as its killer feature. Create named groups of alarms and toggle them all on or off with a single switch.

## Why Alarmzy?

Google Clock doesn't support alarm grouping. Samsung's Clock app does, but it's locked to Samsung devices. Alarmzy brings this feature to every Android phone.

## Features

### Core Alarm
- Create, edit, and delete alarms with Material3 time picker
- Repeat modes: Once, Daily, Weekdays, Weekends, or custom day selection
- Alarm labels
- Ringtone selection from device alarm tones with preview
- Vibration toggle
- Configurable snooze duration (5/10/15/20/30 min)
- Gradual volume increase
- Fullscreen alarm screen over lock screen with Snooze and Dismiss
- Next alarm indicator on the home screen

### Alarm Grouping
- Create named groups (e.g., Work, Gym, Weekend)
- Assign alarms to groups from either the alarm or group editor
- **Master toggle** on each group — one tap to enable or disable all alarms in the group
- Expandable group cards showing member alarms
- Group badge on alarm cards for quick identification
- An alarm can belong to one group at a time; deleting a group keeps its alarms

### Settings
- Dark / Light / System theme with Material You dynamic colors
- Default snooze duration, vibration, and gradual volume preferences

## Screenshots

*Coming soon*

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Architecture | Hybrid MVVM + MVI |
| Database | Room |
| DI | Hilt |
| Navigation | Navigation Compose |
| Preferences | DataStore |
| Scheduling | AlarmManager (`setAlarmClock`) |
| Alarm Playback | Foreground Service + MediaPlayer |
| Min SDK | 31 (Android 12) |

## Architecture

```
presentation/          MVVM+MVI — StateFlow<UiState> + sealed Intent per screen
domain/                Models (Alarm, AlarmGroup, RepeatMode) + repository interfaces
data/                  Room entities, DAOs, repository implementations
alarm/                 AlarmScheduler, AlarmReceiver, AlarmService, AlarmPlayer,
                       AlarmRingingActivity, BootReceiver
di/                    Hilt modules (Database, Repository, Alarm)
```

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17+ and Android SDK 36.

## License

TBD
