import React, { useEffect } from 'react';
import { BackHandler, Pressable, StyleSheet, View, useWindowDimensions, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
import Animated, { FadeIn, FadeOut, SlideInDown, SlideOutDown } from 'react-native-reanimated';

import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/spacing';

interface Props {
  visible: boolean;
  onClose: () => void;
  /** Share of the window height the sheet may take. */
  maxHeightRatio?: number;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
}

/**
 * Bottom sheet drawn over the whole app (mount it from `SheetHost` in App.tsx, not inside a
 * screen), with a scrim, slide-in/out and the Android back button closing it.
 *
 * Deliberately **not** an RN `Modal`: on RN 0.87 with edge-to-edge, the Modal's content root is
 * laid out at the screen height minus the status and navigation bars (2216 of 2400 px on a Pixel
 * 8a) yet drawn from y = 0, so a sheet anchored to its bottom overflowed it — the lower half of
 * the Save/Cancel buttons was drawn but not touchable, and taps there closed the sheet (found on
 * the emulator). Here the sheet lives in the activity window, whose root really is full-screen.
 * The max height is numeric (from the window), not a percentage of an unknown parent.
 */
export function BottomSheet({ visible, onClose, maxHeightRatio = 0.94, style, children }: Props) {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();

  useEffect(() => {
    if (!visible) return;
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      onClose();
      return true;
    });
    return () => sub.remove();
  }, [visible, onClose]);

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {visible && (
        <Animated.View entering={FadeIn.duration(200)} exiting={FadeOut.duration(180)} style={[StyleSheet.absoluteFill, { backgroundColor: colors.scrim }]}>
          <Pressable style={StyleSheet.absoluteFill} onPress={onClose} accessibilityLabel={t('common.close')} accessibilityRole="button" />
        </Animated.View>
      )}
      {visible && (
        <Animated.View
          entering={SlideInDown.duration(260)}
          exiting={SlideOutDown.duration(200)}
          style={[
            styles.sheet,
            { backgroundColor: colors.surface, maxHeight: height * maxHeightRatio, paddingBottom: insets.bottom + spacing.md },
            style,
          ]}
          accessibilityViewIsModal
        >
          <View style={[styles.handle, { backgroundColor: colors.border }]} />
          {children}
        </Animated.View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    paddingTop: spacing.sm,
  },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, marginBottom: spacing.sm },
});
