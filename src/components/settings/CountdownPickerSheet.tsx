import React from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
import Animated, { SlideInDown } from 'react-native-reanimated';
import { Circle, CircleCheck } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { remaining } from '../../core/countdown';
import { countdownTitle, relativePhrase } from '../../core/present';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { sortCountdowns, useCountdownStore } from '../../store/useCountdownStore';
import { accentOf } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/spacing';
import { typography } from '../../theme/typography';

interface Props {
  visible: boolean;
  onPick: (id: string) => void;
  onClose: () => void;
}

/** Bottom sheet listing every countdown as a radio, to choose the one on the widget (and the Home
 * hero). Applies on tap. */
export function CountdownPickerSheet({ visible, onPick, onClose }: Props) {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { lang, rowDirection, textAlign } = useDirection();
  const insets = useSafeAreaInsets();
  const now = useNow();
  const countdowns = useCountdownStore(s => s.countdowns);
  const pinnedId = useCountdownStore(s => s.pinnedId);

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose} statusBarTranslucent navigationBarTranslucent>
      <Pressable style={[styles.scrim, { backgroundColor: colors.scrim }]} onPress={onClose} accessibilityLabel={t('common.close')} />
      <Animated.View
        entering={SlideInDown.duration(260)}
        style={[styles.sheet, { backgroundColor: colors.surface, paddingBottom: insets.bottom + spacing.lg }]}
        accessibilityViewIsModal
      >
        <View style={[styles.handle, { backgroundColor: colors.border }]} />
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
                onPress={() => onPick(c.id)}
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
      </Animated.View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  scrim: { ...StyleSheet.absoluteFill },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    maxHeight: '80%',
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    paddingTop: spacing.sm,
  },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, marginBottom: spacing.md },
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
