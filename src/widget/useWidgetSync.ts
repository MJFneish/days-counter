import { useEffect, useRef } from 'react';
import { AppState, Platform } from 'react-native';
import { useTranslation } from 'react-i18next';

import NativeDaysWidget from '../native/NativeDaysWidget';
import { selectPinned, useCountdownStore } from '../store/useCountdownStore';
import { useSettingsStore } from '../store/useSettingsStore';
import { buildWidgetPayload } from './payload';

export const widgetAvailable = Platform.OS === 'android' && NativeDaysWidget != null;

/**
 * Mounted once (App.tsx, after hydration). Pushes the pinned countdown to the widget whenever it
 * or the language / count-up / clock setting changes, and again each time the app comes to the
 * foreground (cheap, and it repairs a widget placed while the app was closed). Same role as
 * prayer-times-lb's useWidgetSync.
 */
export function useWidgetSync(enabled: boolean) {
  const { t, i18n } = useTranslation();
  const pinned = useCountdownStore(selectPinned);
  const countUp = useSettingsStore(s => s.countUp);
  const use24h = useSettingsStore(s => s.use24h);
  const lang = i18n.language === 'ar' ? 'ar' : 'en';
  const json = enabled ? JSON.stringify(buildWidgetPayload(pinned, lang, countUp, use24h, t)) : null;
  const latest = useRef(json);
  latest.current = json;

  useEffect(() => {
    if (widgetAvailable && json) NativeDaysWidget!.setPayload(json);
  }, [json]);

  useEffect(() => {
    if (!widgetAvailable) return;
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active' && latest.current) NativeDaysWidget!.setPayload(latest.current);
    });
    return () => sub.remove();
  }, []);
}
