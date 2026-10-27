package com.devlance.dayscounter.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownColor
import com.devlance.dayscounter.core.CountdownIcon
import com.devlance.dayscounter.core.LocalDateTime
import com.devlance.dayscounter.core.countdownSubtitle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.core.daysInMonth
import com.devlance.dayscounter.core.dayPeriod
import com.devlance.dayscounter.core.hour12
import com.devlance.dayscounter.core.localizeDigits
import com.devlance.dayscounter.core.monthName
import com.devlance.dayscounter.core.pad2
import com.devlance.dayscounter.core.relativePhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.core.toInstant
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.data.CountdownDraft
import com.devlance.dayscounter.data.YEAR_MAX
import com.devlance.dayscounter.data.YEAR_MIN
import com.devlance.dayscounter.data.newCountdownId
import com.devlance.dayscounter.ui.BottomSheet
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.Sheets
import com.devlance.dayscounter.ui.components.ConfirmDialog
import com.devlance.dayscounter.ui.components.DangerButton
import com.devlance.dayscounter.ui.components.SectionLabel
import com.devlance.dayscounter.ui.components.Toasts
import com.devlance.dayscounter.ui.components.pressable
import com.devlance.dayscounter.ui.home.HeroBubble
import com.devlance.dayscounter.ui.home.gradientFill
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.icons.lucide
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.TOUCH_TARGET
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import com.devlance.dayscounter.ui.theme.palette

private const val TITLE_MAX = 40

/** The add/edit sheet, mounted once over the app (AppRoot) and opened through `Sheets.openEditor`. */
@Composable
fun EditorSheet() {
  val request = Sheets.editor
  // Keep the last request while the sheet slides out.
  var shown by remember { mutableStateOf(request) }
  if (request != null) shown = request
  BottomSheet(visible = request != null, onClose = Sheets::closeEditor) {
    // A fresh editor (and draft) for every opening.
    shown?.let { androidx.compose.runtime.key(it.token) { CountdownEditor(it.countdownId) } }
  }
}

/**
 * v1's CountdownEditor: a live preview in the chosen gradient, name, date and time wheels, color,
 * icon, "show on the widget", delete. Nothing is saved until Save.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CountdownEditor(countdownId: String?) {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val now = LocalNow.current
  val state by AppStore.countdowns.collectAsStateWithLifecycle()
  val settings by AppStore.settings.collectAsStateWithLifecycle()
  val countdown = countdownId?.let { id -> state.countdowns.firstOrNull { it.id == id } }
  val isPinned = countdown != null && countdown.id == state.pinnedId
  var draft by remember {
    mutableStateOf(
      countdown?.let { CountdownDraft(it.title, it.target, it.color, it.icon, it.id == state.pinnedId) }
        ?: CountdownDraft.new(state.countdowns, state.pinnedId != null, now),
    )
  }
  var confirmDelete by remember { mutableStateOf(false) }

  val preview = Countdown("preview", draft.title.trim(), draft.target, draft.color, draft.icon, now)
  val isPast = toInstant(draft.target) <= now

  fun save() {
    val input = draft.input
    if (countdown != null) {
      AppStore.updateCountdowns { st -> st.update(countdown.id, input).let { if (draft.pin && !isPinned) it.pin(countdown.id) else it } }
      Toasts.show(s["toast.saved"])
    } else {
      AppStore.updateCountdowns { it.add(input, draft.pin, newCountdownId(), System.currentTimeMillis()) }
      Toasts.show(s["toast.added"])
    }
    Sheets.closeEditor()
  }

  // Title row
  Row(Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
    Text(
      if (countdown != null) s["editor.editTitle"] else s["editor.newTitle"],
      style = Type.title,
      color = c.text,
      modifier = Modifier.weight(1f).semantics { heading() },
    )
    Box(
      Modifier.size(40.dp).clip(CircleShape).background(c.surfaceAlt).clickable(role = Role.Button, onClick = Sheets::closeEditor).semantics { contentDescription = s["common.close"] },
      contentAlignment = Alignment.Center,
    ) {
      LucideIcon(LucideIcons.X, c.text, 20.dp)
    }
  }

  Column(
    Modifier
      .weight(1f, fill = false)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = Spacing.lg)
      .padding(bottom = Spacing.lg),
  ) {
    // Live preview
    Row(
      Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).gradientFill(draft.color.palette, listOf(floatArrayOf(0.96f, 0.1f, 0.3f, 0.08f))).padding(Spacing.md),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
      HeroBubble(draft.icon.lucide, size = 40.dp)
      Column(Modifier.weight(1f)) {
        Text(countdownTitle(preview, theme.lang), style = Type.bodyStrong, color = c.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val subtitle = countdownSubtitle(preview, s, settings.use24h)
        if (subtitle.isNotEmpty()) Text(subtitle, style = Type.caption, color = c.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(relativePhrase(remaining(draft.target, now), s), style = Type.bodyStrong, color = c.onHero)
    }

    // Name
    SectionLabel(s["editor.name"], first = true)
    val fieldShape = RoundedCornerShape(Radius.md)
    BasicTextField(
      value = draft.title,
      onValueChange = { draft = draft.copy(title = it.take(TITLE_MAX)) },
      singleLine = true,
      textStyle = Type.body.copy(color = c.text),
      cursorBrush = SolidColor(c.primary),
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = TOUCH_TARGET)
        .clip(fieldShape)
        .background(c.surfaceAlt)
        .border(1.dp, c.border, fieldShape)
        .semantics { contentDescription = s["editor.name"] },
      decorationBox = { inner ->
        Box(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md), contentAlignment = Alignment.CenterStart) {
          if (draft.title.isEmpty()) Text(s["editor.namePlaceholder"], style = Type.body, color = c.textMuted)
          inner()
        }
      },
    )
    Text(s["editor.nameHint"], style = Type.caption, color = c.textMuted, modifier = Modifier.padding(top = Spacing.xs))

    // Date + time
    SectionLabel(s["editor.date"])
    DateWheels(draft.target) { draft = draft.copy(target = it) }
    SectionLabel(s["editor.time"])
    TimeWheels(draft.target, settings.use24h) { draft = draft.copy(target = it) }
    if (isPast) {
      Row(
        Modifier.fillMaxWidth().padding(top = Spacing.md).clip(RoundedCornerShape(Radius.md)).background(c.primarySoft).padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
      ) {
        LucideIcon(LucideIcons.Info, c.primary, 18.dp)
        Text(s["editor.pastNote"], style = Type.caption, color = c.text, modifier = Modifier.weight(1f))
      }
    }

    // Color
    SectionLabel(s["editor.color"])
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
      for (color in CountdownColor.entries) {
        val active = color == draft.color
        val p = color.palette
        Box(
          Modifier.size(TOUCH_TARGET).selectable(active, role = Role.RadioButton) { draft = draft.copy(color = color) }.semantics { contentDescription = s["editor.colors.${color.id}"] },
          contentAlignment = Alignment.Center,
        ) {
          Box(Modifier.size(46.dp).border(2.5.dp, if (active) p.to else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(p.to), contentAlignment = Alignment.Center) {
              if (active) LucideIcon(LucideIcons.Check, Color.White, 18.dp, strokeWidth = 3f)
            }
          }
        }
      }
    }

    // Icon
    SectionLabel(s["editor.icon"])
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
      for (icon in CountdownIcon.entries) {
        val active = icon == draft.icon
        Box(
          Modifier
            .size(TOUCH_TARGET)
            .clip(RoundedCornerShape(Radius.md))
            .background(if (active) draft.color.palette.to else c.surfaceAlt)
            .selectable(active, role = Role.RadioButton) { draft = draft.copy(icon = icon) }
            .semantics { contentDescription = s["editor.icons.${icon.id}"] },
          contentAlignment = Alignment.Center,
        ) {
          LucideIcon(icon.lucide, if (active) Color.White else c.textMuted, 22.dp)
        }
      }
    }

    // Pin
    if (!isPinned) {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(top = Spacing.xl)
          .clip(RoundedCornerShape(Radius.md))
          .background(c.surfaceAlt)
          .clickable(role = Role.Switch) { draft = draft.copy(pin = !draft.pin) }
          .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
      ) {
        Column(Modifier.weight(1f)) {
          Text(s["editor.pin"], style = Type.bodyStrong, color = c.text)
          Text(s["editor.pinHint"], style = Type.caption, color = c.textMuted)
        }
        Switch(
          checked = draft.pin,
          onCheckedChange = null,
          colors = SwitchDefaults.colors(
            checkedTrackColor = c.primary,
            checkedThumbColor = c.onPrimary,
            uncheckedTrackColor = c.switchTrackOff,
            uncheckedThumbColor = c.switchThumbOff,
            uncheckedBorderColor = c.switchTrackOff,
          ),
        )
      }
    }

    if (countdown != null) {
      DangerButton(LucideIcons.Trash2, s["editor.delete"], onClick = { confirmDelete = true }, modifier = Modifier.padding(top = Spacing.xl))
    }
  }

  // Cancel / Save
  Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.border))
  Row(Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
    ActionButton(s["common.cancel"], c.surfaceAlt, c.text, Modifier.weight(1f), Sheets::closeEditor)
    ActionButton(s["common.save"], c.primary, c.onPrimary, Modifier.weight(1f), ::save)
  }

  if (confirmDelete && countdown != null) {
    ConfirmDialog(
      title = s["editor.deleteTitle"],
      body = s["editor.deleteBody"],
      confirm = s["common.delete"],
      onConfirm = {
        confirmDelete = false
        AppStore.updateCountdowns { it.remove(countdown.id, System.currentTimeMillis()) }
        Toasts.show(s["toast.deleted"])
        Sheets.closeEditor()
      },
      onDismiss = { confirmDelete = false },
    )
  }
}

@Composable
private fun ActionButton(label: String, background: Color, color: Color, modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier.heightIn(min = TOUCH_TARGET).clip(RoundedCornerShape(Radius.md)).background(background).pressable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, style = Type.bodyStrong, color = color)
  }
}

@Composable
private fun WheelColumn(label: String, modifier: Modifier, content: @Composable () -> Unit) {
  Column(modifier) {
    Text(label, style = Type.caption, color = appTheme().colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs))
    content()
  }
}

/** Day / month / year wheels. A day past the new month's end is clamped (31 → 30 Sep). Mirrors in
 * Arabic, so the day sits on the right as Arabic dates are read. */
@Composable
private fun DateWheels(value: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
  val theme = appTheme()
  val s = theme.strings
  val lang = theme.lang
  val dayCount = daysInMonth(value.year, value.month)
  val days = remember(dayCount, lang) { (1..dayCount).map { localizeDigits(it, lang) } }
  val months = remember(lang) { (1..12).map { monthName(it, lang) } }
  val years = remember(lang) { (YEAR_MIN..YEAR_MAX).map { localizeDigits(it, lang) } }

  fun set(year: Int = value.year, month: Int = value.month, day: Int = value.day) =
    onChange(value.copy(year = year, month = month, day = minOf(day, daysInMonth(year, month))))

  Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    WheelColumn(s["editor.day"], Modifier.weight(0.8f)) { WheelPicker(days, value.day - 1, { set(day = it + 1) }, s["editor.day"]) }
    WheelColumn(s["editor.month"], Modifier.weight(1.6f)) { WheelPicker(months, value.month - 1, { set(month = it + 1) }, s["editor.month"]) }
    WheelColumn(s["editor.year"], Modifier.weight(1f)) { WheelPicker(years, value.year - YEAR_MIN, { set(year = YEAR_MIN + it) }, s["editor.year"]) }
  }
}

/** Hour / minute wheels, 24-hour ("18") or 12-hour ("6 PM") per Settings. */
@Composable
private fun TimeWheels(value: LocalDateTime, use24h: Boolean, onChange: (LocalDateTime) -> Unit) {
  val theme = appTheme()
  val s = theme.strings
  val lang = theme.lang
  val hours = remember(use24h, lang) {
    (0..23).map { h -> if (use24h) localizeDigits(pad2(h), lang) else localizeDigits("${hour12(h)} ${dayPeriod(h, lang)}", lang) }
  }
  val minutes = remember(lang) { (0..59).map { localizeDigits(pad2(it), lang) } }
  Row(Modifier.padding(horizontal = Spacing.xxl), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    WheelColumn(s["editor.hour"], Modifier.weight(1f)) { WheelPicker(hours, value.hour, { onChange(value.copy(hour = it)) }, s["editor.hour"]) }
    WheelColumn(s["editor.minute"], Modifier.weight(1f)) { WheelPicker(minutes, value.minute, { onChange(value.copy(minute = it)) }, s["editor.minute"]) }
  }
}
