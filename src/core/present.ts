import type { TFunction } from 'i18next';

import type { Remaining } from './countdown';
import { formatClock, formatLongDate, formatShortDate } from './format';
import type { Countdown } from './types';
import { countPhrase, type Unit } from '../i18n/plural';
import type { Language } from '../i18n/language';

/** The countdown's name, or its date when it has none ("Sunday, 5 September 2027"). */
export const countdownTitle = (c: Countdown, lang: Language): string => c.title || formatLongDate(c.target, lang);

/** The line under the title: the date (unless the title already is the date) and, when the target
 * isn't midnight, its time. */
export function countdownSubtitle(c: Countdown, lang: Language, use24h: boolean, t: TFunction, short = false): string {
  const parts: string[] = [];
  if (c.title) parts.push(short ? formatShortDate(c.target, lang) : formatLongDate(c.target, lang));
  if (c.target.hour !== 0 || c.target.minute !== 0) {
    parts.push(t('countdown.at', { time: formatClock(c.target.hour, c.target.minute, lang, use24h) }));
  }
  return parts.join(' · ');
}

/** The biggest non-zero unit — "316 days", or "5 hours" on the last day. */
export function headline(r: Remaining): { value: number; unit: Unit } {
  if (r.days > 0) return { value: r.days, unit: 'day' };
  if (r.hours > 0) return { value: r.hours, unit: 'hour' };
  if (r.minutes > 0) return { value: r.minutes, unit: 'minute' };
  return { value: r.seconds, unit: 'second' };
}

/** "in 316 days" / "بعد ٣١٦ يومًا", "3 days ago" / "منذ ٣ أيام", "Today" / "اليوم". */
export function relativePhrase(r: Remaining, lang: Language, t: TFunction): string {
  if (r.status === 'arrived') return t('countdown.today');
  const { value, unit } = headline(r);
  const phrase = countPhrase(unit, value, lang);
  return r.status === 'upcoming' ? t('countdown.in', { phrase }) : t('countdown.ago', { phrase });
}
