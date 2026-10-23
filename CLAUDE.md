# Days Counter (عدّاد الأيام) — countdowns + a resizable home-screen widget

A bare **React Native CLI** (TypeScript) app that counts down to the dates that matter — days,
hours, minutes and seconds, ticking live — with a **resizable Android home-screen widget** for the
pinned countdown. Bilingual (English / Arabic with RTL), light and dark themes, fully offline.
Two tabs (Home + Settings) and an animated loading screen.

See [`CONTINUE_PROJECT.md`](./CONTINUE_PROJECT.md) for a handoff snapshot if you're picking this up
fresh, [`TODO.md`](./TODO.md) for the phased checklist (source of truth for "what's done"), and
[`help.md`](./help.md) for the command cheat sheet.

**Project skills** (`.claude/skills/`) — check these before the matching kind of work:
- `verify` — **read before running `yarn test` / `yarn typecheck` / a Gradle build.** Don't run any
  of them proactively; the user wants an explicit "test it" / "build it" first.

**This is bare RN CLI, not Expo** — same as the sibling `Gregorian-hijri-date-converter` (the
template this app was built from), unlike `prayer-times-lb`. `android/` and `ios/` are real,
checked-in native projects you edit directly.

## Every commit = a version (user rule, 2026-10-23)

The user asked for **each commit to carry its own app version, a GitHub tag and a release, and to
be pushed to `develop`** (the repo's only branch). The routine, in this order:

1. `yarn bump minor` (feature) / `yarn bump patch` (fix) — in the **same** commit as the work.
2. `git commit` → `git tag -a v<version> -m "<version>"` → `git push origin develop --follow-tags`.
3. `yarn release` → GitHub release titled `<version>` (notes = the commit message;
   [`scripts/github-release.js`](./scripts/github-release.js), REST API with the token from
   `git credential fill` — `gh` isn't installed; never print the token).

Versions ran **0.1.0 (scaffold) → minor per feature → 1.0.0 = finished and fully tested**. 0.x
releases are marked pre-release. **No APKs on GitHub** — the user said "do not upload the apk to
github" (2026-10-23); the one briefly attached to 1.0.0 was removed and the script has no upload
option. Build APKs locally (help.md) and hand over the path.

## Stack

- React Native **0.87.1** (New Architecture, Hermes), TypeScript — the exact native versions the
  Hijri converter and lexitoon already build with on this machine (reanimated 4.6.0 + worklets
  0.12.1, gesture-handler 3.2.1, screens 4.27.0, svg 15.15.5, safe-area-context 5.9.1).
- **Yarn 1 (classic) only** — never npm.
- Navigation: `@react-navigation/native` + `native-stack` for the root, and **prayer-times-lb's
  live-drag tab pager** for Home/Settings (see "Tabs pager"). Native headers off everywhere.
- State: `zustand` + `persist` over AsyncStorage — settings and countdowns.
- i18n: `i18next` + `react-i18next`, device language via `react-native-localize`. RTL is
  **JS-driven** through `useDirection` (instant switch, no restart), exactly like the sibling apps
  — native `I18nManager` RTL is deliberately off, so every direction-sensitive layout mirrors
  itself via `rowDirection` / `textAlign` / `isRTL`.
- Animation: `react-native-reanimated` v4 (Babel plugin is `'react-native-worklets/plugin'`) +
  `react-native-gesture-handler` (imported first in `index.js`).
- Icons: `lucide-react-native` (SVG, one stroke family — no emoji icons).
- Native splash: `react-native-bootsplash` → hands off to the animated JS `LoadingScreen`.
- Relative imports (no path alias).

## Countdown engine — read before touching `src/core/`

- A countdown stores its target as a **wall-clock `LocalDateTime`** (`{year, month, day, hour,
  minute}`, device time zone), never an epoch — "5 Sep 2027 00:00" stays local midnight even if
  the phone changes time zone. `toInstant()` resolves it with `new Date(y, m-1, d, h, min)`.
- [`remaining(target, now)`](./src/core/countdown.ts) counts **calendar days anchored to the
  target's time of day**: `days` = the largest d with "target − d calendar days" ≥ now. A midnight
  countdown therefore flips its day count at local midnight, also across DST — counting 24-hour
  blocks would flip it at 23:00 or 01:00 for half the year. The H/M/S part is the physical time to
  that next flip (`nextChangeAt`), so on a DST day it can read up to 24:59:59 / start at 22:59:59.
  A skipped midnight (Lebanon springs forward 00:00 → 01:00) resolves to 01:00.
- Seconds are **truncated**, like Android's `Chronometer`, so the app and the widget tick together.
- After the target: `arrived` for the first day (the "The day is here!" state), then `past`, both
  counting up the same way. Settings → "Count up after the date" can hide the count-up.
- `calendarSpan` = the most whole months that fit (clamped to month ends: 31 Jan → 1 Mar is
  "1 month and 1 day"), then leftover days — computed in UTC day numbers, free of DST.
- Counted phrases ("10 months and 12 days", "١٠ أشهر و١٢ يومًا") come from
  [`src/i18n/plural.ts`](./src/i18n/plural.ts), **not** i18next plurals — those need
  `Intl.PluralRules`, which Hermes doesn't reliably provide, and Arabic needs 6 CLDR forms.
  `unitWord()` gives the bare noun for a label under a big number ("يومًا", "دقائق").
- Arabic UI shows Arabic-Indic digits via `localizeDigits()`; Gregorian month names are Levantine
  (كانون الثاني…), as Lebanese calendars print them.
- Tests: [`countdown.test.ts`](./src/core/__tests__/countdown.test.ts) runs in **Asia/Beirut**
  (`jest.globalSetup.js` sets `TZ`), covering the user's example (23 Oct 2026 15:00 → 316 days
  9:00:00 to 5 Sep 2027), every midnight flip on the way, both 2026/27 DST changes, leap day,
  arrived/past, spans and both languages' phrases.

### Stores

- [`useCountdownStore`](./src/store/useCountdownStore.ts) — `countdowns[]`, `pinnedId` (the Home
  hero + the widget; always set while any countdown exists — removing the pinned one hands it to
  the next one due), `seeded` (the user's 5 Sep 2027 example is added once on first launch, with
  an empty title so it shows as its date; deleting everything doesn't revive it). Persisted data
  is sanitized on load (`merge`).
- [`useSettingsStore`](./src/store/useSettingsStore.ts) — language, theme mode, show seconds,
  count up, 24-hour clock (default from `react-native-localize`'s `uses24HourClock()`).
- App start waits for **both** stores' `hasHydrated`.

## UI architecture

- **Tabs pager** ([`MainTabs.tsx`](./src/navigation/MainTabs.tsx)): ported from prayer-times-lb via
  the Hijri converter — all pages side by side, the row live-translates with the finger, snaps on
  release/fling. `failOffsetY` keeps vertical drags (scrolling, wheels) from starting a tab swipe.
  **In Arabic the row and the tab bar mirror together.** Screens reach tabs via
  `useTabPager().goToTab()`.
- **One clock** ([`useClock.ts`](./src/hooks/useClock.ts)): `useClockDriver` (mounted once in
  App.tsx) ticks a zustand store on whole seconds — whole minutes when "Show seconds" is off —
  aligned to the wall clock, and stops in the background. Every countdown reads `useNow()`, so
  they all tick together. Don't add per-component intervals.
- **Home** ([`HomeScreen.tsx`](./src/screens/HomeScreen.tsx)): header (today's date + round "+"
  button) → [`HeroCountdown`](./src/components/home/HeroCountdown.tsx) for the **pinned**
  countdown (its gradient, big day count, H/M/S tiles whose digits slide in on change, progress of
  the wait since the countdown was created, "The day is here!" on the day) →
  [`StatsCard`](./src/components/home/StatsCard.tsx) (same wait on the calendar / in weeks / in
  hours) → every other countdown as a [`CountdownRow`](./src/components/home/CountdownRow.tsx)
  (headline number drops to hours/minutes on the last day; pin button moves it to the hero +
  widget). The list re-sorts only when a countdown passes, not every second. Empty state when
  nothing is left. A countdown with no name shows its date as the title
  ([`present.ts`](./src/core/present.ts)).
- **Editor** ([`CountdownEditor.tsx`](./src/components/editor/CountdownEditor.tsx)): one bottom
  sheet for add + edit — live preview in the chosen gradient, name (optional, 40 chars), date
  wheels (1900–2100, day clamped to the month) and time wheels (12/24 h per Settings) from
  [`DateTimeWheels`](./src/components/picker/DateTimeWheels.tsx) on the Hijri converter's
  `WheelPicker`, 9 colors, 12 icons, "show on the widget", delete (confirm). A new countdown
  defaults to one month ahead at 00:00 in the first unused color, pinned if none is. A past date
  is allowed (note shown) — it counts up.
- **Sheets are not RN `Modal`s.** [`BottomSheet`](./src/components/ui/BottomSheet.tsx) draws a
  scrim + sliding sheet over the whole app, closes on the Android back button, and takes a
  numeric max height from the window. Every sheet (editor, countdown picker) is mounted once in
  [`SheetHost`](./src/components/SheetHost.tsx) (App.tsx, above the navigator) and opened through
  [`useSheetStore`](./src/store/useSheetStore.ts) (`openEditor(id | null)`, `openPicker()`).
  Why: see "Found on the first emulator run" below.
- **Settings** ([`SettingsScreen.tsx`](./src/screens/SettingsScreen.tsx)), in this order:
  language (segmented EN/AR) · appearance (follow-system switch, light/dark tiles otherwise —
  same interaction as the sibling apps) · countdown (show seconds — the clock then ticks per
  minute; count up after the date; 24-hour time) · home-screen widget
  ([`WidgetSection`](./src/components/settings/WidgetSection.tsx), Android only: the pinned
  countdown + "Change" → [`CountdownPickerSheet`](./src/components/settings/CountdownPickerSheet.tsx),
  "Add the widget" via `requestPinAppWidget` when the launcher supports it (manual steps
  otherwise), resize hint, lock-screen note) · data (delete all, confirm) · about (logo,
  version from package.json, description).
- **Loading**: bootsplash (night `#170B26`, 120 dp emblem) → `LoadingScreen`, whose first frame is
  identical (emblem drawn with `arc={false}` + a separate `ArcLayer` at rest), so
  `BootSplash.hide({ fade: false })` is invisible; then the gold arc spins, glows drift, sparks
  twinkle, min 1.2 s (`MIN_LOADING_VISIBLE_MS` in `App.tsx`). Waits for both stores to hydrate.
  Respects reduced motion.

## Android home-screen widget — read before touching `android/…/widget/`

- **Bridge**: an app-local **Turbo Native Module** `DaysWidget`. Spec
  [`src/native/NativeDaysWidget.ts`](./src/native/NativeDaysWidget.ts) → codegen
  (`package.json` → `codegenConfig`, `javaPackageName com.devlance.dayscounter.widget`) generates
  `NativeDaysWidgetSpec` → [`DaysWidgetModule.kt`](./android/app/src/main/java/com/devlance/dayscounter/widget/DaysWidgetModule.kt),
  registered by hand in `MainApplication` via `DaysWidgetPackage` (app code isn't autolinked).
  `TurboModuleRegistry.get` (not `getEnforcing`), so iOS — no native side — gets `null` and
  `widgetAvailable` is false. Methods: `setPayload(json)`, `requestPin()`,
  `isRequestPinSupported()`, `getInstalledCount()`.
- **The widget computes the countdown itself.** JS pushes only the pinned countdown's wall-clock
  target, title/detail lines, color, language, count-up flag and a few UI strings
  ([`payload.ts`](./src/widget/payload.ts)); [`useWidgetSync`](./src/widget/useWidgetSync.ts)
  (App.tsx) re-pushes when any of that changes and on every foreground. Natively,
  [`Countdown.kt`](./android/app/src/main/java/com/devlance/dayscounter/widget/Countdown.kt) is a
  port of `remaining()` (lenient `Calendar`, same calendar-day semantics) — **keep the two in
  sync**, and keep `WidgetPayload` in sync with `WidgetStore.kt`. Known difference: for a target
  wall time inside a DST *repeated* hour, JS picks the first occurrence and Java the second.
- **Live seconds without the app**: the day count is text; H:MM:SS is a `Chronometer` counting
  down to `nextChangeAt` (or up from the last change after the date). `DaysWidgetProvider`
  schedules an alarm for the day change and one **an hour before it**, to switch the Chronometer
  format to `"0:%s"` — by default it drops the hour under 60 minutes ("59:59"), which would read
  as H:MM. Exact alarms only if "Alarms & reminders" is allowed (`SCHEDULE_EXACT_ALARM`,
  pre-granted on 12/12L), otherwise a 1-minute window; `updatePeriodMillis` 30 min is the
  backstop; TIME_SET / TIMEZONE / DATE / LOCALE changes and app updates refresh too.
  The Chronometer's digits follow the **phone's** locale (Android API), the rest the app language.
- **Resizable**: four layouts sharing view ids — `widget_small` (1×1/2×1: number + unit),
  `widget_square` (2×2: + name + H:MM:SS), `widget_wide` (n×1: name/date | number/H:MM:SS),
  `widget_large` (3×2+: + progress). Android 12+ gets them as a size map
  (40×40 / 100×100 / 180×50 / 200×110 dp — the launcher takes the closest that fits, live while
  resizing); older versions pick from `OPTION_APPWIDGET_MIN_WIDTH`/`MAX_HEIGHT` with the same
  thresholds. Min resize 40×40 dp (1×1), default 3×2.
- Backgrounds: one gradient drawable per countdown color (`res/drawable/widget_bg_*.xml`, same
  stops as `COUNTDOWN_PALETTE`), set with `setBackgroundResource` on the root
  (`@android:id/background`, `clipToOutline`, radius = the launcher's on 12+). White text.
  RTL via `setLayoutDirection` on the root; the progress drawable clips from "start".
- Unit words use Android `<plurals>` (values-ar has zero/one/two/few/many/other, matching
  `AR_WORD.day`) through a context switched to the **app's** language. Numbers are mapped to
  Arabic-Indic digits **by hand** (`formatNumber` in the provider): Android's `ar` locale formats
  with Latin digits, unlike the app.
- **Lock screen**: declared `home_screen|keyguard` with `initialKeyguardLayout`. Stock Android
  phones don't allow third-party lock-screen widgets (tablets on Android 15+ and some vendors do);
  the user declined a lock-screen notification instead (2026-10-23).
- Picker preview: `widget_preview.xml` (Android 12+ `previewLayout`, sample values, localized).

## Design system ("Dusk")

Generated with the `ui-ux-pro-max` skill (it recommended "excitement purple + action orange",
vibrant & block-based, for a countdown/event app). Orange leads so the app stays distinct from
its siblings (Hijri converter = indigo, prayer-times-lb = emerald). Tokens in
[`src/theme/colors.ts`](./src/theme/colors.ts):

| Token | Light | Dark |
|---|---|---|
| primary (ember) | `#C2410C` (5.2:1 on white) | `#FF8A50` |
| secondary (violet) | `#6D28D9` | `#B794F6` |
| background | `#FFF8F4` | `#120B18` |
| hero gradient | `#3B0764 → #C2410C` | `#2E0752 → #B23A0C` |

`#F97316` (bright ember) is 2.8:1 on white — decorative only (icon, splash, dots). All text pairs
≥ 4.5:1. System fonts (proper Arabic shaping for free); ticking numbers use `tabular-nums` so a
changing digit never shifts its neighbours. 4/8 dp spacing, 48 dp touch targets, body ≥ 16.

**Per-countdown colors** ([`countdownColors.ts`](./src/theme/countdownColors.ts)): nine palettes
(ember, violet, ocean, teal, rose, forest, indigo, gold, graphite), each a dark→mid gradient whose
both ends carry white text at ≥ 4.5:1 (the hero card and the widget), plus light/dark accents and
soft badge backgrounds. The widget draws the same gradients natively — keep `WidgetColors.kt` in sync.

**App icon**: original — a gold countdown ring three-quarters done (with a white knob) around a
white calendar page (ember header, one day marked), on the dusk tile (`#3B0764 → #C2410C`).
Geometry lives in **two places that must stay in sync**: [`AppLogo.tsx`](./src/components/brand/AppLogo.tsx)
and [`scripts/generate-icons.js`](./scripts/generate-icons.js) (writes `assets/brand/*.svg` + every
Android/iOS icon; the Android 13 themed icon draws the page as an outline). Native splash is
regenerated from `assets/brand/splash-logo.svg` (command in help.md).

## Known gotchas (inherited from the Hijri converter — all still apply)

- **RN CLI init bugs**: Kotlin sources landed in `java/com/com.devlance.dayscounter/` (moved to
  `java/com/devlance/dayscounter/`) and `package.json` "name" was the package id (now `days-counter`).
- RN 0.87 removed `StatusBar` `backgroundColor`/`translucent` (edge-to-edge is the default) and
  `StyleSheet.absoluteFillObject` (use `StyleSheet.absoluteFill`).
- **Never wrap `Pressable` with `Animated.createAnimatedComponent`** — it silently stops receiving
  touches on RN 0.87 + Reanimated 4 + Fabric. Use `PressableScale` (plain Pressable, animated child).
- Android draws by **elevation before zIndex** — an overlapping element needs a higher `elevation`.
- `MainActivity.onCreate` uses `RNScreensFragmentFactory` (+ `RNBootSplash.init` once the splash
  exists). Don't drop either.
- **Windows 260-char path limit** in the New-Architecture codegen C++ build: `android/app/build.gradle`
  pins `externalNativeBuild.cmake.version "3.31.6"` (Ninja 1.12, long-path aware). Don't remove it.
- `.gitattributes` forces LF on `android/gradlew` and `patches/*.patch` (`core.autocrlf=true` here).
- **Never size an `<Svg>` background with `width="100%"`** — on Android it doesn't follow its
  parent as content grows. Use `GradientFill` (measures with onLayout).
- **Don't toggle `backgroundColor` on a rounded view inside a ripple `Pressable`** for a selected
  state (rendered square on Fabric) — keep a painted layer and toggle its `opacity`.
- **No same-direction VirtualizedList inside a ScrollView** (the wheel picker is a plain ScrollView).
- **patch-package** (`patches/`, applied by `postinstall`), same two patches as the Hijri app:
  `react-native+0.87.1.patch` (adds `./src/private/*` to RN's `exports`, silences a Metro warning)
  and `react-native-gesture-handler+3.2.1.patch` (lazy `DrawerLayoutAndroid`, silences a
  deprecation warning on every launch). Re-check both when upgrading RN or gesture-handler.
- RN 0.87's LogBox only says "Open debugger to view warnings" — read them with
  `adb logcat -s ReactNativeJS:V`.
- On Windows Metro can hang after `yarn add` rewrites `node_modules` — `yarn start --reset-cache`.
- On Windows, run Gradle directly (`cd android && ./gradlew …`), not `yarn android`. See help.md.

### Found on the first emulator run (Pixel 8a, API 36, 2026-10-23)

- **RN `Modal` + edge-to-edge (RN 0.87) lays its content out short**: the Modal's content root
  got the screen height minus the status and navigation bars (2216 of 2400 px) but was drawn from
  y = 0. A sheet anchored to its bottom overflowed it, so the lower half of Save/Cancel was drawn
  but not touchable — taps there fell through and closed the sheet. (uiautomator showed the
  sheet's bounds ending at 2216.) Fixed by dropping `Modal` for `BottomSheet` + `SheetHost`
  (above). The Hijri converter's `DatePickerSheet` uses the same Modal props — likely affected too.
- The hero's "days to go" label sat beside the big number and, in Arabic, wrapped and lost a word
  ("متبقٍّ"). It now sits under the number.
- Widget: Android formats `ar` numbers with Latin digits — now mapped by hand (see the widget
  section). The Chronometer's digits still follow the phone (no API to change them).
- Verified working: loader → Home, live hero + tiles, stats, add (wheels, name, color), edit
  (icon), delete (confirm, pin hand-over), pin from a row, Settings in EN/AR and light/dark,
  "Add the widget" (launcher pin dialog with the preview), widget large → wide → square live while
  resizing, widget re-colored/re-titled on pin change, Arabic widget plurals.
- Tooling: with `adb shell …` in Git Bash, set `MSYS_NO_PATHCONV=1` or `/sdcard/…` paths get
  rewritten. An emulator stuck as `unauthorized` after a snapshot boot came back with a cold boot
  (`emulator -avd Pixel_8a -no-snapshot-load`).

## Commands

```
yarn install            # deps (Yarn 1 only)
yarn start              # Metro
yarn android            # build + install + run (or Gradle directly, see help.md)
yarn test               # Jest
yarn typecheck          # tsc --noEmit
yarn lint               # eslint
yarn bump [patch|minor|major] [--dry-run]   # version bump (package.json + build.gradle + pbxproj)
yarn release [--dry-run]                    # GitHub release for the current version's tag (no APKs)
```

## Conventions to keep following

- Yarn, not npm. Relative imports.
- Every new direction-sensitive layout mirrors by hand via `useDirection()`.
- New user-facing strings go in **both** `src/i18n/locales/en.json` and `ar.json`.
- Colors only from theme tokens; icons only from lucide; touch targets ≥ 48 dp.
- Keep `TODO.md` checkboxes current; keep this file current when a convention changes.
- Every commit bumps the version, gets a tag + release, and is pushed (see the top section).
