import React, { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
import Animated, { FadeIn, LinearTransition } from 'react-native-reanimated';
import { Plus } from 'lucide-react-native';

import { AppLogo } from '../components/brand/AppLogo';
import { CountdownEditor } from '../components/editor/CountdownEditor';
import { CountdownRow } from '../components/home/CountdownRow';
import { HeroCountdown } from '../components/home/HeroCountdown';
import { StatsCard } from '../components/home/StatsCard';
import { showToast } from '../components/Toast';
import { PressableScale } from '../components/ui/PressableScale';
import { SectionLabel } from '../components/ui/SectionLabel';
import { fromDate, remaining } from '../core/countdown';
import { formatLongDate } from '../core/format';
import type { Countdown } from '../core/types';
import { useNow } from '../hooks/useClock';
import { useDirection } from '../hooks/useDirection';
import { selectPinned, sortCountdowns, useCountdownStore } from '../store/useCountdownStore';
import { useSettingsStore } from '../store/useSettingsStore';
import { useTheme } from '../theme/useTheme';
import { radius, spacing, TOUCH_TARGET } from '../theme/spacing';
import { typography } from '../theme/typography';

type EditorState = { open: false } | { open: true; countdown: Countdown | null };

function EmptyState({ onAdd }: { onAdd: () => void }) {
  const { t } = useTranslation();
  const { colors } = useTheme();
  return (
    <Animated.View entering={FadeIn.duration(300)} style={styles.empty}>
      <View style={[styles.emptyArt, { backgroundColor: colors.surfaceAlt }]}>
        <AppLogo size={96} tile />
      </View>
      <Text style={[typography.title, { color: colors.text, textAlign: 'center' }]}>{t('home.emptyTitle')}</Text>
      <Text style={[typography.body, { color: colors.textMuted, textAlign: 'center' }]}>{t('home.emptyBody')}</Text>
      <PressableScale onPress={onAdd} style={[styles.emptyButton, { backgroundColor: colors.primary }]} accessibilityRole="button">
        <Plus size={20} color={colors.onPrimary} />
        <Text style={[typography.bodyStrong, { color: colors.onPrimary }]}>{t('home.add')}</Text>
      </PressableScale>
    </Animated.View>
  );
}

/**
 * Home: today's date, the pinned countdown as the hero (the one on the widget) with its numbers,
 * then every other countdown — upcoming soonest first, passed ones after. Tapping any of them
 * opens the editor sheet; the pin button moves one to the hero + widget.
 */
export default function HomeScreen() {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const { lang, rowDirection, textAlign } = useDirection();
  const now = useNow();
  const countUp = useSettingsStore(s => s.countUp);
  const countdowns = useCountdownStore(s => s.countdowns);
  const pinned = useCountdownStore(selectPinned);
  const pin = useCountdownStore(s => s.pin);
  const [editor, setEditor] = useState<EditorState>({ open: false });

  // Re-sort only when a countdown passes its date, not every second.
  const passedCount = countdowns.filter(c => remaining(c.target, now).status !== 'upcoming').length;
  const others = useMemo(
    () => sortCountdowns(countdowns.filter(c => c.id !== pinned?.id), Date.now()),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [countdowns, pinned?.id, passedCount],
  );

  const openNew = useCallback(() => setEditor({ open: true, countdown: null }), []);
  const openEdit = useCallback((countdown: Countdown) => setEditor({ open: true, countdown }), []);
  const close = useCallback(() => setEditor({ open: false }), []);
  const pinCountdown = useCallback(
    (countdown: Countdown) => {
      pin(countdown.id);
      showToast(t('toast.pinned'));
    },
    [pin, t],
  );

  const pinnedStatus = pinned ? remaining(pinned.target, now).status : null;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={[styles.header, { flexDirection: rowDirection }]}>
          <View style={styles.flex}>
            <Text style={[typography.displayMedium, { color: colors.text, textAlign }]} accessibilityRole="header">
              {t('home.title')}
            </Text>
            <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>{formatLongDate(fromDate(new Date(now)), lang)}</Text>
          </View>
          <PressableScale
            onPress={openNew}
            style={[styles.addButton, { backgroundColor: colors.primary }]}
            accessibilityRole="button"
            accessibilityLabel={t('home.add')}
          >
            <Plus size={24} color={colors.onPrimary} />
          </PressableScale>
        </View>

        {pinned ? (
          <>
            <HeroCountdown countdown={pinned} onPress={() => openEdit(pinned)} />
            {(pinnedStatus === 'upcoming' || countUp) && (
              <View style={styles.stats}>
                <StatsCard countdown={pinned} />
              </View>
            )}
          </>
        ) : (
          <EmptyState onAdd={openNew} />
        )}

        {others.length > 0 && (
          <>
            <SectionLabel>{t('home.others')}</SectionLabel>
            <View style={styles.list}>
              {others.map(c => (
                <Animated.View key={c.id} layout={LinearTransition.duration(220)}>
                  <CountdownRow countdown={c} onPress={openEdit} onPin={pinCountdown} />
                </Animated.View>
              ))}
            </View>
          </>
        )}
      </ScrollView>

      <CountdownEditor visible={editor.open} countdown={editor.open ? editor.countdown : null} onClose={close} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  flex: { flex: 1 },
  content: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.xxxl,
  },
  header: { alignItems: 'center', gap: spacing.md, marginBottom: spacing.lg },
  addButton: {
    width: TOUCH_TARGET,
    height: TOUCH_TARGET,
    borderRadius: TOUCH_TARGET / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stats: { marginTop: spacing.md },
  list: { gap: spacing.sm },
  empty: { alignItems: 'center', gap: spacing.md, paddingVertical: spacing.xxxl, paddingHorizontal: spacing.lg },
  emptyArt: { padding: spacing.lg, borderRadius: radius.xl, marginBottom: spacing.sm },
  emptyButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    minHeight: TOUCH_TARGET,
    paddingHorizontal: spacing.xl,
    borderRadius: radius.pill,
    marginTop: spacing.sm,
  },
});
