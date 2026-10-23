import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';

import { remaining } from '../core/countdown';
import { formatLongDate } from '../core/format';
import { useNow } from '../hooks/useClock';
import { useDirection } from '../hooks/useDirection';
import { countPhrase } from '../i18n/plural';
import { selectPinned, useCountdownStore } from '../store/useCountdownStore';
import { useTheme } from '../theme/useTheme';
import { spacing } from '../theme/spacing';
import { typography } from '../theme/typography';

/** Shell placeholder (v0.3.0) — the full Home lands in v0.4.0. */
export default function HomeScreen() {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const { lang, textAlign } = useDirection();
  const now = useNow();
  const pinned = useCountdownStore(selectPinned);
  const r = pinned ? remaining(pinned.target, now) : null;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]} edges={['top']}>
      <Text style={[typography.displayMedium, { color: colors.text, textAlign }]}>{t('home.title')}</Text>
      {pinned && r && (
        <View style={styles.block}>
          <Text style={[typography.subtitle, { color: colors.textMuted, textAlign }]}>{formatLongDate(pinned.target, lang)}</Text>
          <Text style={[typography.displayLarge, { color: colors.primary, textAlign }]}>{countPhrase('day', r.days, lang)}</Text>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: spacing.lg, paddingTop: spacing.md },
  block: { marginTop: spacing.xl, gap: spacing.sm },
});
