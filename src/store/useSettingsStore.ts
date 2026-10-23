import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { uses24HourClock } from 'react-native-localize';

import i18n, { deviceLanguage } from '../i18n';
import { isLanguage, type Language } from '../i18n/language';

export type ThemeMode = 'system' | 'light' | 'dark';

const isThemeMode = (v: unknown): v is ThemeMode => v === 'system' || v === 'light' || v === 'dark';

function deviceUses24h(): boolean {
  try {
    return uses24HourClock();
  } catch {
    return true;
  }
}

interface SettingsState {
  language: Language;
  themeMode: ThemeMode;
  /** Tick the countdown cards every second (off = minutes only, ticking once a minute). */
  showSeconds: boolean;
  /** After a date passes, count the time since it (off = just "This date has passed"). */
  countUp: boolean;
  use24h: boolean;
  /** Not persisted — flips true once AsyncStorage has been read, gating the first render. */
  hasHydrated: boolean;
  setLanguage: (language: Language) => void;
  setThemeMode: (themeMode: ThemeMode) => void;
  setShowSeconds: (value: boolean) => void;
  setCountUp: (value: boolean) => void;
  setUse24h: (value: boolean) => void;
}

/** Same pattern as prayer-times-lb and the Hijri converter: zustand + persist over AsyncStorage. */
export const useSettingsStore = create<SettingsState>()(
  persist(
    set => ({
      language: deviceLanguage,
      themeMode: 'system',
      showSeconds: true,
      countUp: true,
      use24h: deviceUses24h(),
      hasHydrated: false,
      setLanguage: language => {
        i18n.changeLanguage(language);
        set({ language });
      },
      setThemeMode: themeMode => set({ themeMode }),
      setShowSeconds: showSeconds => set({ showSeconds }),
      setCountUp: countUp => set({ countUp }),
      setUse24h: use24h => set({ use24h }),
    }),
    {
      name: 'days-counter-settings',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: ({ language, themeMode, showSeconds, countUp, use24h }) => ({
        language,
        themeMode,
        showSeconds,
        countUp,
        use24h,
      }),
      // Guards against a stored value from a future/older build that this one doesn't know.
      merge: (persisted, current) => {
        const p = (persisted ?? {}) as Partial<SettingsState>;
        return {
          ...current,
          language: isLanguage(p.language) ? p.language : current.language,
          themeMode: isThemeMode(p.themeMode) ? p.themeMode : current.themeMode,
          showSeconds: typeof p.showSeconds === 'boolean' ? p.showSeconds : current.showSeconds,
          countUp: typeof p.countUp === 'boolean' ? p.countUp : current.countUp,
          use24h: typeof p.use24h === 'boolean' ? p.use24h : current.use24h,
        };
      },
      onRehydrateStorage: () => state => {
        if (state) i18n.changeLanguage(state.language);
        // Even when the read failed, let the app start with defaults.
        useSettingsStore.setState({ hasHydrated: true });
      },
    },
  ),
);
