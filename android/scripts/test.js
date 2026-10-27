#!/usr/bin/env node
/**
 * Runs the Android app's JVM unit tests (countdown engine, phrases, stores, v1 migration, widget
 * payload): `yarn test` in android/. Extra arguments go to Gradle, e.g. `--tests '*Countdown*'`.
 * Same script as Mawakit's. (iOS: `swift test` in ios/DaysCounterCore, on the macOS CI runner.)
 */
const path = require('path');
const { spawnSync } = require('child_process');

const nativeDir = path.resolve(__dirname, '..');
const gradlew = path.join(nativeDir, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');
const result = spawnSync(process.platform === 'win32' ? `"${gradlew}"` : gradlew, ['testDebugUnitTest', ...process.argv.slice(2)], {
  cwd: nativeDir,
  stdio: 'inherit',
  shell: process.platform === 'win32',
});
process.exit(result.status ?? 1);
