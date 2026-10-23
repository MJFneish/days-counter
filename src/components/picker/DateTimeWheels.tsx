import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';

import { daysInMonth } from '../../core/countdown';
import { localizeDigits, monthName, pad2 } from '../../core/format';
import type { LocalDateTime } from '../../core/types';
import { useDirection } from '../../hooks/useDirection';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';
import { WheelPicker } from './WheelPicker';

export const YEAR_MIN = 1900;
export const YEAR_MAX = 2100;

const YEARS = Array.from({ length: YEAR_MAX - YEAR_MIN + 1 }, (_, i) => YEAR_MIN + i);
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);
const HOURS = Array.from({ length: 24 }, (_, i) => i);
const MINUTES = Array.from({ length: 60 }, (_, i) => i);

interface Props {
  value: LocalDateTime;
  onChange: (value: LocalDateTime) => void;
  /** Background behind the wheels — their fades blend into it. */
  fadeColor: string;
}

function WheelColumn({ label, flex, children }: { label: string; flex: number; children: React.ReactNode }) {
  const { colors } = useTheme();
  return (
    <View style={{ flex }}>
      <Text style={[typography.caption, styles.columnLabel, { color: colors.textMuted }]}>{label}</Text>
      {children}
    </View>
  );
}

/** Day / month / year wheels. A day past the new month's end is clamped (31 → 30 Sep). Mirrors in
 * Arabic, so the day sits on the right as Arabic dates are read. */
export function DateWheels({ value, onChange, fadeColor }: Props) {
  const { t } = useTranslation();
  const { lang, rowDirection } = useDirection();
  const dayCount = daysInMonth(value.year, value.month);

  const dayItems = useMemo(() => Array.from({ length: dayCount }, (_, i) => localizeDigits(i + 1, lang)), [dayCount, lang]);
  const monthItems = useMemo(() => MONTHS.map(m => monthName(m, lang)), [lang]);
  const yearItems = useMemo(() => YEARS.map(y => localizeDigits(y, lang)), [lang]);

  const set = (patch: Partial<LocalDateTime>) => {
    const next = { ...value, ...patch };
    next.day = Math.min(next.day, daysInMonth(next.year, next.month));
    onChange(next);
  };

  return (
    <View style={[styles.row, { flexDirection: rowDirection }]}>
      <WheelColumn label={t('editor.day')} flex={0.8}>
        <WheelPicker
          items={dayItems}
          selectedIndex={value.day - 1}
          onChange={i => set({ day: i + 1 })}
          accessibilityLabel={t('editor.day')}
          fadeColor={fadeColor}
        />
      </WheelColumn>
      <WheelColumn label={t('editor.month')} flex={1.6}>
        <WheelPicker
          items={monthItems}
          selectedIndex={value.month - 1}
          onChange={i => set({ month: i + 1 })}
          accessibilityLabel={t('editor.month')}
          fadeColor={fadeColor}
        />
      </WheelColumn>
      <WheelColumn label={t('editor.year')} flex={1}>
        <WheelPicker
          items={yearItems}
          selectedIndex={value.year - YEAR_MIN}
          onChange={i => set({ year: YEAR_MIN + i })}
          accessibilityLabel={t('editor.year')}
          fadeColor={fadeColor}
        />
      </WheelColumn>
    </View>
  );
}

/** Hour / minute wheels, 24-hour ("18") or 12-hour ("6 PM") per Settings. */
export function TimeWheels({ value, onChange, fadeColor, use24h }: Props & { use24h: boolean }) {
  const { t } = useTranslation();
  const { lang, rowDirection } = useDirection();

  const hourItems = useMemo(
    () =>
      HOURS.map(h => {
        if (use24h) return localizeDigits(pad2(h), lang);
        const h12 = h % 12 === 0 ? 12 : h % 12;
        const suffix = lang === 'ar' ? (h < 12 ? 'ص' : 'م') : h < 12 ? 'AM' : 'PM';
        return localizeDigits(`${h12} ${suffix}`, lang);
      }),
    [use24h, lang],
  );
  const minuteItems = useMemo(() => MINUTES.map(m => localizeDigits(pad2(m), lang)), [lang]);

  return (
    <View style={[styles.row, styles.timeRow, { flexDirection: rowDirection }]}>
      <WheelColumn label={t('editor.hour')} flex={1}>
        <WheelPicker
          items={hourItems}
          selectedIndex={value.hour}
          onChange={hour => onChange({ ...value, hour })}
          accessibilityLabel={t('editor.hour')}
          fadeColor={fadeColor}
        />
      </WheelColumn>
      <WheelColumn label={t('editor.minute')} flex={1}>
        <WheelPicker
          items={minuteItems}
          selectedIndex={value.minute}
          onChange={minute => onChange({ ...value, minute })}
          accessibilityLabel={t('editor.minute')}
          fadeColor={fadeColor}
        />
      </WheelColumn>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { gap: spacing.xs },
  timeRow: { paddingHorizontal: spacing.xxl },
  columnLabel: { textAlign: 'center', marginBottom: spacing.xs },
});
