# 🛠 Help — Days Counter

Quick command reference and troubleshooting cheat sheet. Two native apps: **`android/`** (Kotlin +
Jetpack Compose, Gradle) and **`ios/`** (Swift + SwiftUI, XcodeGen). v1 (React Native) was removed
in 2.0.0-alpha.2 — `git checkout v1.0.2` to look at it.

> ⚠️ **Yarn only** (Yarn 1 / classic) — never use `npm`. Yarn only runs the helper scripts in
> `android/package.json`; the apps themselves build with Gradle / Xcode.

---

## 🤖 Android (`android/`)

```bash
cd android
yarn test                              # JVM unit tests (countdown engine, phrases, stores, migration, widget payload)
yarn test --tests '*Countdown*'        # one suite
./gradlew assembleDebug                # debug APK → app/build/outputs/apk/debug/app-debug.apk
yarn build:apk                         # release APK → android/dist/DaysCounter-v<version>-build<code>.apk
```

Gradle needs JDK 17 (`JAVA_HOME=C:\Program Files\Java\jdk-17`). PowerShell has no `&&` in 5.1 and
needs `.\` for local scripts: `cd android; .\gradlew assembleDebug`.

### Emulator

```bash
emulator -avd Pixel_8a -port 5554 -no-snapshot-load    # this project's AVD, on a fixed port
adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.devlance.dayscounter/.MainActivity
adb -s emulator-5554 shell run-as com.devlance.dayscounter ls shared_prefs databases   # debug builds only
adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml && adb -s emulator-5554 shell cat /sdcard/ui.xml   # texts on screen
adb -s emulator-5554 exec-out screencap -p > shot.png
```

Other Claude sessions run their own emulators on other ports (the Hijri converter: Pixel_7 on
5560) — always pass `-s emulator-5554`. In Git Bash prefix `adb shell` commands that use device
paths with `MSYS_NO_PATHCONV=1` (otherwise `/sdcard/…` becomes `C:/Program Files/Git/sdcard/…`).

**Every emulator shows `unauthorized`** (even after a cold boot or `-wipe-data`): check the adb
server — `tasklist /V /FI "IMAGENAME eq adb.exe"`. If its session is **Services** (session 0), it
was started under another account and holds a different adb key; `adb kill-server` then
`adb start-server` from your own terminal fixes it (found 2026-10-27). Tell any other session that
uses the emulator first.

**Testing the update from v1:** v1's last APK isn't in the repo any more; build it from the
`v1.0.2` tag if needed. Install it, then `adb install -r` the v2 APK over it — same id and key, so
it must install as an update and import v1's countdowns and settings on first launch.

Without a v1 APK, plant a v1 database instead (debug build): pull/edit/push
`databases/RKStorage` (SQLite, table `catalystLocalStorage`, keys `days-counter-settings` /
`days-counter-countdowns`, values `{"state": {...}, "version": 0}`), delete
`shared_prefs/days_counter.xml`, and launch:

```bash
adb -s emulator-5554 push RKStorage /data/local/tmp/RKStorage
adb -s emulator-5554 shell "run-as com.devlance.dayscounter sh -c 'rm -f shared_prefs/days_counter.xml; cp /data/local/tmp/RKStorage databases/RKStorage'"
```

**iOS UI-test hooks** (launch environment): `DAYS_COUNTER_RESET=1` (fresh data),
`DAYS_COUNTER_LANGUAGE=en|ar`, `DAYS_COUNTER_THEME=system|light|dark`.

---

## 🍎 iOS (`ios/`) — built on GitHub Actions (no Mac here)

The `ios` job of `.github/workflows/ci.yml` runs on every push to `develop` or a `ci/**` branch:
`swift test` (engine), `xcodegen generate`, `xcodebuild test` (app + UI smoke test on the newest
iPhone simulator), screenshots uploaded as `ios-screenshots`.

```bash
gh run list --limit 5                          # latest runs
gh run watch <run-id>                          # follow one
gh run view <run-id> --log-failed              # compiler / test errors
gh run download <run-id> -n ios-screenshots    # the simulator screenshots
```

Getting new Swift code to compile: push it to a scratch branch first
(`git checkout -b ci/<topic> && git push -u origin ci/<topic>`), fix every error from one log in
one round, and only commit the finished change (with its version) on `develop`. Delete the scratch
branch afterwards (`git push origin --delete ci/<topic>`).

On a Mac:

```bash
brew install xcodegen
cd ios && xcodegen generate && open DaysCounter.xcodeproj
cd ios/DaysCounterCore && swift test
```

---

## 🏷 Versioning & releases (every commit)

Each commit carries its own version, tag and GitHub release, and is pushed to `develop`:

```bash
cd android
yarn bump pre                            # v2 phase → 2.0.0-alpha.N (after 2.0.0: minor = feature, patch = fix)
git add -A && git commit -m "…"          # the bump goes in the same commit as the work
git tag -a v2.0.0-alpha.3 -m "2.0.0-alpha.3"   # annotated, so --follow-tags pushes it
git push origin develop --follow-tags
yarn release                             # GitHub release "2.0.0-alpha.3" on its tag (notes = commit message)
yarn release --dry-run                   # preview
```

`yarn release` uses the GitHub token Git already stores (`git credential fill`). It updates the
release if it already exists, and marks 0.x and `-alpha` versions as pre-releases. **APKs are not
uploaded to GitHub** (the user's choice) — build them locally.

```bash
yarn bump                    # build number only (VERSION_CODE +1)
yarn bump patch              # 2.0.0 -> 2.0.1  + build number
yarn bump minor              # 2.0.0 -> 2.1.0  + build number
yarn bump major              # 2.0.0 -> 3.0.0  + build number
yarn bump pre                # 2.0.0-alpha.1 -> 2.0.0-alpha.2 + build number
yarn bump release            # 2.0.0-alpha.7 -> 2.0.0 + build number
yarn bump patch --dry-run    # preview without writing anything
```

Source of truth: `android/version.properties` (`VERSION_NAME`, `VERSION_CODE`; v2 build numbers
start at 100). The same run writes `ios/project.yml` (`MARKETING_VERSION` = the version without
`-alpha.N`, `CURRENT_PROJECT_VERSION` = the build number).

---

## 🧩 Home-screen widget

- Add it: long-press the home screen → **Widgets** → **Days Counter** (or Settings → "Add the
  widget to your home screen" in the app, on launchers that support it). Long-press the placed
  widget and drag its edges to resize — the layout switches between small / square / wide / large.
- It shows the **pinned** countdown (Home → pin button on any countdown, or Settings).
- Exact alarms come with the app (`USE_EXACT_ALARM` on Android 13+), so the day count flips right
  on time without any setting.
- **iPhone**: touch and hold the home screen → Edit → Add Widget → Days Counter (small or medium);
  on the lock screen: touch and hold it → Customize → Lock Screen → Days Counter (circular,
  rectangular or inline).

```bash
adb -s emulator-5554 shell dumpsys appwidget | grep -A3 dayscounter        # placed widgets + their sizes
adb -s emulator-5554 shell am broadcast -a com.devlance.dayscounter.widget.REFRESH -n com.devlance.dayscounter/.widget.DaysWidgetProvider   # force a redraw
adb -s emulator-5554 shell cmd appops set com.devlance.dayscounter SCHEDULE_EXACT_ALARM allow   # Android 12/12L only, if revoked (13+ uses USE_EXACT_ALARM)
```

---

## 🎨 Icons

```bash
cd android
yarn add --dev sharp       # once — the icon generator's only dependency, not kept installed
yarn build:icons           # Android launcher icons + splash emblem, iOS app icon + launch logo
yarn build:lucide          # Android's Lucide icon paths (ui/icons/LucideIcons.kt) from lucide-static 1.60.0 — no install needed
```

Android draws v1's Lucide icons; to add one, put its Lucide name in `scripts/build-lucide.js` and
re-run `yarn build:lucide`. The iOS app uses SF Symbols.

The emblem's geometry is also drawn in code (the Settings → About logo and the loading screen) —
keep `android/scripts/generate-icons.js` and the two `AppLogo` files in sync.

---

## 🔑 Signing (Android)

Every build so far — v1 included — is signed with `android/app/debug.keystore` (the public Android
debug key from the RN template). Updates must keep that signature, or Android refuses to install
them over the existing app. For a Play Store release a real key is needed (and a migration plan):

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
cd android && ./gradlew signingReport      # SHA-1 / SHA-256 fingerprints
```

---

## 📡 Android wireless debugging (adb over Wi-Fi)

1. On the phone: **Developer Options → Wireless Debugging → Pair device with pairing code**
2. `adb pair <ip>:<port>` (ip:port + code from the pairing dialog)
3. `adb connect <ip>:<port>` (ip:port shown at the **top** of the Wireless Debugging screen)
4. `adb disconnect` when done

---

## 🔗 Useful links

- Jetpack Compose — <https://developer.android.com/develop/ui/compose>
- App widgets (Android) — <https://developer.android.com/develop/ui/views/appwidgets>
- SwiftUI — <https://developer.apple.com/documentation/swiftui>
- WidgetKit — <https://developer.apple.com/documentation/widgetkit>
- XcodeGen — <https://github.com/yonaskolb/XcodeGen>
