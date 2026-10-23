import React, { useEffect, useState } from 'react';
import { AppState, Pressable, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Info, LayoutGrid, Lock } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { showToast } from '../Toast';
import { Card } from '../ui/Card';
import { PressableScale } from '../ui/PressableScale';
import { SectionLabel } from '../ui/SectionLabel';
import { remaining } from '../../core/countdown';
import { countdownTitle, relativePhrase } from '../../core/present';
import NativeDaysWidget from '../../native/NativeDaysWidget';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { selectPinned, useCountdownStore } from '../../store/useCountdownStore';
import { useSheetStore } from '../../store/useSheetStore';
import { accentOf } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing, TOUCH_TARGET } from '../../theme/spacing';
import { typography } from '../../theme/typography';
import { widgetAvailable } from '../../widget/useWidgetSync';

/** Settings → Home-screen widget: which countdown it shows (picker sheet via SheetHost), a button
 * that asks the launcher to place the widget, and the lock-screen note. Android only. */
export function WidgetSection() {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { lang, rowDirection, textAlign } = useDirection();
  const now = useNow();
  const pinned = useCountdownStore(selectPinned);
  const count = useCountdownStore(s => s.countdowns.length);
  const openPicker = useSheetStore(s => s.openPicker);
  const [canRequestPin, setCanRequestPin] = useState(false);

  useEffect(() => {
    if (!widgetAvailable) return;
    const check = () => NativeDaysWidget!.isRequestPinSupported().then(setCanRequestPin).catch(() => setCanRequestPin(false));
    check();
    const sub = AppState.addEventListener('change', s => s === 'active' && check());
    return () => sub.remove();
  }, []);

  if (!widgetAvailable) return null;

  const addWidget = async () => {
    const ok = await NativeDaysWidget!.requestPin().catch(() => false);
    if (!ok) showToast(t('settings.addWidgetManual'));
  };

  const Icon = pinned ? COUNTDOWN_ICON_COMPONENTS[pinned.icon] : LayoutGrid;
  const { accent, soft } = pinned ? accentOf(pinned.color, isDark) : { accent: colors.primary, soft: colors.primarySoft };

  return (
    <>
      <SectionLabel>{t('settings.widget')}</SectionLabel>
      <Card style={styles.card}>
        <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>{t('settings.widgetShows')}</Text>
        <View style={[styles.row, { flexDirection: rowDirection }]}>
          <View style={[styles.icon, { backgroundColor: soft }]}>
            <Icon size={20} color={accent} />
          </View>
          <View style={styles.text}>
            <Text numberOfLines={1} style={[typography.bodyStrong, { color: colors.text, textAlign }]}>
              {pinned ? countdownTitle(pinned, lang) : t('settings.widgetNone')}
            </Text>
            {pinned && (
              <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>
                {relativePhrase(remaining(pinned.target, now), lang, t)}
              </Text>
            )}
          </View>
          {count > 1 && (
            <Pressable
              onPress={openPicker}
              style={({ pressed }) => [styles.change, { backgroundColor: colors.surfaceAlt }, pressed && styles.pressed]}
              accessibilityRole="button"
              accessibilityLabel={`${t('common.change')} — ${t('settings.chooseCountdown')}`}
            >
              <Text style={[typography.bodyStrong, { color: colors.primary }]}>{t('common.change')}</Text>
            </Pressable>
          )}
        </View>

        {canRequestPin ? (
          <PressableScale
            onPress={addWidget}
            style={[styles.addButton, { backgroundColor: colors.primary, flexDirection: rowDirection }]}
            accessibilityRole="button"
          >
            <LayoutGrid size={20} color={colors.onPrimary} />
            <Text style={[typography.bodyStrong, styles.addText, { color: colors.onPrimary }]}>{t('settings.addWidget')}</Text>
          </PressableScale>
        ) : (
          <View style={[styles.note, { flexDirection: rowDirection }]}>
            <Info size={16} color={colors.textMuted} />
            <Text style={[typography.caption, styles.text, { color: colors.textMuted, textAlign }]}>{t('settings.addWidgetManual')}</Text>
          </View>
        )}
        <View style={[styles.note, { flexDirection: rowDirection }]}>
          <Info size={16} color={colors.textMuted} />
          <Text style={[typography.caption, styles.text, { color: colors.textMuted, textAlign }]}>{t('settings.addWidgetHint')}</Text>
        </View>
        <View style={[styles.note, { flexDirection: rowDirection }]}>
          <Lock size={16} color={colors.textMuted} />
          <Text style={[typography.caption, styles.text, { color: colors.textMuted, textAlign }]}>{t('settings.lockScreenNote')}</Text>
        </View>
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { gap: spacing.md },
  row: { alignItems: 'center', gap: spacing.md },
  icon: { width: 40, height: 40, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  text: { flex: 1, gap: 2 },
  change: {
    minHeight: 40,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addButton: {
    minHeight: TOUCH_TARGET,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  addText: { flexShrink: 1, textAlign: 'center' },
  note: { alignItems: 'flex-start', gap: spacing.sm },
  pressed: { opacity: 0.6 },
});
