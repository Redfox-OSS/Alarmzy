# Obsidian — design system

The Alarmzy look, extracted into a standalone Jetpack Compose library you can drop into any Android app:
always-dark surfaces, one vivid accent, soft colored glows, frosted glass, and spring-driven motion
that's interruptible everywhere.

- **Module:** `:obsidian` · **package:** `dev.redfox.obsidian` · **min SDK** 31
- **Depends on (exposed as `api`):** Compose BOM, Material 3, [Haze](https://github.com/chrisbanes/haze) 1.7.x
- **Font:** Inter (variable TTF, bundled; OFL license in `assets/licenses/Inter-OFL.txt`)
- **Living style guide:** `ObsidianShowcase()` — every token and component on one screen

```
obsidian/src/main/java/dev/redfox/obsidian/
├── theme/        ObsidianTheme, ObsidianColors (+ Accent/Neutrals presets), ObsidianTypography, ObShapes, ObSpacing
├── effects/      glow + ObGlows presets, breathingGlow, ambientFloat, glassSource/glassEffect, pressable
├── motion/       ObMotion springs, RevealScope/revealOnEnter/RevealColumn, popIn, popOnSelect, RollingText, transitions
├── components/   buttons, switch, checkbox, pills, segmented control, cards, rows, headers, menus, dialogs, empty state
├── navigation/   FloatingNavBar (liquid indicator + per-icon motion), floatingNavClearance()
└── showcase/     ObsidianShowcase
```

---

## 1. Using it in another app

### Option A — source module (simplest, keeps you editing one copy)

Copy (or `git submodule` / symlink) the `obsidian/` folder into the other project, then:

```kotlin
// settings.gradle.kts
include(":obsidian")

// app/build.gradle.kts
dependencies { implementation(project(":obsidian")) }
```

The module reads `libs.versions.toml` aliases: `android-library`, `kotlin-compose`, `androidx-compose-bom`,
`androidx-compose-ui`, `androidx-compose-ui-graphics`, `androidx-compose-material3`,
`androidx-compose-ui-tooling(-preview)` and `haze`. Copy those entries if the host catalog lacks them.

### Option B — Maven artifact

```bash
./gradlew :obsidian:publishToMavenLocal
```

```kotlin
// other app: settings.gradle.kts → repositories { mavenLocal() }
dependencies { implementation("dev.redfox:obsidian:1.0.0") }
```

Bump `version` in `obsidian/build.gradle.kts` when you change it. The same `publishing {}` block works with
GitHub Packages or any Maven repo by adding a `repositories {}` target.

### Root setup

```kotlin
setContent {
    ObsidianTheme(accent = ObsidianAccent.Electric, neutrals = ObsidianNeutrals.Darker) {
        val hazeState = rememberHazeState()                       // dev.chrisbanes.haze
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            Box(Modifier.fillMaxSize().background(ObsidianTheme.colors.background)) {
                AppContent()                                       // scrolling content uses Modifier.glassSource()
                FloatingNavBar(items, selectedIndex, onSelect, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
```

Read tokens via `ObsidianTheme.colors` / `ObsidianTheme.typography` — **not** `MaterialTheme`. A matching M3 color
scheme, type scale and shapes are still installed so stock `AlertDialog`, `DropdownMenu`, `ModalBottomSheet`,
`TextField` blend in.

---

## 2. Color

Always dark. A theme = **one accent** × **one neutral ramp**; every other token is derived by `obsidianColors()`.

### Accents (`ObsidianAccent.Presets`)

| Accent   | `main` (accent) | `light` (accentLight) | `onMain`  | `onLight` | Picker swatch |
|----------|-----------------|-----------------------|-----------|-----------|---------------|
| Crimson  | `#FF535A`       | `#FFB3B0`             | `#5B000D` | `#680010` | `#FF2A42`     |
| Electric | `#3D9BFF`       | `#A9CEFF`             | `#002A55` | `#00315F` | `#0084FF`     |
| Solar    | `#FFB800`       | `#FFD983`             | `#3F2B00` | `#402D00` | `#FFB800`     |
| Emerald  | `#21D07A`       | `#8DF3B7`             | `#00391B` | `#003920` | `#00E676`     |
| Luminous | `#EDEDF0`       | `#FFFFFF`             | `#1A1A1D` | `#1A1A1D` | `#FFFFFF`     |

Custom brand accent: `ObsidianAccent("Brand", main = …, light = …, onMain = …, onLight = …)`. Pick `light` as a
pale tint of `main` (it's used for text on 20–25% `main` containers), and `onMain` as a very dark shade of `main`.

### Neutral ramps (`ObsidianNeutrals.Presets`)

| Ramp    | background | card      | cardInner | tile      | tileHigh  |
|---------|------------|-----------|-----------|-----------|-----------|
| Dark    | `#131316`  | `#201F22` | `#1C1B1E` | `#2A2A2D` | `#353438` |
| Darker  | `#0E0E11`  | `#1A191D` | `#161519` | `#242328` | `#2F2E33` |
| AMOLED  | `#000000`  | `#121114` | `#0D0C0F` | `#1E1D21` | `#29282D` |

### Derived tokens

| Token | Value | Use |
|---|---|---|
| `cardGlass` | card @ 70% | cards over blurred/scrolling content |
| `border` / `borderStrong` | white @ 5% / 10% | hairlines on cards, pills; floating surfaces |
| `divider` | tileHigh @ 40% | 1dp list dividers (`ObDivider`) |
| `headerScrim` / `navScrim` | background @ 80% / card @ 80% | tints over frosted glass |
| `textPrimary` | `#E5E1E5` | titles, values |
| `textSecondary` | `lerp(#CBC7CC, accent.light, 0.45)` | subtitles, labels — **picks up a hint of the accent** |
| `textTertiary` | textSecondary @ 70% | hints |
| `textDisabled` | `#52525B` | disabled, unchecked checkbox outline |
| `accentContainer` / `…Strong` | accent @ 20% / 25% | selected pill / chip / indicator fill |
| `accentBorder` | accentLight @ 30% | border of selected pills |
| `accentGlow` | accent @ 40% | halos |
| `switchTrackOff` / `switchThumbOff` | `#474952` / `#B7B8C2` | off switch |
| `warning` / `error` | `#F59E0B` / `#FFB4AB` | status |

**Rules:** selected state = `accentContainerStrong` fill + `accentBorder` + `accentLight` text. Primary CTA =
solid `accent` + `onAccent` label. On a solid accent with a light accent (Luminous, `luminance > 0.6`) use
`onAccent`, otherwise white, for icons.

---

## 3. Typography

Inter everywhere; numbers are **Light with negative tracking**, labels are **SemiBold with positive tracking**.
Swap the face with `obsidianTypography(fontFamily = MyFont)` → pass to `ObsidianTheme(typography = …)`.

| Style | Weight | Size / line | Tracking | Use |
|---|---|---|---|---|
| `timeLarge` | Light | 44 / 48 | −0.05em | hero numeric readout |
| `timeTile` | Light | 44 / 44 | −0.025em | picker tiles |
| `timeMedium` | Light | 26 / 32 | −0.025em | nested readouts |
| `timeSmall` | SemiBold | 20 / 28 | −0.025em | compact rows |
| `meridiem` | Medium | 14 / 18 | +0.02em | AM/PM, units |
| `screenTitle` | SemiBold | 20 / 26 | −0.5sp | large page header |
| `title` | SemiBold | 20 / 26 | — | card titles, dialog titles |
| `headerTitle` | SemiBold | 16 / 24 | −0.4sp | detail header |
| `bodyLarge` | Medium | 16 / 24 | — | form row titles |
| `body` | Regular | 14 / 20 | — | body, subtitles |
| `label` | Medium | 12 / 16 | +0.04em | chips, small buttons |
| `labelStrong` | SemiBold | 12 / 16 | +0.04em | settings row titles |
| `sectionLabel` | SemiBold | 12 / 16 | +0.05em | UPPERCASE section headers |
| `caption` | SemiBold | 10 / 14 | +0.05em | UPPERCASE eyebrows |
| `button` | Bold | 16 / 24 | — | primary CTA |

---

## 4. Shape & spacing

| `ObShapes` | Radius | | `ObSpacing` | dp |
|---|---|---|---|---|
| `Card`, `Tile` | 16 | | `XXS` / `XS` / `S` | 4 / 6 / 8 |
| `CardLarge` (dialogs, sheets) | 24 | | `M` / `L` | 12 / 16 |
| `Small` | 12 | | `Gutter` (= `ScreenGutter`) | 20 — every page's horizontal edge |
| `Fab` | 18 (squircle) | | `XL` / `XXL` | 24 / 32 |
| `Checkbox` | 8 | | | |
| `Pill` | full | | | |

Card content padding 16. Header bar heights: `LargeHeaderHeight` 64, `DetailHeaderHeight` 60. Tap targets ≥ 44.

---

## 5. Glow

Glows are colored drop shadows (`Modifier.glow`, over Compose's native `dropShadow`). They're what makes the
accent feel lit. Apply **before `clip`** — the glow draws outside the bounds.

```kotlin
Modifier
    .glow(colors.accent, CircleShape, ObGlows.Cta)   // preset
    .clip(CircleShape)
    .background(colors.accent)

Modifier.glow(color, shape, radius = 12.dp, spread = 0.dp, offsetY = 0.dp)   // raw
```

| `ObGlows` | Alpha × color | Radius | Offset Y | Used on |
|---|---|---|---|---|
| `Switch` | 0.35 | 6 | 0 | switch track when on (animated 0→1) |
| `Checkbox` | 0.25 | 6 | 2 | checked checkbox |
| `Action` | 0.45 | 12 | 0 | primary action pill, selected swatch |
| `Fab` | 0.45 | 14 | 8 | FAB |
| `Cta` | 0.30 | 18 | 8 | full-width primary button |
| `Halo` | 1.0 (use `accentGlow`) | 18 | 0 | nav bar selection bubble |
| `Ambient` | 0.34 | 32 | 0 | hero / empty-state badge |
| `Elevation` | 0.35 (use **black**) | 24 | 12 | floating surfaces (nav bar) |

Living glows: `Modifier.breathingGlow(color, shape)` pulses alpha 0.16↔0.34 over 2.6s; pair with
`Modifier.ambientFloat()` (±4dp drift) for an idle hero element.

**Rules:** only glow things that are *on*/primary — never neutral surfaces. One strong glow per region.
Disabled = no glow + 40% alpha.

## 6. Glass

Frosted glass via Haze: provide one `HazeState` through `LocalHazeState`, mark scrolling content with
`Modifier.glassSource()`, and give headers / floating bars `Modifier.glassEffect(tint)` (blur 12dp, no noise).
The glass surface must be a **sibling** of the source, never inside it; clip to the shape before `glassEffect`.
`LargeHeader`, `DetailHeader` and `FloatingNavBar` already do this. Without a provider both are no-ops.

---

## 7. Motion

Everything interactive is a **spring** (interruptible mid-flight); only choreographed entrances use easing.

### Springs — `ObMotion`

| Spec | Damping | Stiffness | For |
|---|---|---|---|
| `bouncy()` | 0.55 | 380 | pops, check marks, icons appearing |
| `snappy()` | 0.82 | 700 | toggles, color changes, pills |
| `gentle()` | 0.90 | 240 | entrances, layout, glow fades |
| `EaseOutEmphasized` | cubic (0.05, 0.7, 0.1, 1) | — | staggered column entrances |
| `StaggerMillis` / `MaxStaggerIndex` | 38ms / 9 | — | list stagger step / cap |

### Press feedback — `Modifier.pressable { }`

Replaces ripple everywhere: sinks to `pressedScale` (default 0.97) with a firm spring (1.0 / 900), releases with
an overshoot (0.42 / 420). Optional `haptic`, `onLongClick` (always long-press haptic). Typical scales: rows
0.985, cards 0.97, buttons 0.96, action pills 0.93, header icons 0.9, chips/FAB 0.88, nav tabs 0.86.

### Entrances

- `RevealScope(enabled)` — wrap each screen/destination. Items animate only while the scope is < 650ms old, so
  scrolling a lazy list never replays them; pass `enabled = false` when popping back to a screen.
- `Modifier.revealOnEnter(index, offsetY = 22.dp)` — rise + fade + scale 0.96→1, staggered by index.
- `RevealColumn { }` — drop-in `Column` whose children stagger in from one shared clock (forms, settings).
- `Modifier.popIn(from = 0.88f)` — springy scale-in for dialogs, sheets, FABs.

### State changes

- `Modifier.popOnSelect(selected, amount = 0.08f)` — swell + bounce when something becomes selected.
- `RollingText(text, style, color)` — odometer digits; up when growing, down when shrinking (`rollUp` to force).
- `RollSwap` — whole-text roll (`AnimatedContent` transitionSpec).
- `HorizontalReveal(visible)` — inline content grows its width while popping in (selection checkboxes).
- `ExpandEnter` / `CollapseExit` — springy disclosure for `AnimatedVisibility`.
- `HeaderActionsSwap` — scale-and-fade swap of header action sets.

### Signature component motion

- **Switch**: thumb stretches 6dp while held (iOS squish), crosses on (0.58 / 520), track color snappy, glow fades in gently, optional dot bounces in.
- **Segmented pill**: indicator slides (0.6 / 420) and stretches 18% wide / 8% short in transit — "liquid".
- **Nav bar**: one shared halo travels (0.58 / 300), stretching up to 55% with velocity; each icon plays a `NavIconMotion` — `Pop`, `Shake`, `Flip`, `Press`, `Jelly`, `Turn`.
- **FAB**: pops in from 50% while its glyph twists from −135°.

Keep per-frame work in `graphicsLayer {}` / `placeWithLayer {}` lambdas (draw phase only, no recomposition).

---

## 8. Components

| Component | Notes |
|---|---|
| `PrimaryButton(text, onClick, icon?, enabled)` | full-width accent pill, `Cta` glow, rolling digits in label, Confirm haptic |
| `ActionPillButton(text, onClick, icon?, primary)` | 56dp pill; primary = accent + `Action` glow; label morphs on change |
| `PillButton(text, onClick, icon?, contentColor, containerColor)` | small neutral pill |
| `ObFab(icon, contentDescription, onClick)` | 56dp squircle, accent gradient, `Fab` glow, spin-in |
| `HeaderIconButton(icon \| imageVector, …)` | 44dp round tap target |
| `ObSwitch(checked, onCheckedChange, size, showDot)` | `SwitchSize.Large/Medium/Small` (52×30 / 48×28 / 46×26) |
| `ObCheckbox(checked, onCheckedChange)` | 24dp squircle, bouncy check |
| `ChoicePill`, `DayChip` | single-choice pill; 40dp circular toggle |
| `SegmentedPill(options, selectedIndex, onSelect)` | liquid sliding indicator, check mark on selected |
| `ObCard(shape, color, borderColor, contentPadding, onClick, onLongClick)` | base surface (Column) |
| `ObListRow(title, icon?, subtitle?, …, trailing)` | icon tile + text + trailing slot |
| `IconTile`, `SectionLabel`, `ObDivider` | building blocks |
| `LargeHeader(title, actions)` / `DetailHeader(title, onBack, actions)` | frosted headers; title rolls on change |
| `ObDropdownMenu` / `ObMenuItem` | squircle floating menu |
| `ObAlertDialog(title, text, confirmLabel, …, destructive)` | card dialog with pill buttons, pops in |
| `EmptyState(icon, title, subtitle)` | floating, breathing badge with staggered text |
| `FloatingNavBar(items, selectedIndex, onSelect)` | frosted pill nav; pad content by `floatingNavClearance()` |
| `ObsidianShowcase(onAccentChange?)` | style guide screen |

## 9. Haptics

| Interaction | `HapticFeedbackType` |
|---|---|
| Switch / checkbox | `ToggleOn` / `ToggleOff` |
| Chip, pill, segment, tab change | `SegmentTick` |
| Primary CTA | `Confirm` |
| FAB, action pill, re-tap current tab | `ContextClick` |
| Long press | `LongPress` (automatic in `pressable`) |

## 10. Icons

Material Symbols **Rounded**, 24dp, converted to vector drawables and tinted at use (`textSecondary` at rest,
`textPrimary` selected, `accentLight` on accent containers). The library only ships the glyphs its own components
need (`ob_ic_back`, `ob_ic_check`, `ob_ic_check_bold`); apps bring their own. Library resources use the `ob_`
prefix so they never clash with the host.
