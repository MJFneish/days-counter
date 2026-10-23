import {
  calendarSpan,
  DAY_MS,
  HOUR_MS,
  MINUTE_MS,
  progress,
  remaining,
  shiftedInstant,
  spanOf,
  toInstant,
} from '../countdown';
import { spanPhrase, weeksPhrase } from '../breakdown';
import { formatClock, formatLongDate, formatShortDate } from '../format';
import { countPhrase, unitWord } from '../../i18n/plural';
import type { LocalDateTime } from '../types';

const at = (year: number, month: number, day: number, hour = 0, minute = 0, second = 0) =>
  new Date(year, month - 1, day, hour, minute, second).getTime();
const dt = (year: number, month: number, day: number, hour = 0, minute = 0): LocalDateTime => ({ year, month, day, hour, minute });

const USER_TARGET = dt(2027, 9, 5);

describe('time zone', () => {
  it('runs in Asia/Beirut (jest.globalSetup.js)', () => {
    // Lebanon: UTC+3 in summer, UTC+2 in winter.
    expect(new Date(2027, 6, 1).getTimezoneOffset()).toBe(-180);
    expect(new Date(2027, 0, 1).getTimezoneOffset()).toBe(-120);
  });
});

describe('remaining — the user example (5 Sep 2027)', () => {
  it('from 23 Oct 2026 15:00 → 316 days 9:00:00', () => {
    const r = remaining(USER_TARGET, at(2026, 10, 23, 15));
    expect(r).toMatchObject({ status: 'upcoming', days: 316, hours: 9, minutes: 0, seconds: 0 });
    expect(r.nextChangeAt).toBe(at(2026, 10, 24));
  });

  it('from 2026-10-23 00:00 → exactly 317 days', () => {
    expect(remaining(USER_TARGET, at(2026, 10, 23))).toMatchObject({ days: 317, hours: 0, minutes: 0, seconds: 0 });
  });

  it('the day count drops by one at every local midnight, the whole way there', () => {
    let previous = remaining(USER_TARGET, at(2026, 10, 23, 12)).days;
    const notMidnight: string[] = [];
    for (let k = 1; k <= 316; k++) {
      const boundary = shiftedInstant(USER_TARGET, -(317 - k));
      expect(remaining(USER_TARGET, boundary - 1000).days).toBe(previous);
      const after = remaining(USER_TARGET, boundary + 1000);
      expect(after.days).toBe(previous - 1);
      previous = after.days;
      const b = new Date(boundary);
      if (b.getHours() !== 0) notMidnight.push(`${b.getFullYear()}-${b.getMonth() + 1}-${b.getDate()} ${b.getHours()}:00`);
    }
    expect(previous).toBe(0);
    // The only midnight that doesn't exist: Lebanon springs forward 00:00 → 01:00 that night.
    expect(notMidnight).toEqual(['2027-3-28 1:00']);
  });

  it('h/m/s always add up to the time left before the next day change', () => {
    for (let now = at(2026, 10, 23, 15, 7, 31); now < toInstant(USER_TARGET); now += 7 * HOUR_MS + 13 * MINUTE_MS + 17_000) {
      const r = remaining(USER_TARGET, now);
      const shown = (r.hours * 3600 + r.minutes * 60 + r.seconds) * 1000;
      expect(Math.floor((r.nextChangeAt - now) / 1000) * 1000).toBe(shown);
      expect(r.minutes).toBeLessThan(60);
      expect(r.seconds).toBeLessThan(60);
      expect(r.hours).toBeLessThanOrEqual(24);
    }
  });
});

describe('remaining — edges', () => {
  const target = dt(2027, 1, 1, 18, 30);

  it('one second before → 0 days 0:00:01', () => {
    expect(remaining(target, toInstant(target) - 1000)).toMatchObject({ status: 'upcoming', days: 0, hours: 0, minutes: 0, seconds: 1 });
  });

  it('truncates seconds like Android Chronometer', () => {
    expect(remaining(target, toInstant(target) - 1999).seconds).toBe(1);
    expect(remaining(target, toInstant(target) - 999).seconds).toBe(0);
  });

  it('at the instant → arrived, 0 elapsed; next change one day later', () => {
    const r = remaining(target, toInstant(target));
    expect(r).toMatchObject({ status: 'arrived', days: 0, hours: 0, minutes: 0, seconds: 0, totalMs: 0 });
    expect(r.nextChangeAt).toBe(at(2027, 1, 2, 18, 30));
  });

  it('counts up after: 3 days 2 h later → past, 3 days 2:00:00', () => {
    expect(remaining(target, at(2027, 1, 4, 20, 30))).toMatchObject({ status: 'past', days: 3, hours: 2, minutes: 0, seconds: 0 });
  });

  it('a non-midnight target flips the day count at its own time of day', () => {
    expect(remaining(target, at(2026, 12, 30, 18, 29, 59)).days).toBe(2);
    expect(remaining(target, at(2026, 12, 30, 18, 30, 1)).days).toBe(1);
  });

  it('leap day: 28 Feb 2027 → 29 Feb 2028 is 366 days', () => {
    expect(remaining(dt(2028, 2, 29), at(2027, 2, 28)).days).toBe(366);
  });
});

describe('remaining — DST in Lebanon', () => {
  it('autumn (25 Oct 2026, 00:00 → 23:00): days still flip at midnight, the long day shows 13 h from noon', () => {
    const target = dt(2026, 12, 1);
    const r = remaining(target, at(2026, 10, 24, 12));
    // 24 Oct → 1 Dec is 38 calendar days; from noon, 37 full days + the rest of the 25-hour day.
    expect(r).toMatchObject({ days: 37, hours: 13, minutes: 0, seconds: 0 });
    expect(new Date(r.nextChangeAt).getHours()).toBe(0);
  });

  it('spring (28 Mar 2027, 00:00 → 01:00): the skipped midnight resolves to 01:00', () => {
    const target = dt(2027, 4, 1);
    const r = remaining(target, at(2027, 3, 27, 12));
    expect(r.days).toBe(4);
    expect(new Date(r.nextChangeAt).getHours()).toBe(1);
    expect(r.hours).toBe(12);
    // A naive 24-hour count would say 4 days 11 h here.
    expect(Math.floor((toInstant(target) - at(2027, 3, 27, 12)) / DAY_MS)).toBe(4);
  });
});

describe('progress', () => {
  it('runs 0 → 1 from creation to the target', () => {
    const start = at(2026, 10, 23);
    const end = toInstant(USER_TARGET);
    expect(progress(USER_TARGET, start, start)).toBe(0);
    expect(progress(USER_TARGET, start, (start + end) / 2)).toBeCloseTo(0.5);
    expect(progress(USER_TARGET, start, end + 1)).toBe(1);
    expect(progress(USER_TARGET, end + 5, end - 5)).toBe(0);
  });
});

describe('calendar span', () => {
  it('23 Oct 2026 → 5 Sep 2027 = 10 months 13 days', () => {
    expect(calendarSpan({ year: 2026, month: 10, day: 23 }, { year: 2027, month: 9, day: 5 })).toEqual({ years: 0, months: 10, days: 13 });
  });
  it('31 Jan → 1 Mar 2027 = 1 month 1 day', () => {
    expect(calendarSpan({ year: 2027, month: 1, day: 31 }, { year: 2027, month: 3, day: 1 })).toEqual({ years: 0, months: 1, days: 1 });
  });
  it('spanOf matches the day count', () => {
    const r = remaining(USER_TARGET, at(2026, 10, 23, 15));
    // 316 days counted from 24 Oct → 5 Sep.
    expect(spanOf(USER_TARGET, r)).toEqual({ years: 0, months: 10, days: 12 });
  });
});

describe('phrases', () => {
  const r = remaining(USER_TARGET, at(2026, 10, 23, 15));

  it('English', () => {
    expect(spanPhrase(USER_TARGET, r, 'en')).toBe('10 months and 12 days');
    expect(weeksPhrase(r, 'en')).toBe('45 weeks and 1 day');
    expect(countPhrase('day', 1, 'en')).toBe('1 day');
    expect(unitWord('second', 1, 'en')).toBe('second');
    expect(formatLongDate(USER_TARGET, 'en')).toBe('Sunday, 5 September 2027');
    expect(formatShortDate(USER_TARGET, 'en')).toBe('5 Sep 2027');
    expect(formatClock(18, 5, 'en', false)).toBe('6:05 PM');
    expect(formatClock(0, 0, 'en', true)).toBe('00:00');
  });

  it('Arabic plural forms', () => {
    expect(countPhrase('day', 1, 'ar')).toBe('يوم واحد');
    expect(countPhrase('day', 2, 'ar')).toBe('يومان');
    expect(countPhrase('day', 5, 'ar')).toBe('٥ أيام');
    expect(countPhrase('day', 12, 'ar')).toBe('١٢ يومًا');
    expect(countPhrase('day', 316, 'ar')).toBe('٣١٦ يومًا');
    expect(countPhrase('day', 300, 'ar')).toBe('٣٠٠ يوم');
    expect(countPhrase('hour', 0, 'ar')).toBe('٠ ساعة');
    expect(unitWord('day', 316, 'ar')).toBe('يومًا');
    expect(unitWord('minute', 7, 'ar')).toBe('دقائق');
    expect(spanPhrase(USER_TARGET, r, 'ar')).toBe('١٠ أشهر و١٢ يومًا');
    expect(formatLongDate(USER_TARGET, 'ar')).toBe('الأحد ٥ أيلول ٢٠٢٧');
    expect(formatClock(18, 5, 'ar', false)).toBe('٦:٠٥ م');
  });
});
