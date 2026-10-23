import type { TFunction } from 'i18next';

import en from '../../i18n/locales/en.json';
import ar from '../../i18n/locales/ar.json';
import { remaining } from '../countdown';
import { countdownSubtitle, countdownTitle, headline, relativePhrase } from '../present';
import type { Countdown } from '../types';
import { buildWidgetPayload } from '../../widget/payload';

/** A minimal `t` over the real locale files, with {{var}} interpolation — enough to check the
 * strings the screens and the widget get. */
function makeT(dict: Record<string, unknown>): TFunction {
  return ((key: string, vars?: Record<string, string>) => {
    const value = key.split('.').reduce<unknown>((node, part) => (node as Record<string, unknown>)?.[part], dict);
    if (typeof value !== 'string') return key;
    return value.replace(/\{\{(\w+)\}\}/g, (_, name) => vars?.[name] ?? '');
  }) as unknown as TFunction;
}
const tEn = makeT(en);
const tAr = makeT(ar);

const at = (y: number, m: number, d: number, h = 0, min = 0) => new Date(y, m - 1, d, h, min).getTime();

const seed: Countdown = {
  id: 'seed',
  title: '',
  target: { year: 2027, month: 9, day: 5, hour: 0, minute: 0 },
  color: 'ember',
  icon: 'calendar',
  createdAt: at(2026, 10, 23),
};
const named: Countdown = { ...seed, id: 'named', title: 'Graduation', target: { ...seed.target, hour: 18, minute: 30 } };

describe('titles', () => {
  it('an unnamed countdown is titled by its date and has no subtitle at midnight', () => {
    expect(countdownTitle(seed, 'en')).toBe('Sunday, 5 September 2027');
    expect(countdownSubtitle(seed, 'en', true, tEn)).toBe('');
  });

  it('a named one shows the date and a non-midnight time underneath', () => {
    expect(countdownTitle(named, 'en')).toBe('Graduation');
    expect(countdownSubtitle(named, 'en', true, tEn)).toBe('Sunday, 5 September 2027 · at 18:30');
    expect(countdownSubtitle(named, 'en', false, tEn, true)).toBe('5 Sep 2027 · at 6:30 PM');
    expect(countdownSubtitle(named, 'ar', false, tAr, true)).toBe('٥ أيلول ٢٠٢٧ · الساعة ٦:٣٠ م');
  });
});

describe('relative phrases', () => {
  it('days, then hours on the last day', () => {
    expect(relativePhrase(remaining(seed.target, at(2026, 10, 23, 15)), 'en', tEn)).toBe('in 316 days');
    expect(relativePhrase(remaining(seed.target, at(2027, 9, 4, 19)), 'en', tEn)).toBe('in 5 hours');
    expect(headline(remaining(seed.target, at(2027, 9, 4, 23, 59)))).toEqual({ value: 1, unit: 'minute' });
  });

  it('today, then ago', () => {
    expect(relativePhrase(remaining(seed.target, at(2027, 9, 5, 9)), 'en', tEn)).toBe('Today');
    expect(relativePhrase(remaining(seed.target, at(2027, 9, 8, 9)), 'en', tEn)).toBe('3 days ago');
  });

  it('Arabic', () => {
    expect(relativePhrase(remaining(seed.target, at(2026, 10, 23, 15)), 'ar', tAr)).toBe('بعد ٣١٦ يومًا');
    expect(relativePhrase(remaining(seed.target, at(2027, 9, 8, 9)), 'ar', tAr)).toBe('منذ ٣ أيام');
  });
});

describe('widget payload', () => {
  it('carries the wall-clock target and the localized strings', () => {
    const p = buildWidgetPayload(named, 'ar', true, true, tAr);
    expect(p).toMatchObject({
      v: 1,
      lang: 'ar',
      rtl: true,
      countUp: true,
      countdown: { title: 'Graduation', target: named.target, color: 'ember', createdAt: named.createdAt },
    });
    expect(p.countdown?.detail).toBe('٥ أيلول ٢٠٢٧ · الساعة ١٨:٣٠');
    expect(p.strings.progress).toBe('مضى {p} من الانتظار');
    expect(p.strings.toGo).toBe(ar.countdown.toGo);
  });

  it('is empty-but-valid with no countdown', () => {
    const p = buildWidgetPayload(null, 'en', false, true, tEn);
    expect(p.countdown).toBeNull();
    expect(p.strings.empty).toBe(en.home.emptyTitle);
    expect(JSON.parse(JSON.stringify(p))).toEqual(p);
  });
});

describe('locales', () => {
  /** Every key path in one locale exists in the other. */
  const paths = (node: unknown, prefix = ''): string[] =>
    typeof node === 'object' && node !== null
      ? Object.entries(node).flatMap(([k, v]) => paths(v, prefix ? `${prefix}.${k}` : k))
      : [prefix];

  it('en.json and ar.json have the same keys', () => {
    expect(paths(ar).sort()).toEqual(paths(en).sort());
  });
});
