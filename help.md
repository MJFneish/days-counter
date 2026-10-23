# 🛠 Help — Days Counter (bare React Native CLI)

Quick command reference and troubleshooting cheat sheet. This project is **bare React Native
CLI** (like `Gregorian-hijri-date-converter`, unlike the Expo-based `prayer-times-lb`) —
`android/` and `ios/` are real, checked-in native projects; there is no `expo prebuild`.

> ⚠️ **Yarn only** (Yarn 1 / classic) — never use `npm` in this repo.

---

## 🚀 Daily commands

```bash
yarn install        # install deps
yarn start          # Metro bundler
yarn android        # build + install + run on a connected device/emulator
yarn ios            # build + install + run (macOS + Xcode only)
yarn test           # jest — countdown engine tests
yarn typecheck      # tsc --noEmit
yarn lint           # eslint
```

```bash
yarn start --reset-cache          # Metro with a cleared bundler cache
npx react-native doctor           # diagnose toolchain/env issues
```

---

## 🏷 Versioning & releases (every commit)

Each commit carries its own version, tag and GitHub release, and is pushed to `develop`:

```bash
yarn bump minor                          # feature → 0.2.0 (fix → yarn bump patch)
git add -A && git commit -m "…"          # the bump goes in the same commit as the work
git tag -a v0.2.0 -m "0.2.0"             # annotated, so --follow-tags pushes it
git push origin develop --follow-tags
yarn release                             # GitHub release "0.2.0" on tag v0.2.0 (notes = commit message)
yarn release --asset android/app/build/outputs/apk/release/DaysCounter-1.0.0.apk   # attach the APK
yarn release --dry-run                   # preview
```

`yarn release` uses the GitHub token Git already stores (`git credential fill`) — `gh` isn't
installed on this machine. It updates the release if it already exists, and marks 0.x versions as
pre-releases.

### Version bump details

```bash
yarn bump                    # build number only: android versionCode +1 (+ iOS CURRENT_PROJECT_VERSION)
yarn bump patch              # 1.0.0 -> 1.0.1  + build number
yarn bump minor              # 1.0.0 -> 1.1.0  + build number
yarn bump major              # 1.0.0 -> 2.0.0  + build number
yarn bump patch --dry-run    # preview without writing anything
```

Updates `package.json` (source of truth — Settings → About shows it), `android/app/build.gradle`
(`versionCode` / `versionName`) and `ios/DaysCounter.xcodeproj/project.pbxproj`
(`MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`) in place — see `scripts/bump-version.js`.

---

## 📦 Release APK file name

Release APKs are named **`DaysCounter-<version>.apk`** in `android/app/build/outputs/apk/release/`
— set in `android/app/build.gradle` (`releaseApkBaseName`) from `versionName`, so each
`yarn bump` changes it. Debug builds keep `app-debug.apk` (the RN CLI expects that name).

---

## 🩹 Patched libraries

`patches/` holds two patch-package patches (see CLAUDE.md "Known gotchas"), re-applied
automatically by `postinstall` after every `yarn install`. To re-apply by hand:

```bash
npx patch-package
```

---

## 🪵 Reading console warnings/errors

RN 0.87 only says "Open debugger to view warnings" in the app. Without a debugger:

```bash
adb logcat -c                                     # clear, then reproduce
adb logcat -s ReactNativeJS:V ReactNative:W AndroidRuntime:E
```

If the app hangs on the splash in debug, check Metro answers: `curl http://localhost:8081/status`
→ `packager-status:running`. No answer = Metro is stuck (common after `yarn add` on Windows):
close it and run `yarn start --reset-cache`.

---

## 📵 "Unable to load script" / Metro connection issues

```bash
yarn start                        # make sure Metro is running
adb reverse tcp:8081 tcp:8081     # forward device → Metro over USB
adb devices                       # check the device is attached (not "offline")
adb kill-server && adb start-server   # if it shows "offline"
```

---

## 🪟 Windows: building without `yarn android`

`npx react-native run-android` can fail on this machine with `'gradlew.bat' is not recognized` (a
Node child-process quirk). Build with Gradle directly:

> ⚠️ `installDebug` / `adb install` need a **running emulator or a USB-connected phone** — with
> nothing attached the build succeeds but the install step fails with
> `DeviceException: No connected devices!`. Check with `adb devices` first.

```bash
# 1. Attach a device — either start an emulator (AVDs on this machine: Pixel_4a, Pixel_7, Pixel_8a, Medium_Phone_API_36.0)…
emulator -avd Pixel_8a          # full path: $ANDROID_HOME/emulator/emulator.exe — leave it running
# …or plug in the phone with Developer options → USB debugging on, and accept the prompt on the phone
adb devices                     # must list a device as "device" (not "unauthorized"/"offline")

# 2a. Debug build (needs Metro running for the JS)
yarn start                                             # terminal 1
cd android && ./gradlew installDebug && cd ..          # terminal 2 — builds + installs
adb reverse tcp:8081 tcp:8081                          # phone over USB → Metro on this PC

# 2b. Standalone release APK (no Metro needed)
cd android && ./gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a && cd ..   # phone ABI (fast)
cd android && ./gradlew assembleRelease && cd ..       # all ABIs (also runs on x86_64 emulators)
cd android && ./gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a,x86_64 && cd ..   # phone + emulator
adb install -r android/app/build/outputs/apk/release/DaysCounter-1.0.0.apk   # use the current version
adb shell am start -n com.devlance.dayscounter/.MainActivity

# Only build, no device needed
cd android && ./gradlew assembleDebug && cd ..         # → android/app/build/outputs/apk/debug/app-debug.apk
```

**PowerShell (VS Code's default terminal on Windows):** Windows PowerShell 5.1 has no `&&`, and
needs `.\` for local scripts — use `;` instead:

```powershell
cd android; .\gradlew installDebug; cd ..
cd android; .\gradlew assembleRelease -PreactNativeArchitectures=arm64-v8a; cd ..
```

The `arm64-v8a` release APK runs on real phones only — x86_64 emulators need the all-ABI build
(or use the debug build, which includes every ABI).

Gradle needs JDK 17 (`JAVA_HOME=C:\Program Files\Java\jdk-17` on this machine).

**"ninja: error: Stat(…): Filename longer than 260 characters"** — Windows path limit hit by the
native codegen build. Already fixed: `android/app/build.gradle` pins CMake **3.31.6** (Ninja 1.12,
long-path aware). Requirements if it ever comes back:

```bash
sdkmanager "cmake;3.31.6"                         # (AGP also auto-downloads it)
reg query HKLM\SYSTEM\CurrentControlSet\Control\FileSystem /v LongPathsEnabled   # must be 0x1
cd android && ./gradlew clean && cd ..            # drop the stale .cxx built with the old Ninja
```

---

## 🔑 Release keystores (Android)

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
# CN=Days Counter, OU=DV, O=Devlance, L=Beirut, ST=Beirut, C=LB
```

Place it under `android/app/` and wire it in `android/app/build.gradle` → `signingConfigs`. Until
then, release builds are signed with the template's `debug.keystore` (fine for side-loading, not
for the Play Store). `*.keystore` (except `debug.keystore`) is git-ignored — back it up elsewhere.

```bash
cd android && ./gradlew bundleRelease && cd ..      # AAB  → android/app/build/outputs/bundle/release/
cd android && ./gradlew signingReport && cd ..      # SHA-1 / SHA-256 fingerprints
```

---

## 🧹 Cleaning / cache nukes

From least to most destructive:

```bash
yarn start --reset-cache                                        # Metro bundler cache
cd android && ./gradlew clean && cd ..                          # Gradle build outputs
rm -rf android/app/build android/.gradle android/.cxx android/build
rm -rf ~/.gradle/caches ~/.gradle/daemon                        # global Gradle caches
rm -rf node_modules && yarn install                             # dependency reset
```

```bash
cd ios && pod install && cd ..          # after any native iOS dependency change (macOS only)
```

---

## 📡 Android wireless debugging (adb over Wi-Fi)

1. On the phone: **Developer Options → Wireless Debugging → Pair device with pairing code**
2. `adb pair <ip>:<port>` (ip:port + code from the pairing dialog)
3. `adb connect <ip>:<port>` (ip:port shown at the **top** of the Wireless Debugging screen)
4. `adb disconnect` when done

---

## 🔗 Useful links

- React Native — <https://reactnative.dev/docs/getting-started>
- Turbo Native Modules (Android) — <https://reactnative.dev/docs/turbo-native-modules-introduction>
- App widgets — <https://developer.android.com/develop/ui/views/appwidgets>
- React Navigation — <https://reactnavigation.org/docs/getting-started>
- Reanimated v4 — <https://docs.swmansion.com/react-native-reanimated/>
- react-native-bootsplash — <https://github.com/zoontek/react-native-bootsplash>
