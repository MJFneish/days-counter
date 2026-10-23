import type { LocalDateTime } from './types';

export const SECOND_MS = 1000;
export const MINUTE_MS = 60 * SECOND_MS;
export const HOUR_MS = 60 * MINUTE_MS;
export const DAY_MS = 24 * HOUR_MS;

/** Epoch ms of the target's wall-clock time shifted by `days` calendar days (negative = earlier),
 * in the device's time zone. Date normalizes day overflow (31 Mar − 31 days → 28/29 Feb's
 * neighbours) and resolves a time that a DST jump skipped to the first valid instant after it. */
export function shiftedInstant(t: LocalDateTime, days: number): number {
  return new Date(t.year, t.month - 1, t.day + days, t.hour, t.minute, 0, 0).getTime();
}

export const toInstant = (t: LocalDateTime): number => shiftedInstant(t, 0);

export function fromDate(date: Date): LocalDateTime {
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate(),
    hour: date.getHours(),
    minute: date.getMinutes(),
  };
}

export type CountdownStatus = 'upcoming' | 'arrived' | 'past';

export interface Remaining {
  /** `upcoming` until the target instant, `arrived` for the first calendar day after it, then `past`. */
  status: CountdownStatus;
  /**
   * Whole calendar days to the target (upcoming) or since it (arrived/past). Days are anchored to
   * the target's wall-clock time, so a midnight countdown flips its day count at local midnight —
   * also across a DST change, where counting 24-hour blocks would flip it at 23:00 or 01:00.
   */
  days: number;
  /** The rest, below one calendar day (on a DST day that rest can reach 23 or 24 hours). */
  hours: number;
  minutes: number;
  seconds: number;
  /** |target − now| in ms. */
  totalMs: number;
  /** When `days` changes next — the widget's Chronometer counts to this. */
  nextChangeAt: number;
}

function split(ms: number) {
  const total = Math.floor(ms / SECOND_MS);
  return {
    hours: Math.floor(total / 3600),
    minutes: Math.floor((total % 3600) / 60),
    seconds: total % 60,
  };
}

/**
 * Time left until (or elapsed since) `target` at `now`. Mirrors `Countdown.kt` in the widget —
 * keep the two in sync. Seconds are truncated, like Android's Chronometer, so the app and the
 * widget tick over on the same second.
 */
export function remaining(target: LocalDateTime, now: number): Remaining {
  const targetAt = toInstant(target);

  if (now < targetAt) {
    // Largest d with (target − d calendar days) >= now. The 24h estimate is off by at most one
    // around DST changes; the loops correct it.
    let d = Math.floor((targetAt - now) / DAY_MS);
    while (shiftedInstant(target, -(d + 1)) >= now) d++;
    while (d > 0 && shiftedInstant(target, -d) < now) d--;
    const boundary = shiftedInstant(target, -d);
    return { status: 'upcoming', days: d, ...split(boundary - now), totalMs: targetAt - now, nextChangeAt: boundary };
  }

  // Largest d with (target + d calendar days) <= now.
  let d = Math.floor((now - targetAt) / DAY_MS);
  while (shiftedInstant(target, d + 1) <= now) d++;
  while (d > 0 && shiftedInstant(target, d) > now) d--;
  const boundary = shiftedInstant(target, d);
  return {
    status: d === 0 ? 'arrived' : 'past',
    days: d,
    ...split(now - boundary),
    totalMs: now - targetAt,
    nextChangeAt: shiftedInstant(target, d + 1),
  };
}

/** Share of the way from `startAt` to the target, 0…1 (1 once the target has passed). */
export function progress(target: LocalDateTime, startAt: number, now: number): number {
  const targetAt = toInstant(target);
  if (now >= targetAt) return 1;
  if (targetAt <= startAt) return 0;
  return Math.min(1, Math.max(0, (now - startAt) / (targetAt - startAt)));
}

export interface Ymd {
  years: number;
  months: number;
  days: number;
}

const daysInMonth = (year: number, month: number) => new Date(year, month, 0).getDate();

interface DateOnly {
  year: number;
  month: number;
  day: number;
}

/** Day number in UTC — date arithmetic free of the local DST hour. */
const utcDay = (d: DateOnly) => Date.UTC(d.year, d.month - 1, d.day) / DAY_MS;

/** `a` plus whole months, clamped to the month's last day (31 Jan + 1 month = 28/29 Feb). */
function addMonths(a: DateOnly, months: number): DateOnly {
  const total = a.year * 12 + (a.month - 1) + months;
  const year = Math.floor(total / 12);
  const month = (total % 12) + 1;
  return { year, month, day: Math.min(a.day, daysInMonth(year, month)) };
}

/**
 * Calendar span between two dates (a <= b), as a person would say it: 5 Oct → 5 Sep next year is
 * "11 months", 23 Oct → 5 Sep is "10 months and 13 days", 31 Jan → 1 Mar is "1 month and 1 day"
 * (the most whole months that fit, each landing on the same day-of-month or the month's last day,
 * then the leftover days).
 */
export function calendarSpan(a: DateOnly, b: DateOnly): Ymd {
  let months = Math.max(0, (b.year - a.year) * 12 + (b.month - a.month));
  while (months > 0 && utcDay(addMonths(a, months)) > utcDay(b)) months--;
  const days = Math.max(0, utcDay(b) - utcDay(addMonths(a, months)));
  return { years: Math.floor(months / 12), months: months % 12, days };
}

/** The Y/M/D span covered by `r.days` — from the date the current day count started (or ends) to
 * the target's date. */
export function spanOf(target: LocalDateTime, r: Remaining): Ymd {
  const from = new Date(shiftedInstant(target, r.status === 'upcoming' ? -r.days : 0));
  const to = new Date(shiftedInstant(target, r.status === 'upcoming' ? 0 : r.days));
  return calendarSpan(
    { year: from.getFullYear(), month: from.getMonth() + 1, day: from.getDate() },
    { year: to.getFullYear(), month: to.getMonth() + 1, day: to.getDate() },
  );
}

/** Weekday of a target, 0 = Sunday. */
export const weekdayOf = (t: LocalDateTime): number => new Date(t.year, t.month - 1, t.day).getDay();

export const isValidDate = (year: number, month: number, day: number): boolean =>
  Number.isInteger(year) && month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month);

export { daysInMonth };
