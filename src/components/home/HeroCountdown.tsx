import React, { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import Animated, { FadeInDown, useAnimatedStyle, useReducedMotion, useSharedValue, withTiming } from 'react-native-reanimated';
import { PartyPopper, Pin } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { GradientFill } from '../ui/GradientFill';
import { PressableScale } from '../ui/PressableScale';
import { progress, remaining } from '../../core/countdown';
import { formatPercent, localizeDigits, pad2 } from '../../core/format';
import { countdownSubtitle, countdownTitle, relativePhrase } from '../../core/present';
import type { Countdown } from '../../core/types';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { countPhrase, formatNumber, unitWord, type Unit } from '../../i18n/plural';
import type { Language } from '../../i18n/language';
import { useSettingsStore } from '../../store/useSettingsStore';
import { COUNTDOWN_PALETTE } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

const CIRCLES = [
  [0.95, 0.05, 0.32, 0.08],
  [0.02, 1.02, 0.26, 0.06],
  [0.72, 0.78, 0.08, 0.05],
] as const;

/** One H/M/S tile. The number slides in from above when it changes (an odometer rolling down);
 * reduced motion swaps it without animating. */
function TimeTile({ value, unit, lang, still }: { value: number; unit: Unit; lang: Language; still: boolean }) {
  const { colors } = useTheme();
  return (
    <View
      style={[styles.tile, { backgroundColor: colors.onHeroPanel }]}
      accessible
      accessibilityLabel={countPhrase(unit, value, lang)}
    >
      <View style={styles.tileDigits}>
        <Animated.Text
          key={value}
          entering={still ? undefined : FadeInDown.duration(220)}
          style={[typography.digits, { color: colors.onHero }]}
        >
          {localizeDigits(pad2(value), lang)}
        </Animated.Text>
      </View>
      <Text numberOfLines={1} style={[typography.caption, { color: colors.onHeroMuted }]}>
        {unitWord(unit, value, lang)}
      </Text>
    </View>
  );
}

/** Fills from the reading start: left in English, right in Arabic. */
export function ProgressBar({ value, color, track, rtl, height = 6 }: { value: number; color: string; track: string; rtl: boolean; height?: number }) {
  const width = useSharedValue(value);
  useEffect(() => {
    width.value = withTiming(value, { duration: 600 });
  }, [value, width]);
  const style = useAnimatedStyle(() => ({ width: `${Math.max(2, width.value * 100)}%` }));
  return (
    <View style={[styles.progressTrack, { backgroundColor: track, height, borderRadius: height / 2 }]}>
      <Animated.View
        style={[{ height, borderRadius: height / 2, backgroundColor: color, alignSelf: rtl ? 'flex-end' : 'flex-start' }, style]}
      />
    </View>
  );
}

interface Props {
  countdown: Countdown;
  onPress: () => void;
}

/** The pinned countdown, big: icon + name, the day count, live H/M/S tiles and the progress of
 * the wait — in the countdown's own gradient. The same countdown the widget shows. */
export function HeroCountdown({ countdown, onPress }: Props) {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const { lang, rowDirection, textAlign, isRTL } = useDirection();
  const still = useReducedMotion();
  const now = useNow();
  const showSeconds = useSettingsStore(s => s.showSeconds);
  const countUp = useSettingsStore(s => s.countUp);
  const use24h = useSettingsStore(s => s.use24h);

  const r = remaining(countdown.target, now);
  const palette = COUNTDOWN_PALETTE[countdown.color];
  const Icon = COUNTDOWN_ICON_COMPONENTS[countdown.icon];
  const title = countdownTitle(countdown, lang);
  const subtitle = countdownSubtitle(countdown, lang, use24h, t);
  const passed = r.status !== 'upcoming';
  const showCount = !passed || countUp;
  const pct = progress(countdown.target, countdown.createdAt, now);

  return (
    <PressableScale
      onPress={onPress}
      style={[styles.card, { backgroundColor: palette.to }]}
      accessibilityRole="button"
      accessibilityLabel={`${title}. ${relativePhrase(r, lang, t)}`}
      accessibilityHint={t('home.edit', { name: title })}
    >
      <GradientFill id={`hero-${countdown.color}`} from={palette.from} to={palette.to} circles={CIRCLES} />

      <View style={[styles.header, { flexDirection: rowDirection }]}>
        <View style={[styles.iconBubble, { backgroundColor: colors.onHeroPanel }]}>
          <Icon size={22} color={colors.onHero} />
        </View>
        <View style={styles.headerText}>
          <Text numberOfLines={2} style={[typography.subtitle, { color: colors.onHero, textAlign }]}>
            {title}
          </Text>
          {!!subtitle && (
            <Text numberOfLines={1} style={[typography.caption, { color: colors.onHeroMuted, textAlign }]}>
              {subtitle}
            </Text>
          )}
        </View>
        <View style={[styles.pinChip, { backgroundColor: colors.onHeroPanel, flexDirection: rowDirection }]}>
          <Pin size={13} color={colors.onHero} />
          <Text style={[typography.label, { color: colors.onHero, letterSpacing: 0 }]}>{t('home.pinned')}</Text>
        </View>
      </View>

      {r.status === 'arrived' && (
        <View style={[styles.arrived, { flexDirection: rowDirection }]}>
          <PartyPopper size={22} color={colors.onHero} />
          <Text style={[typography.title, { color: colors.onHero }]}>{t('countdown.arrived')}</Text>
        </View>
      )}

      {showCount ? (
        <>
          <View style={[styles.daysRow, { flexDirection: rowDirection }]}>
            <Animated.Text
              key={r.days}
              entering={still ? undefined : FadeInDown.duration(260)}
              style={[typography.hero, { color: colors.onHero }]}
              adjustsFontSizeToFit
              numberOfLines={1}
            >
              {formatNumber(r.days, lang)}
            </Animated.Text>
            <Text style={[typography.subtitle, styles.daysLabel, { color: colors.onHeroMuted, textAlign }]}>
              {`${unitWord('day', r.days, lang)} ${passed ? t('countdown.since') : t('countdown.toGo')}`}
            </Text>
          </View>

          <View style={[styles.tiles, { flexDirection: rowDirection }]}>
            <TimeTile value={r.hours} unit="hour" lang={lang} still={still} />
            <TimeTile value={r.minutes} unit="minute" lang={lang} still={still} />
            {showSeconds && <TimeTile value={r.seconds} unit="second" lang={lang} still={still} />}
          </View>
        </>
      ) : (
        <Text style={[typography.title, styles.doneText, { color: colors.onHero, textAlign }]}>
          {t('countdown.doneNoCountUp')}
        </Text>
      )}

      {!passed && (
        <View style={styles.progressBlock}>
          <ProgressBar value={pct} color={colors.onHero} track={colors.onHeroPanel} rtl={isRTL} />
          <Text style={[typography.caption, { color: colors.onHeroMuted, textAlign: isRTL ? 'right' : 'left' }]}>
            {t('countdown.progress', { percent: formatPercent(Math.floor(pct * 100), lang) })}
          </Text>
        </View>
      )}
      {r.status === 'arrived' && countUp && (
        <Text style={[typography.caption, styles.progressBlock, { color: colors.onHeroMuted, textAlign }]}>
          {t('countdown.arrivedBody')}
        </Text>
      )}
    </PressableScale>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radius.xl,
    padding: spacing.lg,
    overflow: 'hidden',
  },
  header: { alignItems: 'center', gap: spacing.md },
  iconBubble: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerText: { flex: 1, gap: 2 },
  pinChip: {
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
    borderRadius: radius.pill,
    alignSelf: 'flex-start',
  },
  arrived: { alignItems: 'center', gap: spacing.sm, marginTop: spacing.lg },
  daysRow: { alignItems: 'flex-end', gap: spacing.sm, marginTop: spacing.lg },
  daysLabel: { flexShrink: 1, marginBottom: 12 },
  tiles: { gap: spacing.sm, marginTop: spacing.md },
  tile: {
    flex: 1,
    borderRadius: radius.md,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  tileDigits: { height: 36, justifyContent: 'center', overflow: 'hidden' },
  doneText: { marginTop: spacing.lg },
  progressBlock: { marginTop: spacing.lg, gap: spacing.xs },
  progressTrack: { overflow: 'hidden' },
});
