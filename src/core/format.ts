import { weekdayOf } from './countdown';
import type { LocalDateTime } from './types';
import type { Language } from '../i18n/language';

const ARABIC_INDIC = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];

/** Arabic UI shows Arabic-Indic digits (٢٠٢٧), like the sibling apps. */
export function localizeDigits(value: string | number, lang: Language): string {
  const s = String(value);
  return lang === 'ar' ? s.replace(/[0-9]/g, d => ARABIC_INDIC[Number(d)]) : s;
}

/** Levantine month names (كانون الثاني…), as Lebanese calendars print them — same as the Hijri app. */
const MONTHS_AR = [
  'كانون الثاني',
  'شباط',
  'آذار',
  'نيسان',
  'أيار',
  'حزيران',
  'تمّوز',
  'آب',
  'أيلول',
  'تشرين الأول',
  'تشرين الثاني',
  'كانون الأول',
] as const;
const MONTHS_EN = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
] as const;
const MONTHS_EN_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'] as const;
const WEEKDAYS_AR = ['الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة', 'السبت'] as const;
const WEEKDAYS_EN = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'] as const;

export const monthName = (month: number, lang: Language): string => (lang === 'ar' ? MONTHS_AR : MONTHS_EN)[month - 1];

export const weekdayName = (weekday: number, lang: Language): string => (lang === 'ar' ? WEEKDAYS_AR : WEEKDAYS_EN)[weekday];

/** "Sunday, 5 September 2027" / "الأحد ٥ أيلول ٢٠٢٧". */
export function formatLongDate(t: LocalDateTime, lang: Language): string {
  const weekday = weekdayName(weekdayOf(t), lang);
  return lang === 'ar'
    ? localizeDigits(`${weekday} ${t.day} ${monthName(t.month, lang)} ${t.year}`, lang)
    : `${weekday}, ${t.day} ${monthName(t.month, lang)} ${t.year}`;
}

/** "5 Sep 2027" / "٥ أيلول ٢٠٢٧" — Arabic has no customary short month names. */
export function formatShortDate(t: LocalDateTime, lang: Language): string {
  return lang === 'ar'
    ? localizeDigits(`${t.day} ${monthName(t.month, lang)} ${t.year}`, lang)
    : `${t.day} ${MONTHS_EN_SHORT[t.month - 1]} ${t.year}`;
}

export const pad2 = (n: number): string => String(n).padStart(2, '0');

/** "00:00" (24h) or "12:00 AM" / "١٢:٠٠ ص" (12h). */
export function formatClock(hour: number, minute: number, lang: Language, use24h: boolean): string {
  if (use24h) return localizeDigits(`${pad2(hour)}:${pad2(minute)}`, lang);
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  const suffix = lang === 'ar' ? (hour < 12 ? 'ص' : 'م') : hour < 12 ? 'AM' : 'PM';
  return localizeDigits(`${h12}:${pad2(minute)} ${suffix}`, lang);
}

/** "62%" / "٦٢٪". */
export function formatPercent(percent: number, lang: Language): string {
  return lang === 'ar' ? `${localizeDigits(percent, lang)}٪` : `${percent}%`;
}
