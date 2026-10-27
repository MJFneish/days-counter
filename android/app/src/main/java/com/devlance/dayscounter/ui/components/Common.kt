package com.devlance.dayscounter.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIconData
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.TOUCH_TARGET
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme

/** A rounded card on the surface colour: a soft shadow in light mode, a hairline border in dark. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val theme = appTheme()
  val shape = RoundedCornerShape(Radius.lg)
  Column(
    modifier
      .fillMaxWidth()
      .then(if (theme.isDark) Modifier else Modifier.shadow(2.dp, shape, ambientColor = theme.colors.cardShadow, spotColor = theme.colors.cardShadow))
      .clip(shape)
      .background(theme.colors.card)
      .then(if (theme.isDark) Modifier.border(1.dp, theme.colors.border, shape) else Modifier)
      .padding(Spacing.lg),
    content = content,
  )
}

/** Uppercase section label (not in Arabic, which has no case). */
@Composable
fun SectionLabel(text: String, first: Boolean = false) {
  val theme = appTheme()
  Text(
    if (theme.isRtl) text else text.uppercase(),
    style = if (theme.isRtl) Type.label.copy(letterSpacing = 0.sp) else Type.label,
    color = theme.colors.textMuted,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = if (first) Spacing.lg else Spacing.xl, bottom = Spacing.sm)
      .semantics { heading() },
  )
}

/** Press feedback by scaling to 96 %, like v1's PressableScale. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(if (pressed) 90 else 140), label = "press")
  return this.scale(scale)
}

/** A tappable surface that scales slightly while pressed. */
@Composable
fun Modifier.pressable(role: Role = Role.Button, label: String? = null, onClick: () -> Unit): Modifier {
  val interaction = remember { MutableInteractionSource() }
  return this
    .pressScale(interaction)
    .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = role, onClickLabel = label, onClick = onClick)
}

/** A rounded icon badge (Settings rows, list rows). */
@Composable
fun IconBadge(icon: LucideIconData, tint: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp = 40.dp, iconSize: androidx.compose.ui.unit.Dp = 20.dp) {
  Box(Modifier.size(size).clip(RoundedCornerShape(Radius.md)).background(background), contentAlignment = Alignment.Center) {
    LucideIcon(icon, tint, iconSize)
  }
}

/** A labelled switch row (icon, title, hint) — the shape every Settings toggle shares. */
@Composable
fun SwitchRow(icon: LucideIconData, title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = TOUCH_TARGET)
      .clip(RoundedCornerShape(Radius.sm))
      .clickable(role = Role.Switch) { onChange(!checked) },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
  ) {
    IconBadge(icon, c.primary, c.primarySoft)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = Type.bodyStrong, color = c.text)
      Text(hint, style = Type.caption, color = c.textMuted)
    }
    Switch(
      checked = checked,
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

/** A hairline divider between rows of a card. */
@Composable
fun Divider() {
  Box(Modifier.padding(vertical = Spacing.md).fillMaxWidth().height(0.5.dp).background(appTheme().colors.border))
}

data class SegmentOption<T>(val value: T, val label: String)

/** Pill segmented control with a sliding selection indicator. Mirrors in RTL (the layout does). */
@Composable
fun <T> SegmentedControl(options: List<SegmentOption<T>>, value: T, onChange: (T) -> Unit, modifier: Modifier = Modifier) {
  val theme = appTheme()
  val c = theme.colors
  val selected = options.indexOfFirst { it.value == value }.coerceAtLeast(0)
  BoxWithConstraints(
    modifier
      .fillMaxWidth()
      .clip(CircleShape)
      .background(c.surfaceAlt)
      .padding(4.dp),
  ) {
    val segment = maxWidth / options.size
    val offset by animateDpAsState(segment * selected, tween(220), label = "segment")
    Box(
      Modifier
        .offset(x = offset)
        .width(segment)
        .heightIn(min = 44.dp)
        .clip(CircleShape)
        .background(c.primary),
    )
    Row(Modifier.fillMaxWidth().selectableGroup()) {
      options.forEachIndexed { i, option ->
        val active = i == selected
        Box(
          Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .selectable(selected = active, role = Role.RadioButton) { onChange(option.value) },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            option.label,
            style = Type.bodyStrong.copy(fontSize = 15.sp),
            color = if (active) c.onPrimary else c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.sm),
          )
        }
      }
    }
  }
}

/** Full-width outlined danger button (delete all / delete countdown). */
@Composable
fun DangerButton(icon: LucideIconData, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val c = appTheme().colors
  val shape = RoundedCornerShape(Radius.md)
  Row(
    modifier
      .fillMaxWidth()
      .heightIn(min = TOUCH_TARGET)
      .clip(shape)
      .border(1.5.dp, c.danger, shape)
      .clickable(role = Role.Button, onClick = onClick),
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LucideIcon(icon, c.danger, 18.dp)
    Text(label, style = Type.bodyStrong, color = c.danger)
  }
}
