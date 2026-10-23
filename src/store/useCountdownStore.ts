import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { isValidDate, toInstant } from '../core/countdown';
import {
  isCountdownColor,
  isCountdownIcon,
  type Countdown,
  type CountdownColor,
  type CountdownIcon,
  type LocalDateTime,
} from '../core/types';

export interface CountdownInput {
  title: string;
  target: LocalDateTime;
  color: CountdownColor;
  icon: CountdownIcon;
}

/** The user's own example (2026-10-23): count down to 5 September 2027. Seeded on first launch. */
export const SEED_TARGET: LocalDateTime = { year: 2027, month: 9, day: 5, hour: 0, minute: 0 };

interface CountdownState {
  countdowns: Countdown[];
  /** The countdown on the Home hero card and the widget. Always set while any countdown exists. */
  pinnedId: string | null;
  /** The seed is added once, so deleting every countdown leaves an empty list instead of reviving it. */
  seeded: boolean;
  hasHydrated: boolean;
  add: (input: CountdownInput, pin?: boolean) => string;
  update: (id: string, input: CountdownInput) => void;
  remove: (id: string) => void;
  pin: (id: string) => void;
  removeAll: () => void;
}

const newId = () => `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;

const isLocalDateTime = (t: unknown): t is LocalDateTime => {
  const v = t as LocalDateTime | null;
  return (
    !!v &&
    isValidDate(v.year, v.month, v.day) &&
    Number.isInteger(v.hour) &&
    v.hour >= 0 &&
    v.hour <= 23 &&
    Number.isInteger(v.minute) &&
    v.minute >= 0 &&
    v.minute <= 59
  );
};

function sanitize(list: unknown): Countdown[] {
  if (!Array.isArray(list)) return [];
  return list.flatMap((c: Partial<Countdown>) =>
    c && typeof c.id === 'string' && isLocalDateTime(c.target)
      ? [
          {
            id: c.id,
            title: typeof c.title === 'string' ? c.title : '',
            target: c.target,
            color: isCountdownColor(c.color) ? c.color : 'ember',
            icon: isCountdownIcon(c.icon) ? c.icon : 'calendar',
            createdAt: typeof c.createdAt === 'number' ? c.createdAt : Date.now(),
          },
        ]
      : [],
  );
}

/** Upcoming first (soonest on top), then passed ones (most recent first). */
export function sortCountdowns(list: Countdown[], now: number): Countdown[] {
  return [...list].sort((a, b) => {
    const ta = toInstant(a.target);
    const tb = toInstant(b.target);
    const pa = ta <= now;
    const pb = tb <= now;
    if (pa !== pb) return pa ? 1 : -1;
    return pa ? tb - ta : ta - tb;
  });
}

export const useCountdownStore = create<CountdownState>()(
  persist(
    (set, get) => ({
      countdowns: [],
      pinnedId: null,
      seeded: false,
      hasHydrated: false,
      add: (input, pin = false) => {
        const id = newId();
        const countdown: Countdown = { id, ...input, title: input.title.trim(), createdAt: Date.now() };
        set(s => ({
          countdowns: [...s.countdowns, countdown],
          pinnedId: pin || !s.pinnedId ? id : s.pinnedId,
        }));
        return id;
      },
      update: (id, input) =>
        set(s => ({
          countdowns: s.countdowns.map(c => (c.id === id ? { ...c, ...input, title: input.title.trim() } : c)),
        })),
      remove: id => {
        const rest = get().countdowns.filter(c => c.id !== id);
        const pinnedId = get().pinnedId;
        set({
          countdowns: rest,
          // Losing the pinned one hands the widget to the next countdown due.
          pinnedId: pinnedId === id ? sortCountdowns(rest, Date.now())[0]?.id ?? null : pinnedId,
        });
      },
      pin: id => {
        if (get().countdowns.some(c => c.id === id)) set({ pinnedId: id });
      },
      removeAll: () => set({ countdowns: [], pinnedId: null }),
    }),
    {
      name: 'days-counter-countdowns',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: ({ countdowns, pinnedId, seeded }) => ({ countdowns, pinnedId, seeded }),
      merge: (persisted, current) => {
        const p = (persisted ?? {}) as Partial<CountdownState>;
        const countdowns = sanitize(p.countdowns);
        const pinnedId = countdowns.some(c => c.id === p.pinnedId) ? p.pinnedId! : countdowns[0]?.id ?? null;
        return { ...current, countdowns, pinnedId, seeded: p.seeded === true };
      },
      onRehydrateStorage: () => () => {
        const s = useCountdownStore.getState();
        if (!s.seeded) {
          useCountdownStore.setState({ seeded: true });
          if (s.countdowns.length === 0) s.add({ title: '', target: SEED_TARGET, color: 'ember', icon: 'calendar' }, true);
        }
        useCountdownStore.setState({ hasHydrated: true });
      },
    },
  ),
);

/** The pinned countdown, or null when the list is empty. */
export const selectPinned = (s: CountdownState): Countdown | null =>
  s.countdowns.find(c => c.id === s.pinnedId) ?? s.countdowns[0] ?? null;
