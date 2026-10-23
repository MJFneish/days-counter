import React from 'react';
import Svg, { Circle, Defs, G, LinearGradient, Path, RadialGradient, Rect, Stop } from 'react-native-svg';

import { brand } from '../../theme/colors';

/**
 * The emblem, on a 100×100 viewBox: a countdown ring three-quarters done (gold arc + knob) around
 * a calendar page with one day marked. Original artwork. The same numbers live in
 * scripts/generate-icons.js (launcher icons + splash logo) — keep the two in sync.
 */
export const RING = { cx: 50, cy: 50, r: 34, width: 7 } as const;
/** 270° clockwise from 12 o'clock to 9 o'clock. */
export const ARC_PATH = 'M50 16 A34 34 0 1 1 16 50';
export const KNOB = { cx: 16, cy: 50, r: 5.2 } as const;
const SHEET = { x: 32, y: 34, w: 36, h: 33, rx: 6 } as const;
const HEADER_PATH = 'M32 44 V40 A6 6 0 0 1 38 34 H62 A6 6 0 0 1 68 40 V44 Z';
const BINDERS = [40, 55.5] as const;
const DOTS = [
  [41, 52],
  [50, 52],
  [59, 52],
  [41, 60],
  [50, 60],
] as const;
const MARKED_DOT = [59, 60] as const;

interface Props {
  size: number;
  /** Draw the rounded dusk tile behind the emblem (Settings → About). */
  tile?: boolean;
  /** Draw the gold progress arc + knob. The loading screen turns this off and spins its own copy. */
  arc?: boolean;
}

export function ArcLayer({ size }: { size: number }) {
  return (
    <Svg width={size} height={size} viewBox="0 0 100 100">
      <Defs>
        <LinearGradient id="logoArcSpin" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#FFE0A3" />
          <Stop offset="1" stopColor={brand.goldDeep} />
        </LinearGradient>
      </Defs>
      <Path d={ARC_PATH} stroke="url(#logoArcSpin)" strokeWidth={RING.width} strokeLinecap="round" fill="none" />
      <Circle cx={KNOB.cx} cy={KNOB.cy} r={KNOB.r} fill="#FFFFFF" />
    </Svg>
  );
}

export function AppLogo({ size, tile = false, arc = true }: Props) {
  const inner = tile ? 'translate(14 14) scale(0.72)' : undefined;
  return (
    <Svg width={size} height={size} viewBox="0 0 100 100">
      <Defs>
        <LinearGradient id="logoTile" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor={brand.tileFrom} />
          <Stop offset="1" stopColor={brand.tileTo} />
        </LinearGradient>
        <RadialGradient id="logoGlow" cx="0.3" cy="0.25" r="0.7">
          <Stop offset="0" stopColor="#A855F7" stopOpacity={0.45} />
          <Stop offset="1" stopColor="#A855F7" stopOpacity={0} />
        </RadialGradient>
        <LinearGradient id="logoArc" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#FFE0A3" />
          <Stop offset="1" stopColor={brand.goldDeep} />
        </LinearGradient>
      </Defs>
      {tile && (
        <>
          <Rect width={100} height={100} rx={22} fill="url(#logoTile)" />
          <Rect width={100} height={100} rx={22} fill="url(#logoGlow)" />
        </>
      )}
      <G transform={inner}>
        <Circle cx={RING.cx} cy={RING.cy} r={RING.r} stroke="#FFFFFF" strokeOpacity={0.22} strokeWidth={RING.width} fill="none" />
        {arc && (
          <>
            <Path d={ARC_PATH} stroke="url(#logoArc)" strokeWidth={RING.width} strokeLinecap="round" fill="none" />
            <Circle cx={KNOB.cx} cy={KNOB.cy} r={KNOB.r} fill="#FFFFFF" />
          </>
        )}
        <Rect x={SHEET.x} y={SHEET.y} width={SHEET.w} height={SHEET.h} rx={SHEET.rx} fill="#FFFFFF" />
        <Path d={HEADER_PATH} fill={brand.ember} />
        {BINDERS.map(x => (
          <Rect key={x} x={x} y={30} width={4.5} height={8} rx={2.25} fill="#FFE3D0" />
        ))}
        {DOTS.map(([x, y]) => (
          <Circle key={`${x}-${y}`} cx={x} cy={y} r={2.8} fill="#E9D9F0" />
        ))}
        <Circle cx={MARKED_DOT[0]} cy={MARKED_DOT[1]} r={3.4} fill={brand.ember} />
      </G>
    </Svg>
  );
}
