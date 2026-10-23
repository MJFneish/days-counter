/** A wall-clock date-time in the device's time zone (month 1-12). Countdowns store this rather
 * than an epoch instant, so "5 Sep 2027 at 00:00" stays midnight local time even if the phone's
 * time zone or DST rules change between now and then. */
export interface LocalDateTime {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
}

export const COUNTDOWN_COLORS = ['ember', 'violet', 'ocean', 'teal', 'rose', 'forest', 'indigo', 'gold', 'graphite'] as const;
export type CountdownColor = (typeof COUNTDOWN_COLORS)[number];

export const COUNTDOWN_ICONS = [
  'calendar',
  'star',
  'heart',
  'cake',
  'plane',
  'graduation',
  'briefcase',
  'home',
  'baby',
  'trophy',
  'gift',
  'flag',
] as const;
export type CountdownIcon = (typeof COUNTDOWN_ICONS)[number];

export interface Countdown {
  id: string;
  /** May be empty — the UI then shows the formatted target date as the title. */
  title: string;
  target: LocalDateTime;
  color: CountdownColor;
  icon: CountdownIcon;
  /** Epoch ms the countdown was created — the start of its progress bar. */
  createdAt: number;
}

export const isCountdownColor = (v: unknown): v is CountdownColor =>
  typeof v === 'string' && (COUNTDOWN_COLORS as readonly string[]).includes(v);

export const isCountdownIcon = (v: unknown): v is CountdownIcon =>
  typeof v === 'string' && (COUNTDOWN_ICONS as readonly string[]).includes(v);
