import React, { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Circle, CircleCheck } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { showToast } from '../Toast';
import { BottomSheet } from '../ui/BottomSheet';
import { remaining } from '../../core/countdown';
import { countdownTitle, relativePhrase } from '../../core/present';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { sortCountdowns, useCountdownStore } from '../../store/useCountdownStore';
import { useSheetStore } from '../../store/useSheetStore';
import { accentOf } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

/** Bottom sheet listing every countdown as a radio, to choose the one on the widget (and the Home
 * hero). Applies on tap. Opened through `useSheetStore().openPicker()`; rendered by `SheetHost`. */
export function CountdownPickerSheet() {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { lang, rowDirection, textAlign } = useDirection();
  const now = useNow();
  const visible = useSheetStore(s => s.pickerOpen);
  const onClose = useSheetStore(s => s.closePicker);
  const countdowns = useCountdownStore(s => s.countdowns);
  const pinnedId = useCountdownStore(s => s.pinnedId);
  const pin = useCountdownStore(s => s.pin);

  const pick = useCallback(
    (id: string) => {
      pin(id);
      onClose();
      showToast(t('toast.pinned'));
    },
    [pin, onClose, t],
  );

  return (
    <BottomSheet visible={visible} onClose={onClose} maxHeightRatio={0.8}>
      <Text style={[typography.title, styles.title, { color: colors.text, textAlign }]} accessibilityRole="header">
        {t('settings.chooseCountdown')}
      </Text>
      <ScrollView style={styles.list} contentContainerStyle={styles.listContent} accessibilityRole="radiogroup">
        {sortCountdowns(countdowns, now).map(c => {
          const active = c.id === pinnedId;
          const Icon = COUNTDOWN_ICON_COMPONENTS[c.icon];
          const { accent, soft } = accentOf(c.color, isDark);
          const title = countdownTitle(c, lang);
          return (
            <Pressable
              key={c.id}
              onPress={() => pick(c.id)}
              style={({ pressed }) => [
                styles.option,
                {
                  flexDirection: rowDirection,
                  borderColor: active ? colors.primary : colors.border,
                  backgroundColor: active ? colors.primarySoft : 'transparent',
                },
                pressed && styles.pressed,
              ]}
              accessibilityRole="radio"
              accessibilityState={{ checked: active }}
              accessibilityLabel={title}
            >
              <View style={[styles.icon, { backgroundColor: soft }]}>
                <Icon size={20} color={accent} />
              </View>
              <View style={styles.text}>
                <Text numberOfLines={1} style={[typography.bodyStrong, { color: colors.text, textAlign }]}>
                  {title}
                </Text>
                <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>
                  {relativePhrase(remaining(c.target, now), lang, t)}
                </Text>
              </View>
              {active ? <CircleCheck size={22} color={colors.primary} /> : <Circle size={22} color={colors.textMuted} />}
            </Pressable>
          );
        })}
      </ScrollView>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  title: { paddingHorizontal: spacing.lg, marginBottom: spacing.md },
  list: { flexShrink: 1 },
  listContent: { paddingHorizontal: spacing.lg, gap: spacing.sm },
  option: {
    alignItems: 'center',
    gap: spacing.md,
    borderWidth: 1.5,
    borderRadius: radius.lg,
    padding: spacing.md,
    minHeight: 64,
  },
  icon: { width: 40, height: 40, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  text: { flex: 1, gap: 2 },
  pressed: { opacity: 0.7 },
});
