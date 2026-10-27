# Continue this project

If you're a Claude session picking this up cold, read this first, then `CLAUDE.md`, then
`TODO.md`. This file is a snapshot — trust `TODO.md`'s checkboxes and the actual code over this
file's prose if they disagree.

## What this is

**Days Counter / عدّاد الأيام** — counts down to dates (days, hours, minutes, seconds, live), with
a resizable Android home-screen widget for the pinned countdown. EN/AR (RTL), light/dark, offline.
Built on 2026-10-23 as a bare React Native CLI app (v1, `MJFneish/days-counter`, branch
`develop`); **since 2026-10-27 being converted to a native Kotlin + Jetpack Compose app in
`native/` (v2)**, the same way as Mawakit (prayer-times-lb). v1 stays in the repo until removed.

## Decisions the user made (don't re-litigate)

| Question | Answer |
|---|---|
| Framework | Bare RN CLI, "like the last ones" (Gregorian-hijri-date-converter, prayer-times-lb) — same configs, md files, skills |
| Pages | Two: Home + Settings (language, theme, about + app settings), plus a loader at launch |
| The countdown | Days + hours + minutes + seconds to a date; their example is **5 Sep 2027** (pre-added) |
| How many countdowns | **Several, one pinned** — the pinned one drives the widget (not per-widget choice) |
| Widget | Resizable home-screen widget; **lock screen: widget only** (declined a live notification). Declared `keyguard` too, for devices that allow lock-screen widgets |
| Icon, primary/secondary colors | Left to Claude |
| Git | **Every commit bumps the version, gets a `v<version>` tag + GitHub release, and is pushed to `develop`** |
| Versions | 0.1.0 scaffold → minor per feature → **1.0.0** when finished and fully tested |
| APK on GitHub | **None** — first "final release only", then "do not upload the apk to github"; the 1.0.0 one was removed |
| Testing | A **full test at the end** (tests, typecheck, lint, build, emulator) — explicitly asked |
| v2 (2026-10-27) | "Convert this app like the prayer time app (Mawakit) to a native application, not React Native CLI"; **start at v2, commit every feature, push it with a tag**; "test carefully"; coordinate with the other Claude session testing another app (the Hijri converter, also going native) |
| v2 versions | 2.0.0-alpha.N per phase (GitHub pre-releases) → **2.0.0** after the full test; build numbers from 100 |

## State as of this handoff

**v2 (native):**
- 2.0.0-alpha.1: `native/` skeleton (Mawakit's toolchain), same id + key + launcher activity as
  v1 — installed over v1 1.0.1 on the emulator, v1's data (`RKStorage`) and widget prefs survive
  for the migration; splash; version source `native/version.properties`; scripts
  (`test:native`, `build:apk`, `bump pre|release`, release reads the new file).
- Next: engine port + tests (alpha.2), then data/shell/Settings, Home, editor, widget, 2.0.0.

**v1 (React Native):**

- v0.1.0: scaffold, configs, patches, bump + release scripts, docs.
- v0.2.0: countdown engine (calendar-day, DST-safe) + 19 Jest tests, stores, EN/AR strings.
- v0.3.0: Dusk theme, tab pager, icon, native splash → animated loader, shared clock, first Settings.
- v0.4.0: Home — live hero (pinned), stats, list, add/edit sheet with wheels, colors, icons.
- v0.5.0: Android widget — Turbo Module bridge, native engine port, live Chronometer, 4 resizable layouts, lock-screen capable.
- v0.6.0: Settings — countdown options, widget section (picker, add widget), delete all.
- v1.0.0: full test (27 tests, typecheck, lint, release build, emulator walk-through) and its
  fixes — sheets off RN Modal, hero label, widget digits.
- v1.0.1: release-script fix only; the app is the same as 1.0.0.
- v1.0.2: docs + script — no APKs on GitHub (the 1.0.0 asset was deleted, the upload option
  removed). Local APK: `android/app/build/outputs/apk/release/DaysCounter-<version>.apk`.
- **Not verified**: a physical phone, lock-screen placement (needs a device that allows it), the
  exact-alarm path on Android 13+ with "Alarms & reminders" granted, iOS (no macOS here).
- See `TODO.md` for the rest.

## Next steps a fresh session might be asked for

1. Continue the phases in `TODO.md`.
2. Ideas parked at the bottom of `TODO.md` — ask first.
