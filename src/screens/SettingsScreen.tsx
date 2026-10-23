import React from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTranslation } from 'react-i18next';
import Animated, { FadeInDown, FadeOutUp } from 'react-native-reanimated';
import { Clock, Contrast, History, Moon, Sun, Timer, Trash2, type LucideIcon } from 'lucide-react-native';

import { version } from '../../package.json';
import { AppLogo } from '../components/brand/AppLogo';
import { WidgetSection } from '../components/settings/WidgetSection';
import { showToast } from '../components/Toast';
import { Card } from '../components/ui/Card';
import { SectionLabel } from '../components/ui/SectionLabel';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { localizeDigits } from '../core/format';
import { useDirection } from '../hooks/useDirection';
import type { Language } from '../i18n/language';
import { useCountdownStore } from '../store/useCountdownStore';
import { useSettingsStore, type ThemeMode } from '../store/useSettingsStore';
import { useTheme } from '../theme/useTheme';
import { radius, spacing } from '../theme/spacing';
import { typography } from '../theme/typography';

function IconBadge({ Icon }: { Icon: LucideIcon }) {
  const { colors } = useTheme();
  return (
    <View style={[styles.iconBadge, { backgroundColor: colors.primarySoft }]}>
      <Icon size={20} color={colors.primary} />
    </View>
  );
}

/** A labelled switch row (icon, title, hint) — the shape every Settings toggle shares. */
function SwitchRow({ Icon, title, hint, value, onChange }: {
  Icon: LucideIcon;
  title: string;
  hint: string;
  value: boolean;
  onChange: (value: boolean) => void;
}) {
  const { colors } = useTheme();
  const { rowDirection, textAlign } = useDirection();
  return (
    <View style={[styles.row, { flexDirection: rowDirection }]}>
      <IconBadge Icon={Icon} />
      <View style={styles.rowText}>
        <Text style={[typography.bodyStrong, { color: colors.text, textAlign }]}>{title}</Text>
        <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>{hint}</Text>
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: colors.switchTrackOff, true: colors.primary }}
        thumbColor={value ? colors.onPrimary : colors.switchThumbOff}
        accessibilityLabel={title}
      />
    </View>
  );
}

export default function SettingsScreen() {
  const { t } = useTranslation();
  const { colors, isDark } = useTheme();
  const { rowDirection, textAlign, lang } = useDirection();

  const language = useSettingsStore(s => s.language);
  const setLanguage = useSettingsStore(s => s.setLanguage);
  const themeMode = useSettingsStore(s => s.themeMode);
  const setThemeMode = useSettingsStore(s => s.setThemeMode);
  const showSeconds = useSettingsStore(s => s.showSeconds);
  const setShowSeconds = useSettingsStore(s => s.setShowSeconds);
  const countUp = useSettingsStore(s => s.countUp);
  const setCountUp = useSettingsStore(s => s.setCountUp);
  const use24h = useSettingsStore(s => s.use24h);
  const setUse24h = useSettingsStore(s => s.setUse24h);
  const hasCountdowns = useCountdownStore(s => s.countdowns.length > 0);
  const removeAll = useCountdownStore(s => s.removeAll);

  const confirmReset = () =>
    Alert.alert(t('settings.resetTitle'), t('settings.resetBody'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('common.delete'),
        style: 'destructive',
        onPress: () => {
          removeAll();
          showToast(t('toast.reset'));
        },
      },
    ]);

  const followSystem = themeMode === 'system';
  const themeOptions: Array<{ mode: Exclude<ThemeMode, 'system'>; labelKey: string; Icon: LucideIcon }> = [
    { mode: 'light', labelKey: 'settings.light', Icon: Sun },
    { mode: 'dark', labelKey: 'settings.dark', Icon: Moon },
  ];

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <Text style={[typography.displayMedium, { color: colors.text, textAlign }]} accessibilityRole="header">
          {t('settings.title')}
        </Text>

        {/* Language */}
        <SectionLabel first>{t('settings.language')}</SectionLabel>
        <SegmentedControl<Language>
          value={language}
          onChange={setLanguage}
          accessibilityLabel={t('settings.language')}
          options={[
            { value: 'en', label: 'English' },
            { value: 'ar', label: 'العربية' },
          ]}
        />

        {/* Appearance — same interaction as the sibling apps: follow-system switch, tiles otherwise */}
        <SectionLabel>{t('settings.appearance')}</SectionLabel>
        <Card>
          <SwitchRow
            Icon={Contrast}
            title={t('settings.followSystem')}
            hint={t('settings.followSystemHint')}
            value={followSystem}
            onChange={value => setThemeMode(value ? 'system' : isDark ? 'dark' : 'light')}
          />
          {!followSystem && (
            <Animated.View
              entering={FadeInDown.duration(200)}
              exiting={FadeOutUp.duration(150)}
              style={[styles.themeTiles, { flexDirection: rowDirection }]}
            >
              {themeOptions.map(({ mode, labelKey, Icon }) => {
                const active = themeMode === mode;
                return (
                  <Pressable
                    key={mode}
                    onPress={() => setThemeMode(mode)}
                    style={[
                      styles.themeTile,
                      {
                        borderColor: active ? colors.primary : colors.border,
                        backgroundColor: active ? colors.primarySoft : 'transparent',
                      },
                    ]}
                    accessibilityRole="radio"
                    accessibilityState={{ checked: active }}
                  >
                    <Icon size={22} color={active ? colors.primary : colors.textMuted} />
                    <Text style={[typography.bodyStrong, { color: active ? colors.primary : colors.text }]}>{t(labelKey)}</Text>
                  </Pressable>
                );
              })}
            </Animated.View>
          )}
        </Card>

        {/* Countdown */}
        <SectionLabel>{t('settings.countdown')}</SectionLabel>
        <Card style={styles.rows}>
          <SwitchRow Icon={Timer} title={t('settings.showSeconds')} hint={t('settings.showSecondsHint')} value={showSeconds} onChange={setShowSeconds} />
          <View style={[styles.divider, { backgroundColor: colors.border }]} />
          <SwitchRow Icon={History} title={t('settings.countUp')} hint={t('settings.countUpHint')} value={countUp} onChange={setCountUp} />
          <View style={[styles.divider, { backgroundColor: colors.border }]} />
          <SwitchRow Icon={Clock} title={t('settings.clock24')} hint={t('settings.clock24Hint')} value={use24h} onChange={setUse24h} />
        </Card>

        {/* Widget (Android only — renders nothing elsewhere) */}
        <WidgetSection />

        {/* Data */}
        {hasCountdowns && (
          <>
            <SectionLabel>{t('settings.data')}</SectionLabel>
            <Pressable
              onPress={confirmReset}
              style={({ pressed }) => [styles.danger, { borderColor: colors.danger, flexDirection: rowDirection }, pressed && styles.pressed]}
              accessibilityRole="button"
            >
              <Trash2 size={18} color={colors.danger} />
              <Text style={[typography.bodyStrong, { color: colors.danger }]}>{t('settings.reset')}</Text>
            </Pressable>
          </>
        )}

        {/* About */}
        <SectionLabel>{t('settings.about')}</SectionLabel>
        <Card>
          <View style={[styles.row, { flexDirection: rowDirection, marginBottom: spacing.md }]}>
            <AppLogo size={52} tile />
            <View style={styles.rowText}>
              <Text style={[typography.subtitle, { color: colors.text, textAlign }]}>{t('app.name')}</Text>
              <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>
                {t('settings.version', { version: localizeDigits(version, lang) })}
              </Text>
            </View>
          </View>
          <Text style={[typography.body, { color: colors.text, textAlign }]}>{t('settings.aboutBody')}</Text>
          <Text style={[typography.caption, { color: colors.textMuted, textAlign, marginTop: spacing.md }]}>
            {t('settings.madeBy')}
          </Text>
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.xxxl,
  },
  row: { alignItems: 'center', gap: spacing.md },
  rows: { gap: spacing.md },
  divider: { height: StyleSheet.hairlineWidth },
  danger: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    minHeight: 48,
    borderRadius: radius.md,
    borderWidth: 1.5,
  },
  pressed: { opacity: 0.6 },
  rowText: { flex: 1, gap: 2 },
  iconBadge: {
    width: 40,
    height: 40,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  themeTiles: { marginTop: spacing.md, gap: spacing.sm },
  themeTile: {
    flex: 1,
    minHeight: 72,
    borderWidth: 1.5,
    borderRadius: radius.md,
    paddingVertical: spacing.md,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
  },
});
