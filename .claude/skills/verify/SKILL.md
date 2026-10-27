---
name: verify
description: This project's verification policy — read before running yarn test:native, a Gradle build or an emulator check (and v1's yarn test / typecheck / lint). Do not run any of these proactively; only when the user explicitly asks to test/verify/build, or when the global per-feature release cycle for D:\workspace\me projects calls for it.
---

# Verification policy for Days Counter

**Do not run tests, builds or emulator checks unless the user explicitly asks you to test, verify
or build** — or the global "one release per feature" cycle for `D:\workspace\me` projects applies
(each feature tested, committed, tagged), which counts as that request. This is the user's standing
preference across all their projects (it has cost real budget before).

The shipping app is the native one in `native/` (v2, Kotlin + Compose). The React Native app in
`src/`, `android/`, `ios/` is v1, kept until it's removed.

## When the user does ask (v2), match the check to the change

- **Countdown engine / phrases / formats** (`native/app/src/main/java/com/devlance/dayscounter/core/`,
  `i18n/`): `yarn test:native` is the right and only check. The suites run in `Asia/Beirut` (set
  by the tests themselves), so the DST cases actually cross a daylight-saving change. ~20 s with a
  warm Gradle daemon. No emulator needed.
- **Data / logic outside the UI** (stores, JSON, v1 migration, widget payload): `yarn test:native`.
- **UI / visual changes** (a screen, a sheet, styling, animation): build the debug APK, install it
  on this project's emulator and look — `cd native && ./gradlew assembleDebug`, then
  `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`. Read texts with
  `adb -s emulator-5554 shell uiautomator dump` + `grep text=` instead of screenshots where you
  can; take **at most one** screenshot after all edits are done. Check Arabic and dark only if the
  change touches them. In Git Bash prefix adb commands with `MSYS_NO_PATHCONV=1`.
- **Widget**: `adb shell dumpsys appwidget`, `dumpsys alarm | grep -A3 dayscounter`, and (debug
  builds) `adb shell run-as com.devlance.dayscounter cat shared_prefs/days_widget.xml`. Layout
  changes need a look on the home screen — the size map can't be checked any other way.
- **i18n-only changes** (`native/app/src/main/assets/i18n/*.json`): re-read the edited file and
  confirm the key matches its `strings["..."]` / `strings.t("...")` call site. The locale-parity
  test guards missing keys.

## Emulator etiquette (other Claude sessions share this machine)

This project uses AVD **Pixel_8a on port 5554** only (`emulator -avd Pixel_8a -port 5554`), and
always `adb -s emulator-5554`. Other projects' sessions use other AVDs/ports (the Hijri converter:
Pixel_7 on 5560). Don't restart the adb server without telling the other sessions first.

## v1 (until it's removed)

`yarn test` (Jest, Asia/Beirut), `yarn typecheck`, `yarn lint`; native v1 build:
`cd android && ./gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a,x86_64`.
