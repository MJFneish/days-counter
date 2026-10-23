import type { TFunction } from 'i18next';

import { countdownSubtitle, countdownTitle } from '../core/present';
import type { Countdown, CountdownColor, LocalDateTime } from '../core/types';
import type { Language } from '../i18n/language';

/**
 * What the app hands the widget. The widget computes the countdown itself (`Countdown.kt`, a port
 * of `remaining()`), so this only changes when the pinned countdown, the language or the count-up
 * setting does — not every day. Keep in sync with `WidgetStore.kt`.
 */
export interface WidgetPayload {
  v: 1;
  lang: Language;
  rtl: boolean;
  countUp: boolean;
  countdown: {
    title: string;
    /** Second line: the date (when the title isn't already the date) and the time. */
    detail: string;
    target: LocalDateTime;
    createdAt: number;
    color: CountdownColor;
  } | null;
  strings: {
    toGo: string;
    since: string;
    arrived: string;
    passed: string;
    empty: string;
    /** "{p} of the wait done" — the widget substitutes its own formatted percent for {p}. */
    progress: string;
  };
}

export function buildWidgetPayload(pinned: Countdown | null, lang: Language, countUp: boolean, use24h: boolean, t: TFunction): WidgetPayload {
  return {
    v: 1,
    lang,
    rtl: lang === 'ar',
    countUp,
    countdown: pinned
      ? {
          title: countdownTitle(pinned, lang),
          detail: countdownSubtitle(pinned, lang, use24h, t, true),
          target: pinned.target,
          createdAt: pinned.createdAt,
          color: pinned.color,
        }
      : null,
    strings: {
      toGo: t('countdown.toGo'),
      since: t('countdown.since'),
      arrived: t('countdown.arrived'),
      passed: t('countdown.doneNoCountUp'),
      empty: t('home.emptyTitle'),
      progress: t('countdown.progress', { percent: '{p}' }),
    },
  };
}
