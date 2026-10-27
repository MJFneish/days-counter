#!/usr/bin/env node
/**
 * Generates every icon of both apps from one emblem definition: the Android launcher icons
 * (legacy + adaptive + monochrome) and splash emblem, and the iOS app icon and launch logo.
 * Original artwork — a countdown ring three quarters done (gold arc + knob) around a calendar page
 * with one day marked, on the dusk tile (deep violet → ember).
 *
 * Usage (in android/):  yarn build:icons
 * Needs `sharp`, which isn't kept installed: `yarn add --dev sharp` first if it's missing.
 *
 * The emblem geometry is also drawn in code — keep these in sync:
 *   android/app/src/main/java/com/devlance/dayscounter/ui/brand/AppLogo.kt
 *   ios/DaysCounter/Views/Brand/AppLogo.swift
 */
const fs = require('fs');
const path = require('path');
const { Buffer } = require('buffer');

let sharp;
try {
  sharp = require('sharp');
} catch {
  console.error('build:icons needs sharp — run `yarn add --dev sharp` in android/ first.');
  process.exit(1);
}

const androidDir = path.resolve(__dirname, '..');
const resDir = path.join(androidDir, 'app', 'src', 'main', 'res');
const assetsDir = path.join(androidDir, '..', 'ios', 'DaysCounter', 'Assets.xcassets');

// Same values as `brand` in src/theme/colors.ts.
const TILE_FROM = '#3B0764';
const TILE_TO = '#C2410C';
const EMBER = '#F97316';
const GOLD_LIGHT = '#FFE0A3';
const GOLD_DEEP = '#FF9A4D';

// --- emblem geometry (100x100 viewBox) — same numbers as AppLogo.tsx ---
const RING = { cx: 50, cy: 50, r: 34, width: 7 };
const ARC_PATH = 'M50 16 A34 34 0 1 1 16 50';
const KNOB = { cx: 16, cy: 50, r: 5.2 };
const SHEET = { x: 32, y: 34, w: 36, h: 33, rx: 6 };
const HEADER_PATH = 'M32 44 V40 A6 6 0 0 1 38 34 H62 A6 6 0 0 1 68 40 V44 Z';
const BINDERS = [40, 55.5];
const DOTS = [
  [41, 52],
  [50, 52],
  [59, 52],
  [41, 60],
  [50, 60],
];
const MARKED_DOT = [59, 60];

/** Emblem markup. `mono` = single flat color (Android 13 themed icon): outlines instead of fills
 * so the page still reads as a page. */
function emblem({ mono = null } = {}) {
  const track = `<circle cx="${RING.cx}" cy="${RING.cy}" r="${RING.r}" stroke="${mono ?? '#FFFFFF'}" stroke-opacity="${mono ? 0.4 : 0.22}" stroke-width="${RING.width}" fill="none"/>`;
  const arc = `<path d="${ARC_PATH}" stroke="${mono ?? 'url(#arc)'}" stroke-width="${RING.width}" stroke-linecap="round" fill="none"/>`;
  const knob = `<circle cx="${KNOB.cx}" cy="${KNOB.cy}" r="${KNOB.r}" fill="${mono ?? '#FFFFFF'}"/>`;
  const binders = BINDERS.map(
    x => `<rect x="${x}" y="30" width="4.5" height="8" rx="2.25" fill="${mono ?? '#FFE3D0'}"/>`,
  ).join('');
  const dots = DOTS.map(([x, y]) => `<circle cx="${x}" cy="${y}" r="2.8" fill="${mono ?? '#E9D9F0'}"/>`).join('');
  const marked = `<circle cx="${MARKED_DOT[0]}" cy="${MARKED_DOT[1]}" r="3.4" fill="${mono ?? EMBER}"/>`;
  const sheet = mono
    ? `<rect x="${SHEET.x + 1.5}" y="${SHEET.y + 1.5}" width="${SHEET.w - 3}" height="${SHEET.h - 3}" rx="${SHEET.rx - 1}" stroke="${mono}" stroke-width="3" fill="none"/><path d="${HEADER_PATH}" fill="${mono}"/>`
    : `<rect x="${SHEET.x}" y="${SHEET.y}" width="${SHEET.w}" height="${SHEET.h}" rx="${SHEET.rx}" fill="#FFFFFF"/><path d="${HEADER_PATH}" fill="${EMBER}"/>`;
  return `${track}${arc}${knob}${sheet}${binders}${dots}${marked}`;
}

const defs = `<defs>
  <linearGradient id="tile" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${TILE_FROM}"/><stop offset="1" stop-color="${TILE_TO}"/></linearGradient>
  <linearGradient id="arc" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${GOLD_LIGHT}"/><stop offset="1" stop-color="${GOLD_DEEP}"/></linearGradient>
  <radialGradient id="glow" cx="0.3" cy="0.25" r="0.7"><stop offset="0" stop-color="#A855F7" stop-opacity="0.45"/><stop offset="1" stop-color="#A855F7" stop-opacity="0"/></radialGradient>
</defs>`;

const svg = (body, { size = 100 } = {}) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 100 100">${defs}${body}</svg>`;

const tileBg = shape =>
  shape === 'circle'
    ? `<circle cx="50" cy="50" r="50" fill="url(#tile)"/><circle cx="50" cy="50" r="50" fill="url(#glow)"/>`
    : shape === 'square'
      ? `<rect width="100" height="100" fill="url(#tile)"/><rect width="100" height="100" fill="url(#glow)"/>`
      : `<rect width="100" height="100" rx="22" fill="url(#tile)"/><rect width="100" height="100" rx="22" fill="url(#glow)"/>`;

const SOURCES = {
  // Legacy launcher icons: emblem at 72% on a rounded / circular tile (same as AppLogo `tile`).
  legacy: svg(`${tileBg('rounded')}<g transform="translate(14 14) scale(0.72)">${emblem()}</g>`),
  legacyRound: svg(`${tileBg('circle')}<g transform="translate(14 14) scale(0.72)">${emblem()}</g>`),
  // Adaptive icon layers (108dp canvas; the launcher masks to ~72dp, so keep the emblem in the
  // central 66% safe zone).
  adaptiveBackground: svg(tileBg('square')),
  adaptiveForeground: svg(`<g transform="translate(19 19) scale(0.62)">${emblem()}</g>`),
  adaptiveMonochrome: svg(`<g transform="translate(19 19) scale(0.62)">${emblem({ mono: '#FFFFFF' })}</g>`),
  // iOS: full-bleed square (iOS applies its own mask), no transparency allowed.
  ios: svg(`${tileBg('square')}<g transform="translate(15 15) scale(0.7)">${emblem()}</g>`),
  // Splash emblem alone (the splash background is the brand night colour), 120 units wide.
  splash: svg(emblem()),
  // Android 12 SplashScreen icon: a 288 dp canvas with the 120 dp emblem in the middle.
  androidSplash: `<svg xmlns="http://www.w3.org/2000/svg" width="288" height="288" viewBox="0 0 288 288">${defs}<g transform="translate(84 84) scale(1.2)">${emblem()}</g></svg>`,
};

const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

async function png(svgString, size, outPath) {
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  await sharp(Buffer.from(svgString), { density: Math.max(72, (72 * size) / 100) })
    .resize(size, size)
    .png()
    .toFile(outPath);
}

async function main() {
  for (const [density, scale] of Object.entries(DENSITIES)) {
    const dir = path.join(resDir, `mipmap-${density}`);
    await png(SOURCES.legacy, 48 * scale, path.join(dir, 'ic_launcher.png'));
    await png(SOURCES.legacyRound, 48 * scale, path.join(dir, 'ic_launcher_round.png'));
    await png(SOURCES.adaptiveBackground, 108 * scale, path.join(dir, 'ic_launcher_background.png'));
    await png(SOURCES.adaptiveForeground, 108 * scale, path.join(dir, 'ic_launcher_foreground.png'));
    await png(SOURCES.adaptiveMonochrome, 108 * scale, path.join(dir, 'ic_launcher_monochrome.png'));
    await png(SOURCES.androidSplash, 288 * scale, path.join(resDir, `drawable-${density}`, 'splash_emblem.png'));
  }

  const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
`;
  const anydpi = path.join(resDir, 'mipmap-anydpi-v26');
  fs.mkdirSync(anydpi, { recursive: true });
  fs.writeFileSync(path.join(anydpi, 'ic_launcher.xml'), adaptiveXml);
  fs.writeFileSync(path.join(anydpi, 'ic_launcher_round.xml'), adaptiveXml);

  // iOS: single 1024 universal icon (Xcode 14+ derives every size from it), no transparency.
  const iconDir = path.join(assetsDir, 'AppIcon.appiconset');
  fs.mkdirSync(iconDir, { recursive: true });
  await sharp(Buffer.from(SOURCES.ios), { density: 740 })
    .resize(1024, 1024)
    .flatten({ background: TILE_FROM })
    .png()
    .toFile(path.join(iconDir, 'AppIcon.png'));

  // iOS launch screen logo: the 120 pt emblem at @1x/@2x/@3x.
  const launchDir = path.join(assetsDir, 'LaunchLogo.imageset');
  for (const scale of [1, 2, 3]) {
    await png(SOURCES.splash, 120 * scale, path.join(launchDir, `LaunchLogo${scale === 1 ? '' : `@${scale}x`}.png`));
  }
  console.log('icons generated');
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
