import type { CountdownColor } from '../core/types';

export interface CountdownPalette {
  /** Hero card / widget gradient. Both ends carry white text at ≥ 4.5:1. */
  from: string;
  to: string;
  /** Accent on light surfaces (≥ 4.5:1 on white) and on dark surfaces. */
  light: string;
  dark: string;
  /** Tinted badge backgrounds behind the accent. */
  softLight: string;
  softDark: string;
}

/**
 * Per-countdown colors. Keep `from`/`to` in sync with `WidgetColors.kt` (the widget draws the same
 * gradient natively).
 */
export const COUNTDOWN_PALETTE: Record<CountdownColor, CountdownPalette> = {
  ember: { from: '#7C2D12', to: '#C2410C', light: '#C2410C', dark: '#FF8A50', softLight: '#FDE7DC', softDark: '#3A1E14' },
  violet: { from: '#3B0764', to: '#7C3AED', light: '#6D28D9', dark: '#B794F6', softLight: '#EDE5FD', softDark: '#2A1E45' },
  ocean: { from: '#0C4A6E', to: '#0369A1', light: '#0369A1', dark: '#7DD3FC', softLight: '#E0F2FE', softDark: '#0F2A3A' },
  teal: { from: '#134E4A', to: '#0F766E', light: '#0F766E', dark: '#5EEAD4', softLight: '#DDF5F1', softDark: '#0F2E2B' },
  rose: { from: '#881337', to: '#E11D48', light: '#BE123C', dark: '#FDA4AF', softLight: '#FFE4E8', softDark: '#3B1520' },
  forest: { from: '#14532D', to: '#15803D', light: '#15803D', dark: '#86EFAC', softLight: '#DCF5E4', softDark: '#13301E' },
  indigo: { from: '#312E81', to: '#4F46E5', light: '#4338CA', dark: '#A5B4FC', softLight: '#E4E6FD', softDark: '#1E1F45' },
  gold: { from: '#78350F', to: '#B45309', light: '#B45309', dark: '#FCD34D', softLight: '#FDF0D5', softDark: '#3A2A10' },
  graphite: { from: '#1E293B', to: '#475569', light: '#475569', dark: '#CBD5E1', softLight: '#E8ECF1', softDark: '#232B36' },
};

export function accentOf(color: CountdownColor, isDark: boolean) {
  const p = COUNTDOWN_PALETTE[color];
  return { accent: isDark ? p.dark : p.light, soft: isDark ? p.softDark : p.softLight };
}
