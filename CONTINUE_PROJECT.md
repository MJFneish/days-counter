# Continue this project

If you're a Claude session picking this up cold, read this first, then `CLAUDE.md`, then
`TODO.md`. This file is a snapshot — trust `TODO.md`'s checkboxes and the actual code over this
file's prose if they disagree.

## What this is

**Days Counter / عدّاد الأيام** — a bare React Native CLI (TypeScript) app that counts down to
dates (days, hours, minutes, seconds, live), with a resizable Android home-screen widget for the
pinned countdown. EN/AR (RTL), light/dark, offline. Built on 2026-10-23 from an empty repo
(`MJFneish/days-counter`, branch `develop`) using the Hijri converter as the template.

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
| APK on GitHub | Final release only |
| Testing | A **full test at the end** (tests, typecheck, lint, build, emulator) — explicitly asked |

## State as of this handoff

- v0.1.0: scaffold, configs, patches, bump + release scripts, docs. See `TODO.md` for the rest.

## Next steps a fresh session might be asked for

1. Continue the phases in `TODO.md`.
2. Ideas parked at the bottom of `TODO.md` — ask first.
