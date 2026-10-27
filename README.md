# Days Counter — عدّاد الأيام

Count down to the dates that matter — days, hours, minutes and seconds, ticking live — with a
resizable Android home-screen widget for your pinned countdown. English and Arabic (RTL), light
and dark themes, fully offline.

- **Home**: the pinned countdown as a big live card (days / hours / minutes / seconds, progress,
  "1 year, 10 months and 13 days"), plus every other countdown in a list. Add, edit, delete, pin.
- **Widget**: the pinned countdown on the home screen, resizable (small / wide / large), ticking
  every second without the app running.
- **Settings**: language, theme, countdown options, widget, about.

**v2 (since 2.0.0): native Android — Kotlin + Jetpack Compose in [`native/`](./native/).**
v1 was a bare React Native CLI app (`src/`, `android/`, `ios/`), kept in the repo until removed.

```bash
yarn test:native  # JVM unit tests of the native app
cd native && ./gradlew assembleDebug   # debug APK
yarn build:apk    # release APK → dist/
yarn bump pre     # version bump (native/version.properties)
yarn release      # GitHub release for the current version's tag
```

Docs: [`CLAUDE.md`](./CLAUDE.md) (architecture), [`help.md`](./help.md) (commands),
[`TODO.md`](./TODO.md) (status), [`CONTINUE_PROJECT.md`](./CONTINUE_PROJECT.md) (handoff).
