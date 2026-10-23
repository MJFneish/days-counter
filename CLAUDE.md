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
releases are marked pre-release. **Only the final release carries the APK** (user's choice) —
`yarn release --asset android/app/build/outputs/apk/release/DaysCounter-<version>.apk`.

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

## Commands

```
yarn install            # deps (Yarn 1 only)
yarn start              # Metro
yarn android            # build + install + run (or Gradle directly, see help.md)
yarn test               # Jest
yarn typecheck          # tsc --noEmit
yarn lint               # eslint
yarn bump [patch|minor|major] [--dry-run]   # version bump (package.json + build.gradle + pbxproj)
yarn release [--asset <apk>] [--dry-run]    # GitHub release for the current version's tag
```

## Conventions to keep following

- Yarn, not npm. Relative imports.
- Every new direction-sensitive layout mirrors by hand via `useDirection()`.
- New user-facing strings go in **both** `src/i18n/locales/en.json` and `ar.json`.
- Colors only from theme tokens; icons only from lucide; touch targets ≥ 48 dp.
- Keep `TODO.md` checkboxes current; keep this file current when a convention changes.
- Every commit bumps the version, gets a tag + release, and is pushed (see the top section).
