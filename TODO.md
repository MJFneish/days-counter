# TODO — Days Counter

Phased build checklist. Check items off as they land; keep this file honest — it's the fastest way
for a fresh Claude session (or the human) to see what's real vs planned. If it disagrees with
`CONTINUE_PROJECT.md`'s prose, trust this file and the actual code. Each phase ships as its own
version (tag + GitHub release, pushed to `develop`).

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

## Phase 3 — App shell, brand, loader (v0.3.0)
- [ ] Theme "Dusk" light/dark/system, spacing/typography tokens, i18n EN/AR, `useDirection`
- [ ] Root stack + live-drag tab pager (Home / Settings, mirrors in Arabic)
- [ ] Original app icon (Android legacy + adaptive + monochrome, iOS 1024) via `scripts/generate-icons.js`
- [ ] Native splash (bootsplash) → animated JS loading screen (seamless hand-off, min 1.2 s,
      reduced-motion aware)

## Phase 4 — Home (v0.4.0)
- [ ] Hero card for the pinned countdown: live days / hours / minutes / seconds tiles, target
      date, progress bar, breakdown line, "it's today" / "since" states
- [ ] List of the other countdowns (days left, mini progress), tap to open, pin from the list
- [ ] Add / edit sheet: title, date wheels, time wheels, color, icon, pin toggle; delete with confirm
- [ ] Empty state

## Phase 5 — Settings (v0.5.0)
- [ ] Language, appearance (follow system + light/dark tiles)
- [ ] Countdown options (show seconds, count up after the date)
- [ ] Widget section (which countdown is pinned, "Add widget to home screen")
- [ ] About (logo, version, description), reset data with confirm

## Phase 6 — Android widget (v0.6.0)
- [ ] TurboModule `DaysWidget` (codegen spec in `src/native/`), pushes the pinned countdown
- [ ] `AppWidgetProvider` with live `Chronometer` H:M:S + days, alarm at each day boundary,
      size map (small / wide / large) so it resizes live, RTL + Arabic plurals natively
- [ ] Declared for `home_screen|keyguard` (lock screen where the device allows it)
- [ ] Pin-widget request from the app (`requestPinAppWidget`)

## Phase 7 — Full test + 1.0.0
- [ ] `yarn test`, `yarn typecheck`, `yarn lint`
- [ ] Release build, emulator run (Home, add/edit/delete, Settings EN/AR light/dark, widget
      placed + resized), fixes
- [ ] 1.0.0 release with the APK attached

## Ideas / not built (ask the user before building)
- Lock-screen live notification (offered, user chose widget only)
- Per-widget countdown choice (offered, user chose one pinned countdown)
- Reminders (e.g. "30 days left"), repeating yearly countdowns (birthdays), share as image
- iOS widget (WidgetKit) — needs macOS
