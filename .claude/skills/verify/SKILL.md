---
name: verify
description: This project's verification policy — read before running yarn test, yarn typecheck, yarn lint, a Gradle build or an emulator run. Do not run any of these proactively; only when the user explicitly asks to test/verify/build.
---

# Verification policy for Days Counter

**Do not run `yarn test`, `yarn typecheck`, `yarn lint`, Gradle builds or emulator runs unless the
user explicitly asks you to test, verify or build.** This is the user's standing preference across
all their projects (it has cost real budget before). Make the change, explain what you did and why
you're confident in it, and stop. Wait for an explicit "test it" / "verify this" / "build it".

## When the user does ask, match the check to the change

- **Countdown engine** (`src/core/**`): `yarn test` is the right and only check (a few seconds).
  Jest runs in `Asia/Beirut` (`jest.globalSetup.js`) so the DST cases in `countdown.test.ts`
  actually cross a daylight-saving change. No build needed.
- **Type-only / non-visual changes** (a type, a util, a store field): `yarn typecheck` alone.
- **i18n string changes**: re-read the edited JSON and confirm the key matches its `t('...')` call
  site, in both `en.json` and `ar.json`. Nothing to run.
- **Native changes** (`android/**` — the widget, the TurboModule, bootsplash/icon regeneration, a
  new native dependency): one Gradle build —
  `cd android && ./gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a,x86_64` (release
  also runs the Metro bundle + Hermes compile, catching broken imports). Not via `yarn android` on
  Windows (see help.md). Widget layout changes need a look on a device — the size map can't be
  checked any other way.
- **UI/visual changes**: the only case that needs a device/emulator. Take at most one screenshot
  after all edits are done — never after every intermediate edit. Check both themes and Arabic
  only if the change touches them.

## A "full test" (asked for at the end of the first build, 2026-10-23)

`yarn test` → `yarn typecheck` → `yarn lint` → release build (`arm64-v8a,x86_64`) → install on the
running emulator → walk Home, add/edit/delete a countdown, pin, Settings (EN/AR, light/dark),
place and resize the widget. A handful of screenshots in total, not one per step.
