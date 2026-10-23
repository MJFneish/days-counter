import React, { memo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Pin } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { PressableScale } from '../ui/PressableScale';
import { progress, remaining } from '../../core/countdown';
import { countdownSubtitle, countdownTitle, headline, relativePhrase } from '../../core/present';
import type { Countdown } from '../../core/types';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { formatNumber, unitWord } from '../../i18n/plural';
import { useSettingsStore } from '../../store/useSettingsStore';
import { accentOf } from '../../theme/countdownColors';
import { elevation } from '../../theme/elevation';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing, TOUCH_TARGET } from '../../theme/spacing';
import { typography } from '../../theme/typography';
import { ProgressBar } from './HeroCountdown';

interface Props {
  countdown: Countdown;
  onPress: (countdown: Countdown) => void;
  onPin: (countdown: Countdown) => void;
}

/** A countdown in the Home list: icon, name, date, a thin progress line, the headline number
 * ("316 days" — hours/minutes on the last day) and a pin button that moves it to the widget. */
export const CountdownRow = memo(function CountdownRow({ countdown, onPress, onPin }: Props) {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { lang, rowDirection, textAlign, isRTL } = useDirection();
  const now = useNow();
  const use24h = useSettingsStore(s => s.use24h);
  const countUp = useSettingsStore(s => s.countUp);

  const r = remaining(countdown.target, now);
  const { accent, soft } = accentOf(countdown.color, isDark);
  const Icon = COUNTDOWN_ICON_COMPONENTS[countdown.icon];
  const title = countdownTitle(countdown, lang);
  const subtitle = countdownSubtitle(countdown, lang, use24h, t, true);
  const { value, unit } = headline(r);
  const passed = r.status !== 'upcoming';

  return (
    <PressableScale
      onPress={() => onPress(countdown)}
      style={[
        styles.row,
        { backgroundColor: colors.card, borderColor: colors.border, borderWidth: isDark ? 1 : 0, flexDirection: rowDirection },
        elevation(colors.cardShadow),
      ]}
      accessibilityRole="button"
      accessibilityLabel={`${title}. ${relativePhrase(r, lang, t)}`}
      accessibilityHint={t('home.edit', { name: title })}
    >
      <View style={[styles.iconBadge, { backgroundColor: soft }]}>
        <Icon size={22} color={accent} />
      </View>

      <View style={styles.middle}>
        <Text numberOfLines={1} style={[typography.bodyStrong, { color: colors.text, textAlign }]}>
          {title}
        </Text>
        {!!subtitle && (
          <Text numberOfLines={1} style={[typography.caption, { color: colors.textMuted, textAlign }]}>
            {subtitle}
          </Text>
        )}
        {!passed && (
          <View style={styles.progress}>
            <ProgressBar value={progress(countdown.target, countdown.createdAt, now)} color={accent} track={soft} rtl={isRTL} height={4} />
          </View>
        )}
      </View>

      <View style={styles.count}>
        {r.status === 'arrived' ? (
          <Text style={[typography.bodyStrong, { color: accent }]}>{t('countdown.today')}</Text>
        ) : passed && !countUp ? (
          <Text style={[typography.caption, { color: colors.textMuted }]}>{t('countdown.finished')}</Text>
        ) : (
          <>
            <Text style={[typography.digits, styles.number, { color: passed ? colors.textMuted : accent }]}>
              {formatNumber(value, lang)}
            </Text>
            <Text numberOfLines={1} style={[typography.caption, { color: colors.textMuted }]}>
              {unitWord(unit, value, lang)}
            </Text>
          </>
        )}
      </View>

      <Pressable
        onPress={() => onPin(countdown)}
        style={({ pressed }) => [styles.pin, pressed && styles.pressed]}
        accessibilityRole="button"
        accessibilityLabel={t('home.pin', { name: title })}
        hitSlop={4}
      >
        <Pin size={18} color={colors.textMuted} />
      </Pressable>
    </PressableScale>
  );
});

const styles = StyleSheet.create({
  row: {
    alignItems: 'center',
    gap: spacing.md,
    borderRadius: radius.lg,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.md,
  },
  iconBadge: {
    width: 44,
    height: 44,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  middle: { flex: 1, gap: 2 },
  progress: { marginTop: spacing.sm },
  count: { alignItems: 'center', minWidth: 56 },
  number: { fontSize: 24 },
  pin: {
    width: TOUCH_TARGET - 8,
    height: TOUCH_TARGET,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: { opacity: 0.5 },
});
