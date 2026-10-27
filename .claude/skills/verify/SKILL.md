---
name: verify
description: This project's verification policy — read before running unit tests, a Gradle build, an emulator check or the iOS CI job. Do not run any of these proactively; only when the user explicitly asks to test/verify/build, or when the global per-feature release cycle for D:\workspace\me projects calls for it.
---

# Verification policy for Days Counter

**Do not run tests, builds or device checks unless the user explicitly asks you to test, verify
or build** — or the global "one release per feature" cycle for `D:\workspace\me` projects applies
(each feature tested, committed, tagged), which counts as that request. This is the user's standing
preference across all their projects (it has cost real budget before).

Two native apps: `android/` (Kotlin + Compose) and `ios/` (Swift + SwiftUI). A change to shared
behaviour is made — and verified — in both.

## Android (`android/`)

- **Countdown engine / phrases / formats / stores / migration / widget payload** (non-UI logic):
  `yarn test` in `android/` — the JUnit suites, which run in `Asia/Beirut` (set by the tests) so the
  DST cases cross a real change. ~20 s with a warm Gradle daemon. No emulator needed.
- **UI / visual changes**: `./gradlew assembleDebug` in `android/`, install on this project's
  emulator (**Pixel_8a, `emulator-5554`** — other sessions use other ports; always pass
  `adb -s emulator-5554`), and read texts with `adb shell uiautomator dump` + `grep text=` before
  reaching for screenshots; at most one screenshot after all edits. Check Arabic and dark only if
  the change touches them. In Git Bash prefix adb commands with `MSYS_NO_PATHCONV=1`.
- **Widget**: `adb shell dumpsys appwidget`, `dumpsys alarm | grep -A3 dayscounter`, and (debug
  builds) `adb shell run-as com.devlance.dayscounter cat shared_prefs/days_widget.xml`. Layout
  changes need a look on the home screen — the size map can't be checked any other way.

## iOS (`ios/`)

This machine is Windows: there is no Xcode, so iOS is built and tested on GitHub's macOS runners
(the `ios` job in `.github/workflows/ci.yml`; the repo is public, so minutes are free, but a run
takes several minutes). One run covers:

- `swift test` in `ios/DaysCounterCore` — the engine's and app logic's XCTest suites (the same
  cases as the JUnit ones).
- XcodeGen + `xcodebuild test`: the app (and, later, the widget extension) built for the simulator,
  then the UI smoke test, which saves screenshots to the `ios-screenshots` artifact
  (`gh run download <run> -n ios-screenshots`) — look at those instead of guessing.
- The shared-data check: the i18n JSON must be identical in both apps.

Push new Swift code to a `ci/<topic>` scratch branch first, read errors with
`gh run view <run> --log-failed`, fix them all in one round, and commit the finished change on
`develop` only once CI is green.

## Content / i18n-only changes

Edit both language files of both apps (identical copies), re-read them and confirm the key matches
its call sites. The locale-parity tests (both apps) guard missing keys.
