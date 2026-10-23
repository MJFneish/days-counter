import React, { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { useTranslation } from 'react-i18next';
import { Check, Info, Trash2, X } from 'lucide-react-native';

import { COUNTDOWN_ICON_COMPONENTS } from '../CountdownIcon';
import { DateWheels, TimeWheels, YEAR_MAX, YEAR_MIN } from '../picker/DateTimeWheels';
import { showToast } from '../Toast';
import { BottomSheet } from '../ui/BottomSheet';
import { GradientFill } from '../ui/GradientFill';
import { PressableScale } from '../ui/PressableScale';
import { SectionLabel } from '../ui/SectionLabel';
import { fromDate, remaining, toInstant } from '../../core/countdown';
import { countdownSubtitle, countdownTitle, relativePhrase } from '../../core/present';
import {
  COUNTDOWN_COLORS,
  COUNTDOWN_ICONS,
  type Countdown,
  type CountdownColor,
  type CountdownIcon,
  type LocalDateTime,
} from '../../core/types';
import { useNow } from '../../hooks/useClock';
import { useDirection } from '../../hooks/useDirection';
import { useCountdownStore } from '../../store/useCountdownStore';
import { useSettingsStore } from '../../store/useSettingsStore';
import { useSheetStore } from '../../store/useSheetStore';
import { COUNTDOWN_PALETTE } from '../../theme/countdownColors';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing, TOUCH_TARGET } from '../../theme/spacing';
import { typography } from '../../theme/typography';

interface Draft {
  title: string;
  target: LocalDateTime;
  color: CountdownColor;
  icon: CountdownIcon;
  pin: boolean;
}

const TITLE_MAX = 40;

/** A new countdown starts one month from today at midnight, in the next color not yet used. */
function newDraft(existing: Countdown[], hasPinned: boolean): Draft {
  const d = new Date();
  d.setMonth(d.getMonth() + 1);
  const target = { ...fromDate(d), hour: 0, minute: 0 };
  target.year = Math.min(Math.max(target.year, YEAR_MIN), YEAR_MAX);
  const used = new Set(existing.map(c => c.color));
  const color = COUNTDOWN_COLORS.find(c => !used.has(c)) ?? COUNTDOWN_COLORS[existing.length % COUNTDOWN_COLORS.length];
  return { title: '', target, color, icon: 'calendar', pin: !hasPinned };
}

/**
 * Bottom sheet to add or edit a countdown: a live preview in the chosen gradient, name, date and
 * time wheels, color, icon, "show on the widget", delete. Nothing is saved until "Save". Opened
 * through `useSheetStore().openEditor(id | null)`; rendered once by `SheetHost`.
 */
export function CountdownEditor() {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const { lang, rowDirection, textAlign, writingDirection } = useDirection();
  const now = useNow();
  const editor = useSheetStore(s => s.editor);
  const onClose = useSheetStore(s => s.closeEditor);
  const visible = editor.open;
  const use24h = useSettingsStore(s => s.use24h);
  const countdowns = useCountdownStore(s => s.countdowns);
  const pinnedId = useCountdownStore(s => s.pinnedId);
  const add = useCountdownStore(s => s.add);
  const update = useCountdownStore(s => s.update);
  const remove = useCountdownStore(s => s.remove);
  const pin = useCountdownStore(s => s.pin);
  const countdown = (editor.open && editor.countdownId && countdowns.find(c => c.id === editor.countdownId)) || null;

  const [draft, setDraft] = useState<Draft>(() => newDraft(countdowns, !!pinnedId));
  const isPinned = !!countdown && countdown.id === pinnedId;

  useEffect(() => {
    if (!visible) return;
    setDraft(
      countdown
        ? { title: countdown.title, target: countdown.target, color: countdown.color, icon: countdown.icon, pin: countdown.id === pinnedId }
        : newDraft(countdowns, !!pinnedId),
    );
    // Re-seed only when the sheet opens, not on every store change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, countdown?.id]);

  const set = (patch: Partial<Draft>) => setDraft(d => ({ ...d, ...patch }));

  const preview: Countdown = { id: 'preview', createdAt: now, ...draft };
  const r = remaining(draft.target, now);
  const palette = COUNTDOWN_PALETTE[draft.color];
  const PreviewIcon = COUNTDOWN_ICON_COMPONENTS[draft.icon];
  const isPast = toInstant(draft.target) <= now;
  const previewSubtitle = countdownSubtitle(preview, lang, use24h, t);

  const save = () => {
    const input = { title: draft.title, target: draft.target, color: draft.color, icon: draft.icon };
    if (countdown) {
      update(countdown.id, input);
      if (draft.pin && !isPinned) pin(countdown.id);
      showToast(t('toast.saved'));
    } else {
      add(input, draft.pin);
      showToast(t('toast.added'));
    }
    onClose();
  };

  const confirmDelete = () => {
    if (!countdown) return;
    Alert.alert(t('editor.deleteTitle'), t('editor.deleteBody'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('common.delete'),
        style: 'destructive',
        onPress: () => {
          remove(countdown.id);
          showToast(t('toast.deleted'));
          onClose();
        },
      },
    ]);
  };

  return (
    <BottomSheet visible={visible} onClose={onClose}>
      <View style={[styles.titleRow, { flexDirection: rowDirection }]}>
        <Text style={[typography.title, styles.flex, { color: colors.text, textAlign }]} accessibilityRole="header">
          {countdown ? t('editor.editTitle') : t('editor.newTitle')}
        </Text>
        <Pressable
          onPress={onClose}
          style={({ pressed }) => [styles.close, { backgroundColor: colors.surfaceAlt }, pressed && styles.pressed]}
          accessibilityRole="button"
          accessibilityLabel={t('common.close')}
        >
          <X size={20} color={colors.text} />
        </Pressable>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        {/* Live preview */}
        <View style={[styles.preview, { backgroundColor: palette.to, flexDirection: rowDirection }]}>
          <GradientFill id={`editor-${draft.color}`} from={palette.from} to={palette.to} circles={[[0.96, 0.1, 0.3, 0.08]]} />
          <View style={[styles.previewIcon, { backgroundColor: colors.onHeroPanel }]}>
            <PreviewIcon size={22} color={colors.onHero} />
          </View>
          <View style={styles.flex}>
            <Text numberOfLines={1} style={[typography.bodyStrong, { color: colors.onHero, textAlign }]}>
              {countdownTitle(preview, lang)}
            </Text>
            {!!previewSubtitle && (
              <Text numberOfLines={1} style={[typography.caption, { color: colors.onHeroMuted, textAlign }]}>
                {previewSubtitle}
              </Text>
            )}
          </View>
          <Text style={[typography.bodyStrong, { color: colors.onHero }]}>{relativePhrase(r, lang, t)}</Text>
        </View>

        {/* Name */}
        <SectionLabel first>{t('editor.name')}</SectionLabel>
        <TextInput
          value={draft.title}
          onChangeText={title => set({ title })}
          placeholder={t('editor.namePlaceholder')}
          placeholderTextColor={colors.textMuted}
          maxLength={TITLE_MAX}
          returnKeyType="done"
          style={[
            typography.body,
            styles.input,
            { color: colors.text, backgroundColor: colors.surfaceAlt, borderColor: colors.border, textAlign, writingDirection },
          ]}
          accessibilityLabel={t('editor.name')}
          accessibilityHint={t('editor.nameHint')}
        />
        <Text style={[typography.caption, styles.hint, { color: colors.textMuted, textAlign }]}>{t('editor.nameHint')}</Text>

        {/* Date + time */}
        <SectionLabel>{t('editor.date')}</SectionLabel>
        <DateWheels value={draft.target} onChange={target => set({ target })} fadeColor={colors.surface} />
        <SectionLabel>{t('editor.time')}</SectionLabel>
        <TimeWheels value={draft.target} onChange={target => set({ target })} fadeColor={colors.surface} use24h={use24h} />
        {isPast && (
          <View style={[styles.note, { backgroundColor: colors.primarySoft, flexDirection: rowDirection }]}>
            <Info size={18} color={colors.primary} />
            <Text style={[typography.caption, styles.flex, { color: colors.text, textAlign }]}>{t('editor.pastNote')}</Text>
          </View>
        )}

        {/* Color */}
        <SectionLabel>{t('editor.color')}</SectionLabel>
        <View style={[styles.swatches, { flexDirection: rowDirection }]} accessibilityRole="radiogroup">
          {COUNTDOWN_COLORS.map(c => {
            const active = c === draft.color;
            return (
              <Pressable
                key={c}
                onPress={() => set({ color: c })}
                style={styles.swatchHit}
                accessibilityRole="radio"
                accessibilityState={{ checked: active }}
                accessibilityLabel={t(`editor.colors.${c}`)}
              >
                <View style={[styles.swatchRing, { borderColor: active ? COUNTDOWN_PALETTE[c].to : 'transparent' }]}>
                  <View style={[styles.swatch, { backgroundColor: COUNTDOWN_PALETTE[c].to }]}>
                    {active && <Check size={18} color="#FFFFFF" strokeWidth={3} />}
                  </View>
                </View>
              </Pressable>
            );
          })}
        </View>

        {/* Icon */}
        <SectionLabel>{t('editor.icon')}</SectionLabel>
        <View style={[styles.icons, { flexDirection: rowDirection }]} accessibilityRole="radiogroup">
          {COUNTDOWN_ICONS.map(icon => {
            const Icon = COUNTDOWN_ICON_COMPONENTS[icon];
            const active = icon === draft.icon;
            return (
              <Pressable
                key={icon}
                onPress={() => set({ icon })}
                style={[
                  styles.iconCell,
                  {
                    backgroundColor: active ? COUNTDOWN_PALETTE[draft.color].to : colors.surfaceAlt,
                  },
                ]}
                accessibilityRole="radio"
                accessibilityState={{ checked: active }}
                accessibilityLabel={t(`editor.icons.${icon}`)}
              >
                <Icon size={22} color={active ? '#FFFFFF' : colors.textMuted} />
              </Pressable>
            );
          })}
        </View>

        {/* Pin */}
        {!isPinned && (
          <View style={[styles.pinRow, { backgroundColor: colors.surfaceAlt, flexDirection: rowDirection }]}>
            <View style={styles.flex}>
              <Text style={[typography.bodyStrong, { color: colors.text, textAlign }]}>{t('editor.pin')}</Text>
              <Text style={[typography.caption, { color: colors.textMuted, textAlign }]}>{t('editor.pinHint')}</Text>
            </View>
            <Switch
              value={draft.pin}
              onValueChange={value => set({ pin: value })}
              trackColor={{ false: colors.switchTrackOff, true: colors.primary }}
              thumbColor={draft.pin ? colors.onPrimary : colors.switchThumbOff}
              accessibilityLabel={t('editor.pin')}
            />
          </View>
        )}

        {countdown && (
          <Pressable
            onPress={confirmDelete}
            style={({ pressed }) => [styles.deleteRow, { borderColor: colors.danger, flexDirection: rowDirection }, pressed && styles.pressed]}
            accessibilityRole="button"
          >
            <Trash2 size={18} color={colors.danger} />
            <Text style={[typography.bodyStrong, { color: colors.danger }]}>{t('editor.delete')}</Text>
          </Pressable>
        )}
      </ScrollView>

      <View style={[styles.actions, { flexDirection: rowDirection, borderTopColor: colors.border }]}>
        <PressableScale
          containerStyle={styles.flex}
          style={[styles.action, { backgroundColor: colors.surfaceAlt }]}
          onPress={onClose}
          accessibilityRole="button"
        >
          <Text style={[typography.bodyStrong, { color: colors.text }]}>{t('common.cancel')}</Text>
        </PressableScale>
        <PressableScale
          containerStyle={styles.flex}
          style={[styles.action, { backgroundColor: colors.primary }]}
          onPress={save}
          accessibilityRole="button"
        >
          <Text style={[typography.bodyStrong, { color: colors.onPrimary }]}>{t('common.save')}</Text>
        </PressableScale>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  titleRow: { alignItems: 'center', gap: spacing.md, paddingHorizontal: spacing.lg, marginBottom: spacing.sm },
  close: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  scroll: { flexShrink: 1 },
  scrollContent: { paddingHorizontal: spacing.lg, paddingBottom: spacing.lg },
  preview: {
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    borderRadius: radius.lg,
    overflow: 'hidden',
  },
  previewIcon: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  input: {
    minHeight: TOUCH_TARGET,
    borderRadius: radius.md,
    borderWidth: 1,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  hint: { marginTop: spacing.xs },
  note: { alignItems: 'flex-start', gap: spacing.sm, padding: spacing.md, borderRadius: radius.md, marginTop: spacing.md },
  swatches: { flexWrap: 'wrap', gap: spacing.xs },
  swatchHit: { width: TOUCH_TARGET, height: TOUCH_TARGET, alignItems: 'center', justifyContent: 'center' },
  swatchRing: { width: 46, height: 46, borderRadius: 23, borderWidth: 2.5, alignItems: 'center', justifyContent: 'center' },
  swatch: { width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
  icons: { flexWrap: 'wrap', gap: spacing.sm },
  iconCell: { width: TOUCH_TARGET, height: TOUCH_TARGET, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  pinRow: { alignItems: 'center', gap: spacing.md, padding: spacing.md, borderRadius: radius.md, marginTop: spacing.xl },
  deleteRow: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    minHeight: TOUCH_TARGET,
    borderRadius: radius.md,
    borderWidth: 1.5,
    marginTop: spacing.xl,
  },
  actions: { gap: spacing.md, paddingHorizontal: spacing.lg, paddingTop: spacing.md, borderTopWidth: StyleSheet.hairlineWidth },
  action: { minHeight: TOUCH_TARGET, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.6 },
});
