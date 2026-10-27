# Days Counter — عدّاد الأيام

Count down to the dates that matter — days, hours, minutes and seconds, ticking live — with a
resizable home-screen widget for your pinned countdown. English and Arabic (RTL), light and dark
themes, fully offline.

- **Home**: the pinned countdown as a big live card (days / hours / minutes / seconds, progress,
  "1 year, 10 months and 13 days"), plus every other countdown in a list. Add, edit, delete, pin.
- **Widget**: the pinned countdown on the home screen, resizable, ticking every second without the
  app running (Android); small/medium home-screen and lock-screen widgets (iOS).
- **Settings**: language, theme, countdown options, widget, about.

Two native apps, one folder each:

- [`android/`](./android/) — Kotlin + Jetpack Compose (Gradle)
- [`ios/`](./ios/) — Swift + SwiftUI (XcodeGen; built and tested on GitHub Actions)

```bash
cd android
yarn test          # unit tests of the Android app
./gradlew assembleDebug
yarn build:apk     # release APK → android/dist/
yarn bump minor    # version bump (both apps): minor = feature, patch = fix
yarn release       # GitHub release for the current version's tag
```

v2.0.0 (2026-10-27) is the native rewrite; v1 was a React Native app, in git history (tag `v1.0.2`).

Docs: [`CLAUDE.md`](./CLAUDE.md) (architecture), [`help.md`](./help.md) (commands),
[`TODO.md`](./TODO.md) (status), [`CONTINUE_PROJECT.md`](./CONTINUE_PROJECT.md) (handoff).
