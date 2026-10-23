import { useEffect } from 'react';
import { AppState } from 'react-native';
import { create } from 'zustand';

/** The app's one clock. Every countdown on screen reads `now` from here, so they all tick on the
 * same frame instead of each running its own interval. */
export const useClock = create<{ now: number }>(() => ({ now: Date.now() }));

export const useNow = () => useClock(s => s.now);

/**
 * Mounted once (App.tsx). Ticks on whole seconds — or whole minutes when Settings → "Show seconds"
 * is off — aligned to the wall clock, so a countdown to 00:00:00 flips exactly on the second.
 * Stops while the app is in the background and catches up the moment it returns.
 */
export function useClockDriver(everySecond: boolean) {
  useEffect(() => {
    const step = everySecond ? 1000 : 60_000;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const tick = () => {
      const now = Date.now();
      useClock.setState({ now });
      // +5 ms so the timer lands just after the boundary, never a hair before it.
      timer = setTimeout(tick, step - (now % step) + 5);
    };
    const start = () => {
      if (!timer) tick();
    };
    const stop = () => {
      if (timer) clearTimeout(timer);
      timer = null;
    };

    if (AppState.currentState !== 'background') start();
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active') start();
      else if (state === 'background') stop();
    });
    return () => {
      stop();
      sub.remove();
    };
  }, [everySecond]);
}
