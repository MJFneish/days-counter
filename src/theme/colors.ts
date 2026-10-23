/**
 * "Dusk" — an ember-orange primary with a deep violet secondary, the two ends of a dusk-to-dawn
 * gradient (time passing). From the ui-ux-pro-max recommendation for a countdown/event app
 * ("excitement purple + action orange", vibrant & block-based), with orange leading so the app
 * stays distinct from its siblings (the Hijri converter is indigo, prayer-times-lb emerald).
 *
 * Every text-bearing pair is ≥ 4.5:1 in its theme: light `primary` #C2410C is 5.2:1 on white
 * (the brighter #F97316 is only 2.8:1, so it's decorative only), and the hero gradient's ends
 * both carry white text at ≥ 5:1.
 */
export const lightTheme = {
  background: '#FFF8F4',
  surface: '#FFFFFF',
  surfaceAlt: '#F8ECE6',
  primary: '#C2410C',
  primaryDark: '#9A3412',
  primarySoft: '#FDE7DC',
  onPrimary: '#FFFFFF',
  secondary: '#6D28D9',
  secondarySoft: '#EDE5FD',
  onSecondary: '#FFFFFF',
  text: '#1E1428',
  textMuted: '#6A5D72',
  border: '#EEDFD6',
  card: '#FFFFFF',
  cardShadow: 'rgba(120, 45, 18, 0.14)',
  danger: '#B91C1C',
  dangerSoft: '#FDE2E2',
  heroFrom: '#3B0764',
  heroTo: '#C2410C',
  onHero: '#FFFFFF',
  onHeroMuted: 'rgba(255, 255, 255, 0.82)',
  /** Translucent white panels on a gradient (the H/M/S tiles). */
  onHeroPanel: 'rgba(255, 255, 255, 0.16)',
  scrim: 'rgba(20, 10, 30, 0.45)',
  /** Switch "off" state — the default surfaces are too close to the card colour to read. */
  switchTrackOff: '#D9C9C0',
  switchThumbOff: '#FFFFFF',
};

export type ThemeColors = typeof lightTheme;

export const darkTheme: ThemeColors = {
  background: '#120B18',
  surface: '#1C1323',
  surfaceAlt: '#271B30',
  primary: '#FF8A50',
  primaryDark: '#F97316',
  primarySoft: '#3A1E14',
  onPrimary: '#2A0D00',
  secondary: '#B794F6',
  secondarySoft: '#2A1E45',
  onSecondary: '#1A0B33',
  text: '#FBF3F8',
  textMuted: '#B6A8BE',
  border: '#33243D',
  card: '#1E1526',
  cardShadow: 'rgba(0, 0, 0, 0.5)',
  danger: '#F87171',
  dangerSoft: '#3B1717',
  heroFrom: '#2E0752',
  heroTo: '#B23A0C',
  onHero: '#FFFFFF',
  onHeroMuted: 'rgba(255, 255, 255, 0.8)',
  onHeroPanel: 'rgba(255, 255, 255, 0.14)',
  scrim: 'rgba(0, 0, 0, 0.6)',
  switchTrackOff: '#3E2F49',
  switchThumbOff: '#B6A8BE',
};

/** Fixed brand colors for the icon, native splash and JS loading screen (same in both themes, so
 * the native → JS splash hand-off is seamless). Keep `night` in sync with the `--background`
 * passed to react-native-bootsplash (see help.md) and with `scripts/generate-icons.js`. */
export const brand = {
  night: '#170B26',
  violet: '#6D28D9',
  ember: '#F97316',
  gold: '#FFC56E',
  goldDeep: '#FF9A4D',
  light: '#FFF4EC',
  tileFrom: '#3B0764',
  tileTo: '#C2410C',
};
