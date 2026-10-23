import { localizeDigits } from '../core/format';
import type { Language } from './language';

/**
 * Counted phrases ("3 days", "٣ أيام") without i18next's plural keys — those rely on
 * `Intl.PluralRules`, which Hermes doesn't reliably provide, and Arabic needs all six CLDR forms
 * (zero/one/two/few/many/other) to read naturally. Same approach as the Hijri converter; the
 * widget does the same natively through Android `<plurals>` (values-ar/strings.xml).
 */
export type Unit = 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second';

type ArabicForms = { one: string; two: string; few: string; many: string; other: string };

/** Full phrases: "سنة واحدة", "سنتان", "٣ سنوات", "١١ سنة"… */
const AR: Record<Unit, ArabicForms> = {
  year: { one: 'سنة واحدة', two: 'سنتان', few: '{n} سنوات', many: '{n} سنة', other: '{n} سنة' },
  month: { one: 'شهر واحد', two: 'شهران', few: '{n} أشهر', many: '{n} شهرًا', other: '{n} شهر' },
  week: { one: 'أسبوع واحد', two: 'أسبوعان', few: '{n} أسابيع', many: '{n} أسبوعًا', other: '{n} أسبوع' },
  day: { one: 'يوم واحد', two: 'يومان', few: '{n} أيام', many: '{n} يومًا', other: '{n} يوم' },
  hour: { one: 'ساعة واحدة', two: 'ساعتان', few: '{n} ساعات', many: '{n} ساعة', other: '{n} ساعة' },
  minute: { one: 'دقيقة واحدة', two: 'دقيقتان', few: '{n} دقائق', many: '{n} دقيقة', other: '{n} دقيقة' },
  second: { one: 'ثانية واحدة', two: 'ثانيتان', few: '{n} ثوانٍ', many: '{n} ثانية', other: '{n} ثانية' },
};

/** The noun alone, for a label under a big number: "١٢" over "يومًا". */
const AR_WORD: Record<Unit, ArabicForms> = {
  year: { one: 'سنة', two: 'سنتان', few: 'سنوات', many: 'سنة', other: 'سنة' },
  month: { one: 'شهر', two: 'شهران', few: 'أشهر', many: 'شهرًا', other: 'شهر' },
  week: { one: 'أسبوع', two: 'أسبوعان', few: 'أسابيع', many: 'أسبوعًا', other: 'أسبوع' },
  day: { one: 'يوم', two: 'يومان', few: 'أيام', many: 'يومًا', other: 'يوم' },
  hour: { one: 'ساعة', two: 'ساعتان', few: 'ساعات', many: 'ساعة', other: 'ساعة' },
  minute: { one: 'دقيقة', two: 'دقيقتان', few: 'دقائق', many: 'دقيقة', other: 'دقيقة' },
  second: { one: 'ثانية', two: 'ثانيتان', few: 'ثوانٍ', many: 'ثانية', other: 'ثانية' },
};

const EN: Record<Unit, [string, string]> = {
  year: ['year', 'years'],
  month: ['month', 'months'],
  week: ['week', 'weeks'],
  day: ['day', 'days'],
  hour: ['hour', 'hours'],
  minute: ['minute', 'minutes'],
  second: ['second', 'seconds'],
};

/** "9,780" / "٩٬٧٨٠". */
export function formatNumber(n: number, lang: Language): string {
  const grouped = String(Math.abs(Math.trunc(n))).replace(/\B(?=(\d{3})+(?!\d))/g, lang === 'ar' ? '٬' : ',');
  return localizeDigits((n < 0 ? '-' : '') + grouped, lang);
}

export function arabicForm(n: number): keyof ArabicForms {
  const mod100 = n % 100;
  if (n === 1) return 'one';
  if (n === 2) return 'two';
  if (mod100 >= 3 && mod100 <= 10) return 'few';
  if (mod100 >= 11 && mod100 <= 99) return 'many';
  return 'other';
}

export function countPhrase(unit: Unit, n: number, lang: Language): string {
  if (lang === 'ar') {
    if (n === 0) return `${localizeDigits(0, 'ar')} ${AR_WORD[unit].other}`;
    return AR[unit][arabicForm(n)].replace('{n}', formatNumber(n, 'ar'));
  }
  return `${formatNumber(n, 'en')} ${EN[unit][n === 1 ? 0 : 1]}`;
}

/** Unit word that agrees with `n`: "day"/"days", "يوم"/"يومان"/"أيام"/"يومًا". */
export function unitWord(unit: Unit, n: number, lang: Language): string {
  if (lang === 'ar') return n === 0 ? AR_WORD[unit].other : AR_WORD[unit][arabicForm(n)];
  return EN[unit][n === 1 ? 0 : 1];
}

/** Joins non-zero parts: "2 years, 3 months and 5 days" / "سنتان و٣ أشهر و٥ أيام". */
export function joinParts(parts: string[], lang: Language): string {
  if (parts.length <= 1) return parts.join('');
  if (lang === 'ar') return parts.join(' و');
  return `${parts.slice(0, -1).join(', ')} and ${parts[parts.length - 1]}`;
}
