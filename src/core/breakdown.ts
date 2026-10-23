import { HOUR_MS, spanOf, type Remaining } from './countdown';
import type { LocalDateTime } from './types';
import { countPhrase, formatNumber, joinParts } from '../i18n/plural';
import type { Language } from '../i18n/language';

/** "10 months and 13 days" — the calendar span of the day count (empty when under a day). */
export function spanPhrase(target: LocalDateTime, r: Remaining, lang: Language): string {
  const { years, months, days } = spanOf(target, r);
  const parts: string[] = [];
  if (years) parts.push(countPhrase('year', years, lang));
  if (months) parts.push(countPhrase('month', months, lang));
  if (days) parts.push(countPhrase('day', days, lang));
  return joinParts(parts, lang);
}

/** "45 weeks and 1 day" (empty under a week). */
export function weeksPhrase(r: Remaining, lang: Language): string {
  const weeks = Math.floor(r.days / 7);
  if (weeks === 0) return '';
  const rest = r.days % 7;
  return joinParts([countPhrase('week', weeks, lang), ...(rest ? [countPhrase('day', rest, lang)] : [])], lang);
}

/** Whole hours in total, grouped: "7,593". */
export const totalHours = (r: Remaining, lang: Language): string => formatNumber(Math.floor(r.totalMs / HOUR_MS), lang);
