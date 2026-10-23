import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { CalendarClock, CalendarDays, Clock, type LucideIcon } from 'lucide-react-native';

import { Card } from '../ui/Card';
import { spanPhrase, totalHours, weeksPhrase } from '../../core/breakdown';
import { remaining } from '../../core/countdown';
import type { Countdown } from '../../core/types';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { accentOf } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

function Stat({ Icon, label, value, accent, soft }: { Icon: LucideIcon; label: string; value: string; accent: string; soft: string }) {
  const { colors } = useTheme();
  const { rowDirection, textAlign } = useDirection();
  return (
    <View style={[styles.stat, { flexDirection: rowDirection }]} accessible accessibilityLabel={`${label}: ${value}`}>
      <View style={[styles.statIcon, { backgroundColor: soft }]}>
        <Icon size={18} color={accent} />
      </View>
      <View style={styles.statText}>
        <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>{label}</Text>
        <Text style={[typography.bodyStrong, { color: colors.text, textAlign }]}>{value}</Text>
      </View>
    </View>
  );
}

/** "The wait in numbers" under the hero: the same wait on the calendar, in weeks and in hours.
 * Rows that would read "0" (under a day / a week) are left out. */
export function StatsCard({ countdown }: { countdown: Countdown }) {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { lang, textAlign } = useDirection();
  const now = useNow();
  const r = remaining(countdown.target, now);
  const { accent, soft } = accentOf(countdown.color, isDark);

  const span = spanPhrase(countdown.target, r, lang);
  const weeks = weeksPhrase(r, lang);

  return (
    <Card style={styles.card}>
      <Text style={[typography.label, { color: colors.textMuted, textAlign, letterSpacing: 0 }]}>{t('home.details')}</Text>
      {!!span && <Stat Icon={CalendarDays} label={t('home.statSpan')} value={span} accent={accent} soft={soft} />}
      {!!weeks && <Stat Icon={CalendarClock} label={t('home.statWeeks')} value={weeks} accent={accent} soft={soft} />}
      <Stat Icon={Clock} label={t('home.statHours')} value={totalHours(r, lang)} accent={accent} soft={soft} />
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { gap: spacing.md },
  stat: { alignItems: 'center', gap: spacing.md },
  statIcon: {
    width: 36,
    height: 36,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  statText: { flex: 1 },
});
