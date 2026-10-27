# Days Counter (عدّاد الأيام) — countdowns + a resizable home-screen widget

Counts down to the dates that matter — days, hours, minutes and seconds, ticking live — with a
**resizable home-screen widget** for the pinned countdown. Bilingual (English / Arabic with RTL),
light and dark themes, fully offline. Two tabs (Home + Settings) and an animated loading screen.

**Two native apps, one folder each** (the same layout as Mawakit / prayer-times-lb):
[`android/`](./android/) — Kotlin + Jetpack Compose — and [`ios/`](./ios/) — Swift + SwiftUI.
**Every feature is built on both** (the user's standing rule for `D:\workspace\me` projects with
both folders, and their answer for this conversion on 2026-10-27). v1, a bare React Native CLI app,
was removed in 2.0.0-alpha.2; it is in git history (tag `v1.0.2`).

See [`CONTINUE_PROJECT.md`](./CONTINUE_PROJECT.md) for a handoff snapshot if you're picking this up
fresh, [`TODO.md`](./TODO.md) for the phased checklist (source of truth for "what's done"), and
[`help.md`](./help.md) for the command cheat sheet.

**Project skills** (`.claude/skills/`) — check these before the matching kind of work:
- `verify` — **read before running unit tests, a Gradle build, an emulator check or the iOS CI job.**

## The v2 conversion (2026-10-27)

The user asked to convert the app "like Mawakit" to native apps, starting at v2, one commit + tag
per feature, tested carefully. Each phase ships as **2.0.0-alpha.N** on both platforms; **2.0.0**
is the full test pass. The rules in "Countdown engine", "UI", "Widget" and "Design system" below
are v1's behaviour, which both apps reproduce — `TODO.md` says which phases have landed. File
names in those sections are v1's (TypeScript, in git history) unless a Kotlin/Swift path is given.

## Android app (`android/`) — read first

- **Toolchain** = Mawakit's, proven on this machine: Gradle 9.3.1 wrapper, AGP 8.12.0, Kotlin
  2.2.10, Compose BOM 2025.08.00, compileSdk/targetSdk 36, minSdk 24, JDK 17 target. No
  `java.time` (needs API 26 or desugaring, whose library isn't in the offline Gradle cache) —
  dates go through `java.util.Calendar`.
- **Updating over v1 must keep working:** same application id (`com.devlance.dayscounter`), same
  signing key (`android/app/debug.keystore` = v1's RN template key, the public Android debug key),
  same launcher activity (`.MainActivity`). Checked on the emulator: 1.0.1 → 2.0.0-alpha.1 installs
  as an update, and v1's AsyncStorage database (`databases/RKStorage`, table
  `catalystLocalStorage`, keys `days-counter-countdowns` / `days-counter-settings`, values
  `{"state": {...}, "version": 0}`) and the widget's `shared_prefs/days_widget.xml` survive.
- **Splash:** AndroidX SplashScreen (`Theme.App.SplashScreen`) shows v1's bootsplash emblem
  (`drawable-*/splash_emblem.png`: 288 dp canvas, 120 dp emblem) on `#170B26`.
- **Strings** are v1's i18next files as assets (`android/app/src/main/assets/i18n/`), identical to
  the iOS copies (CI compares them).
- **Tooling** lives in `android/package.json` (Node built-ins, nothing to install, run in
  `android/`): `yarn test`, `yarn bump`, `yarn build:apk` (→ `android/dist/`), `yarn release`,
  `yarn build:icons` (needs `sharp`, see help.md).

## iOS app (`ios/`) — read first

- **No Mac on this machine.** The iOS app is compiled and tested on GitHub's macOS runners — the
  `ios` job in `.github/workflows/ci.yml`: `swift test` in `ios/DaysCounterCore`, then XcodeGen +
  `xcodebuild test` (app build + UI smoke test on the newest iPhone simulator), screenshots in the
  `ios-screenshots` artifact. The repo is public, so macOS minutes are free — but each run takes
  several minutes: fix every compile error from one log in one round.
- **Get iOS code compiling on a scratch branch** (`ci/<something>`, which CI also runs) — not on
  `develop`, where every commit is a version. Once green, fast-forward `develop` to it, tag, push,
  and delete the scratch branch.
- **Layout:** `project.yml` (XcodeGen; `DaysCounter.xcodeproj` and the Info.plists are generated,
  not committed), `DaysCounterCore/` (Swift package: engine + app logic + their XCTest suites, and
  the i18n JSON in `Resources/`), `DaysCounter/` (the SwiftUI app), `DaysCounterUITests/` (the UI
  smoke test that saves screenshots). Bundle id `com.devlance.dayscounter`, iOS 17+, iPhone,
  portrait, Swift 5.9 language mode.
- **Versions:** `yarn bump` (in `android/`) writes `ios/project.yml` too — `MARKETING_VERSION` is the
  version without its `-alpha.N` suffix (iOS only accepts x.y.z), `CURRENT_PROJECT_VERSION` the
  shared build number.
- **Launch screen:** `UILaunchScreen` with `LaunchBackground` (`#170B26`) + `LaunchLogo` (the 120 pt
  emblem) — the same picture as Android's splash. App icon = v1's 1024 px icon.

## Every commit = a version (user rule, 2026-10-23)

The user asked for **each commit to carry its own app version, a GitHub tag and a release, and to
be pushed to `develop`** (the repo's only branch). The routine, in this order:

1. `yarn bump minor` (feature) / `yarn bump patch` (fix), in `android/` — in the **same** commit
   as the work. During the v2 conversion each phase is `yarn bump pre` (2.0.0-alpha.N), then
   `yarn bump release` for 2.0.0 once fully tested.
2. `git commit` → `git tag -a v<version> -m "<version>"` → `git push origin develop --follow-tags`.
3. `yarn release` (in `android/`) → GitHub release titled `<version>` (notes = the commit message;
   [`android/scripts/github-release.js`](./android/scripts/github-release.js), REST API with the token from
   `git credential fill` — `gh` isn't installed; never print the token).

Versions ran **0.1.0 (scaffold) → minor per feature → 1.0.0 = finished and fully tested**, then
v2 **2.0.0-alpha.1 → alpha per phase → 2.0.0** (same idea). 0.x and `-alpha` releases are marked
pre-release. **No APKs on GitHub** — the user said "do not upload the apk to
github" (2026-10-23); the one briefly attached to 1.0.0 was removed and the script has no upload
option. Build APKs locally (`yarn build:apk` in `android/`) and hand over the path.

## Countdown engine — read before touching either app's `core/`

Kotlin: `android/app/src/main/java/com/devlance/dayscounter/core/` (`Model`, `Time`, `Remaining`,
`Format`, `Plural`, `Phrases`) + `i18n/Strings.kt`. Swift: the same file names in
`ios/DaysCounterCore/Sources/DaysCounterCore/`. **A port of v1's `src/core/` + `src/i18n/plural.ts`;
the two apps run the same algorithms — change both together, with their tests.**

- A countdown stores its target as a **wall-clock `LocalDateTime`** (`{year, month, day, hour,
  minute}`, device time zone), never an epoch — "5 Sep 2027 00:00" stays local midnight even if
  the phone changes time zone.
- **No platform calendar in the engine.** Dates are day numbers (`epochDay` / `dateOfEpochDay`,
  Hinnant's civil-day algorithms) plus the zone's UTC offset; `wallToInstant` resolves a wall time
  from the offsets a day before and after it. A time a DST change **skips** moves forward by the
  gap (Beirut 28 Mar 2027 00:30 → 01:30); one it **repeats** takes the **first** occurrence — JS
  `Date`'s rules (what v1 used) and java.time's. `java.util.Calendar` and Foundation's `Calendar`
  disagree with each other on those cases, which is why neither is used.
- `remaining(target, now)` counts **calendar days anchored to the target's time of day**: `days` =
  the largest d with "target − d calendar days" ≥ now. A midnight countdown therefore flips its day
  count at local midnight, also across DST — counting 24-hour blocks would flip it at 23:00 or
  01:00 for half the year. The H/M/S part is the physical time to that next flip (`nextChangeAt`),
  so on a DST day it can read up to 24:59:59 / start at 22:59:59. `boundaryAt` is the last flip
  when counting up (the widget counts up from it).
- Seconds are **truncated**, like Android's `Chronometer`, so the app and the widget tick together.
- After the target: `ARRIVED` for the first day (the "The day is here!" state), then `PAST`, both
  counting up the same way. Settings → "Count up after the date" can hide the count-up.
- `calendarSpan` = the most whole months that fit (clamped to month ends: 31 Jan → 1 Mar is
  "1 month and 1 day"), then leftover days — in day numbers, free of DST.
- Counted phrases ("10 months and 12 days", "١٠ أشهر و١٢ يومًا") come from `Plural`, with Arabic's
  forms spelled out (one/two/few/many/other, 0 → "٠ يوم") rather than platform plural rules.
  `unitWord()` gives the bare noun for a label under a big number ("يومًا", "دقائق").
- Arabic UI shows Arabic-Indic digits via `localizeDigits()` (by hand — Android's `ar` locale
  formats with Latin digits); Gregorian month names are Levantine (كانون الثاني…), as Lebanese
  calendars print them.
- Texts: `Strings.of(lang)` over the shared i18next JSON (dot keys, `{{name}}` placeholders,
  English fallback) — the app's language is its own setting, independent of the phone's.
- Tests run in **Asia/Beirut** (JUnit: `TimeZone.setDefault`; XCTest: `TimeZones.override`):
  `CountdownTest` / `CountdownTests` (the user's example 23 Oct 2026 15:00 → 316 days 9:00:00 to
  5 Sep 2027, every midnight flip on the way, both 2026/27 DST changes, the skipped and repeated
  hour, leap day, arrived/past, spans, day-number round trip 1899–2101) and `PhrasesTest` /
  `PhrasesTests` (both languages' phrases, titles, relative phrases, Strings) — v1's Jest cases
  plus the DST semantics.

### Stores

- **Android** `data/AppStore` — `StateFlow`s of `Settings` and `CountdownsState`, saved as JSON to
  SharedPreferences `days_counter` (keys `settings`, `countdowns`) on every change, loaded
  synchronously in `DaysCounterApp.onCreate`. **iOS** `AppModel` (an `ObservableObject`) — the same
  JSON in the App Group's UserDefaults (`group.com.devlance.dayscounter`, keys `settings`,
  `countdowns`), so the widget can read it.
- `CountdownsState` (`data/State.kt`, `DaysCounterCore/State.swift`) — `countdowns`, `pinnedId`
  (the Home hero + the widget; always set while any countdown exists — removing the pinned one
  hands it to the next one due), `seeded` (the user's 5 Sep 2027 example is added once on first
  launch, with an empty title so it shows as its date; deleting everything doesn't revive it).
  Every change returns a new state; the rules are unit-tested on both platforms (`StoreTest` /
  `StoreTests`).
- `StoreJson` — the same shapes as v1's zustand stores, decoded with v1's sanitizing (bad dates,
  unknown colors/icons, a missing pin → defaults, never a failed launch).
- **v1 import (Android only — v1 never shipped on iOS):** on the first launch without v2 data,
  `data/LegacyMigration` reads v1's AsyncStorage (`databases/RKStorage`, table
  `catalystLocalStorage`, keys `days-counter-settings` / `days-counter-countdowns`, value
  `{"state": {...}}`) and hands the `state` objects to `StoreJson`. Checked on the emulator with an
  Arabic/dark/two-countdown v1 database.
- Defaults on a fresh install: the phone's language (Arabic if it's Arabic, else English) and
  12/24-hour clock; theme follows the system; seconds and count-up on.
- iOS UI-test hooks (launch environment): `DAYS_COUNTER_RESET=1`, `DAYS_COUNTER_LANGUAGE=en|ar`,
  `DAYS_COUNTER_THEME=system|light|dark`.

## UI architecture

- **Theme** (Android `ui/theme/Theme.kt`, iOS `Theme.swift`): the Dusk tokens below, the type
  scale, spacing, the per-countdown palettes. **RTL follows the app's language, not the phone's**,
  and flips instantly: Compose `LocalLayoutDirection`, SwiftUI `\.layoutDirection` (the `Themed`
  modifier on iOS). Light/dark: follow the system, or forced from Settings.
- **Icons**: Android draws v1's **Lucide** set — `ui/icons/LucideIcons.kt` is generated by
  `yarn build:lucide` (in `android/`) from lucide-static 1.60.0 (v1's version), drawn by
  `LucideIcon`; add a name in `scripts/build-lucide.js` and re-run. iOS uses **SF Symbols** (the
  platform's own set, as Mawakit does) — `CountdownIcon.symbol` maps the countdown icons.
- **Emblem in code**: `ui/brand/AppLogo.kt` / `Views/Brand/AppLogo.swift` draw the icon's emblem
  (tile, ring, gold arc, page) on a 100-unit grid — keep in sync with `generate-icons.js`.
- **Tabs**: Android — a `HorizontalPager` (Home, Settings) that follows the finger and snaps, with
  a custom tab bar (pill behind the active icon); **in Arabic the pages and the tab bar mirror
  together** (the layout direction flips both); Back on Settings returns to Home. iOS — the native
  `TabView` (hourglass / gearshape).
- **One clock** (Android `ui/Clock.kt` → `LocalNow`; iOS `Clock` in `AppModel.swift`): ticks on
  whole seconds — whole minutes when "Show seconds" is off — aligned to the wall clock, and stops
  in the background. Every countdown reads it, so they all tick together. Don't add per-view timers.
- **Home** (Android `ui/screens/HomeScreen.kt` + `ui/home/`, iOS `Views/HomeView.swift` +
  `HomeParts.swift`): header (today's date + round "+" button) → the hero for the **pinned**
  countdown (its gradient, big day count, H/M/S tiles whose digits roll on change, progress of the
  wait since the countdown was created, "The day is here!" on the day) → "The wait in numbers"
  (on the calendar / in weeks / in hours) → every other countdown as a row (headline number drops
  to hours/minutes on the last day; pin button moves it to the hero + widget), upcoming soonest
  first, then passed ones. Empty state when nothing is left. A countdown with no name shows its
  date as the title. On iOS the hero and each row are single accessibility elements
  ("<title>. in 312 days") — the UI test matches them by that label.
- **Editor** (Android `ui/editor/`, iOS `Views/EditorView.swift`): one sheet for add + edit — live
  preview in the chosen gradient, name (optional, 40 chars), date wheels (1900–2100, day clamped to
  the month; day : month : year columns 0.8 : 1.6 : 1, mirrored in Arabic) and time wheels (12/24 h
  per Settings), 9 colors, 12 icons, "show on the widget" (hidden when it's already pinned), delete
  (confirm). A new countdown is `CountdownDraft.new`: one month ahead at 00:00 (the day clamped to
  that month), the first unused color, pinned if none is. A past date is allowed (note shown) — it
  counts up. Android's wheels are snapping LazyColumns with a selection band and fades; iOS uses
  native wheel `Picker`s (with the UIPickerView intrinsic-width fix so side-by-side wheels don't
  steal each other's touches).
- **Sheets**: Android draws them inside the activity window (`ui/Sheets.kt` `BottomSheet`: scrim,
  slide, Back and a scrim tap close it, `imePadding` keeps Save/Cancel above the keyboard), mounted
  once in `AppRoot` and opened through `Sheets.openEditor(id | null)` / `Sheets.picker` — never a
  dialog/popup (v1's RN Modal left the bottom of Save/Cancel untouchable). iOS uses the native
  `.sheet(item:)` from `AppModel.editor`, re-applying `Themed` + the environment objects inside.
- **Settings** (Android `ui/screens/SettingsScreen.kt`, iOS `Views/SettingsView.swift`; v1's
  `SettingsScreen.tsx`), in this order:
  language (segmented EN/AR) · appearance (follow-system switch, light/dark tiles otherwise —
  same interaction as the sibling apps) · countdown (show seconds — the clock then ticks per
  minute; count up after the date; 24-hour time) · home-screen widget
  ([`WidgetSection`](./src/components/settings/WidgetSection.tsx), Android only: the pinned
  countdown + "Change" → [`CountdownPickerSheet`](./src/components/settings/CountdownPickerSheet.tsx),
  "Add the widget" via `requestPinAppWidget` when the launcher supports it (manual steps
  otherwise), resize hint, lock-screen note — lands with the widgets) · data (delete all,
  confirm) · about (logo, the full version: `BuildConfig.VERSION_NAME` / Info.plist
  `DCVersionName`, description).
- **Loading**: the system splash / launch screen (night `#170B26`, 120 dp emblem) → the animated
  loading screen (Android `ui/LoadingScreen.kt`, iOS `Views/LoadingView.swift`), whose first frame
  is the same picture (emblem without its arc + a separate arc layer at rest), so the hand-off is
  invisible; then the gold arc spins, glows drift, sparks twinkle, the name fades in; at least
  1.2 s, then a 380 ms fade. Android follows the animator-duration scale, iOS Reduce Motion.
- **Toasts / confirms**: a pill above the tab bar (`ToastHost` / `ToastOverlay`, 1.8 s); destructive
  actions ask first (`ConfirmDialog` / `.alert`).

## Home-screen widget (v1's design — v2 keeps it minus the JS bridge)

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

## Lessons from v1 that still apply

- **DST and calendar days**: never count a countdown in 24-hour blocks (see the engine section);
  the tests run in `Asia/Beirut`, whose 2026/27 changes are at midnight.
- **Arabic digits**: the app shows Arabic-Indic digits in Arabic; Android's `ar` locale formats
  numbers with Latin digits, so digits are mapped by hand (the widget too). The widget's
  `Chronometer` digits follow the phone's locale — no API to change them.
- **Arabic labels wrap**: the hero's "days to go" label sits under the number, not beside it — side
  by side it wrapped and lost a word in Arabic ("متبقٍّ").
- **Sheets must cover the whole window** (v1's RN `Modal` laid its content out short under
  edge-to-edge, making the bottom of Save/Cancel untouchable). Check the sheet's buttons are
  tappable on a device after any sheet change.
- **Emulator**: this project's AVD is **Pixel_8a on port 5554** (`emulator -avd Pixel_8a -port 5554`);
  other sessions use other AVDs/ports — always `adb -s emulator-5554`. Git Bash: set
  `MSYS_NO_PATHCONV=1` for `adb shell` paths. If every emulator shows `unauthorized`, the adb server
  may be running under another account (session 0) — see help.md.

## Commands

```
# Android — in android/
yarn test                # JVM unit tests (Gradle testDebugUnitTest)
./gradlew assembleDebug  # debug APK
yarn build:apk           # release APK -> android/dist/DaysCounter-v<version>-build<code>.apk
yarn bump [patch|minor|major|pre|release] [--dry-run]   # android/version.properties + ios/project.yml
yarn release [--dry-run]                                # GitHub release for the current version's tag (no APKs)

# iOS — on the macOS CI runner (no Mac here)
cd ios/DaysCounterCore && swift test
cd ios && xcodegen generate && xcodebuild test -project DaysCounter.xcodeproj -scheme DaysCounter -destination 'platform=iOS Simulator,name=iPhone 16'
gh run list --limit 5 ; gh run view <id> --log-failed ; gh run download <id> -n ios-screenshots
```

## Conventions to keep following

- Every feature in **both** apps, tested on both (Android: unit tests + emulator; iOS: CI).
- The same strings in both apps: edit `android/app/src/main/assets/i18n/*.json` and copy to
  `ios/DaysCounterCore/Sources/DaysCounterCore/Resources/i18n/` (CI checks they're identical).
  New user-facing strings go in **both** `en.json` and `ar.json`.
- Every direction-sensitive layout mirrors in Arabic (instantly, from the app's language — not the
  phone's).
- Colors only from theme tokens; icons only from the Lucide set; touch targets ≥ 48 dp / 44 pt.
- Keep `TODO.md` checkboxes current; keep this file current when a convention changes.
- Every commit bumps the version, gets a tag + release, and is pushed (see the section above).
- Yarn (never npm) for the helper scripts.
