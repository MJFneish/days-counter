# Days Counter — عدّاد الأيام

Count down to the dates that matter — days, hours, minutes and seconds, ticking live — with a
resizable Android home-screen widget for your pinned countdown. English and Arabic (RTL), light
and dark themes, fully offline.

- **Home**: the pinned countdown as a big live card (days / hours / minutes / seconds, progress,
  "1 year, 10 months and 13 days"), plus every other countdown in a list. Add, edit, delete, pin.
- **Widget**: the pinned countdown on the home screen, resizable (small / wide / large), ticking
  every second without the app running.
- **Settings**: language, theme, countdown options, widget, about.

Bare React Native CLI 0.87 + TypeScript, Yarn 1.

```bash
yarn install
yarn start        # Metro
yarn android      # or see help.md for building with Gradle directly on Windows
yarn test         # countdown engine tests
yarn bump minor   # version bump (package.json + Android + iOS)
yarn release      # GitHub release for the current version's tag
```

Docs: [`CLAUDE.md`](./CLAUDE.md) (architecture), [`help.md`](./help.md) (commands),
[`TODO.md`](./TODO.md) (status), [`CONTINUE_PROJECT.md`](./CONTINUE_PROJECT.md) (handoff).
