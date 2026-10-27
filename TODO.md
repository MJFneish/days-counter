# TODO — Days Counter

Phased build checklist. Check items off as they land; keep this file honest — it's the fastest way
for a fresh Claude session (or the human) to see what's real vs planned. If it disagrees with
`CONTINUE_PROJECT.md`'s prose, trust this file and the actual code. Each phase ships as its own
version (tag + GitHub release, pushed to `develop`).

# v2 — native Android app (Kotlin + Jetpack Compose, `native/`)

Asked for on 2026-10-27: convert the app to a native Android app "like Mawakit" (prayer-times-lb's
v2), starting at v2, one commit + tag per feature, tested carefully. Each phase below ships as
`2.0.0-alpha.N` (tag + GitHub pre-release, pushed to `develop`); **2.0.0** = the full test pass.
v1 (the React Native app below) stays in the repo until the user asks to remove it.

## v2 Phase 1 — Native skeleton (2.0.0-alpha.1) ✅
- [x] `native/`: Gradle 9.3.1 / AGP 8.12 / Kotlin 2.2.10 / Compose BOM 2025.08 (Mawakit's set),
      minSdk 24, target 36
- [x] Same application id, signing key (RN template `debug.keystore`) and launcher activity name
      as v1, so v2 installs over it as an update — checked on the emulator (1.0.1 → alpha.1)
- [x] Launcher icons from v1, Android 12 SplashScreen with v1's bootsplash emblem on the night tile
- [x] Version source moved to `native/version.properties` (build numbers from 100); `yarn bump`
      gains `pre`/`release`; `yarn release` reads it and marks `-alpha` as pre-release;
      `yarn test:native`, `yarn build:apk`

## v2 Phase 2 — Countdown engine in Kotlin (2.0.0-alpha.2)
- [ ] `core/`: `remaining` (calendar days, DST-safe, JS-Date semantics incl. the repeated hour),
      progress, calendar span, phrases (EN + Arabic 6 forms), date/time formats, titles
- [ ] `i18n/Strings` over v1's i18next JSON (assets), `{{placeholders}}`
- [ ] JUnit port of every Jest test, in Asia/Beirut, + locale key parity

## v2 Phase 3 — Data, theme, shell, Settings (2.0.0-alpha.3)
- [ ] Stores (countdowns + settings) over SharedPreferences, sanitized; seed 5 Sep 2027 once
- [ ] First-launch import of v1's AsyncStorage (`RKStorage` SQLite) — countdowns, pin, settings
- [ ] Dusk theme (light/dark/system), instant RTL via `LocalLayoutDirection`, Lucide icons as
      vectors, the emblem in Compose, animated loading screen, one shared clock
- [ ] Tabs pager (Home / Settings) mirrored in Arabic; Settings: language, appearance, countdown
      options, delete all, about

## v2 Phase 4 — Home (2.0.0-alpha.4)
- [ ] Hero (live D/H/M/S, digits slide in, progress, arrived / count-up), stats card, list rows
      (headline number, progress, pin), empty state, toasts

## v2 Phase 5 — Editor + countdown picker (2.0.0-alpha.5)
- [ ] Bottom sheet: live preview, name (40), date/time wheels (12/24 h), 9 colors, 12 icons,
      show-on-widget, delete with confirm; past-date note
- [ ] Settings' countdown picker sheet

## v2 Phase 6 — Widget (2.0.0-alpha.6)
- [ ] Provider ported (same class name + prefs, so placed widgets survive the update); payload
      built in Kotlin, pushed on every change and foreground; Settings widget section

## v2 — Full test + 2.0.0
- [ ] Unit tests, release build, emulator walk-through EN/AR light/dark, widget resize, update
      over v1 with real data

# v1 — React Native app (kept until removed)

## Phase 1 — Scaffold (v0.1.0) ✅
- [x] Bare RN CLI 0.87.1 TS project, package `com.devlance.dayscounter`, display name
      "Days Counter" / launcher label "عدّاد الأيام" on Arabic phones (`values-ar/strings.xml`)
- [x] Fixed CLI scaffold bugs (malformed Kotlin dir, package.json name)
- [x] Deps pinned to the Hijri converter's proven native versions; worklets Babel plugin;
      gesture-handler first in `index.js`; the same two patch-package patches
- [x] CMake 3.31.6 pin (Windows long paths), release APK named `DaysCounter-<version>.apk`
- [x] `scripts/bump-version.js` (`yarn bump`), `scripts/github-release.js` (`yarn release`)
- [x] Docs: CLAUDE.md, CONTINUE_PROJECT.md, TODO.md, help.md, README.md, `.claude/skills/verify`

## Phase 2 — Countdown engine + stores (v0.2.0) ✅ (19/19 tests)
- [x] `src/core/countdown.ts`: calendar-aware remaining time (days flip at the target's wall-clock
      time, also across DST), H/M/S remainder, progress, upcoming/arrived/past (count up)
- [x] Breakdown phrases (months + days, weeks, total hours) with Arabic plurals; date/time formats
- [x] Countdown store (zustand + AsyncStorage): list, pinned id, add/edit/delete, seeded with
      5 Sep 2027; settings store (language, theme, show seconds, count up, 24-hour clock)
- [x] i18n EN/AR strings for the whole app
- [x] Jest tests in `Asia/Beirut` (DST crossings, every midnight flip, leap day, past dates)

## Phase 3 — App shell, brand, loader (v0.3.0) ✅ (typecheck clean)
- [x] Theme "Dusk" light/dark/system (ui-ux-pro-max), per-countdown palettes, spacing/typography
      tokens, `useDirection`, one shared clock (`useClock`)
- [x] Root stack + live-drag tab pager (Home / Settings, mirrors in Arabic)
- [x] Original app icon (Android legacy + adaptive + monochrome, iOS 1024) via `scripts/generate-icons.js`
- [x] Native splash (bootsplash, Android + iOS) → animated JS loading screen (seamless hand-off,
      min 1.2 s, reduced-motion aware)
- [x] Settings: language, appearance, about (the rest lands in Phase 5); Home placeholder

## Phase 4 — Home (v0.4.0) ✅ (typecheck clean)
- [x] Hero card for the pinned countdown: live days / hours / minutes / seconds tiles, target
      date, progress bar, "The day is here!" / count-up states
- [x] "The wait in numbers" card (calendar span, weeks, total hours)
- [x] List of the other countdowns (headline number, mini progress), tap to edit, pin from the list
- [x] Add / edit sheet: live preview, name, date wheels, time wheels, color, icon, pin toggle;
      delete with confirm; toasts
- [x] Empty state

## Phase 5 — Android widget (v0.5.0) ✅ (release build passes)
- [x] Turbo Native Module `DaysWidget` (codegen spec in `src/native/`); `useWidgetSync` pushes
      the pinned countdown + language + count-up on change and on every foreground
- [x] `DaysWidgetProvider`: `Countdown.kt` port of the engine, day count + live `Chronometer`
      H:MM:SS, alarms at each day change and one hour before it ("0:MM:SS" format), progress
- [x] Four layouts (small / square / wide / large) as an Android 12+ size map, so it resizes
      live; size thresholds for older Android; per-color gradient backgrounds; RTL; Arabic plurals
- [x] Declared for `home_screen|keyguard` (lock screen where the device allows it), picker preview
- [x] Pin-widget request from the app (`requestPinAppWidget`)

## Phase 6 — Settings (v0.6.0) ✅ (typecheck clean)
- [x] Language, appearance (follow system + light/dark tiles), about (since v0.3.0)
- [x] Countdown options (show seconds, count up after the date, 24-hour time)
- [x] Widget section (which countdown is pinned + picker, "Add the widget", lock-screen note)
- [x] Delete all countdowns with confirm

## Phase 7 — Full test + 1.0.0 ✅
- [x] `yarn test` (27/27), `yarn typecheck` (clean), `yarn lint` (0 errors, style warnings only)
- [x] Release build (arm64-v8a + x86_64), emulator run on a Pixel 8a (API 36): loader, Home, add /
      edit / delete, pin, Settings EN/AR light/dark, widget placed via the in-app button and
      resized large → wide → square live
- [x] Fixes from that run: sheets moved off RN `Modal` (bottom of Save/Cancel wasn't touchable),
      hero label under the number (Arabic wrap), Arabic-Indic digits on the widget
- [x] 1.0.0 release; universal APK (all ABIs, ~78 MB) built locally
- [x] 1.0.1: release-script upload fix; app unchanged
- [x] 1.0.2: no APKs on GitHub (user's call) — the 1.0.0 one removed, upload option dropped
- [ ] Physical phone run (lock screen widget on a device that supports it, exact alarms on 13+)
- [ ] iOS build (needs macOS; no WidgetKit widget — Android only)

## Ideas / not built (ask the user before building)
- Lock-screen live notification (offered, user chose widget only)
- Per-widget countdown choice (offered, user chose one pinned countdown)
- Reminders (e.g. "30 days left"), repeating yearly countdowns (birthdays), share as image
- iOS widget (WidgetKit) — needs macOS
