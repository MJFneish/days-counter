package com.devlance.dayscounter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.core.relativePhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.data.sortCountdowns
import com.devlance.dayscounter.ui.BottomSheet
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.Sheets
import com.devlance.dayscounter.ui.components.Card
import com.devlance.dayscounter.ui.components.IconBadge
import com.devlance.dayscounter.ui.components.SectionLabel
import com.devlance.dayscounter.ui.components.Toasts
import com.devlance.dayscounter.ui.components.pressable
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIconData
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.icons.lucide
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.TOUCH_TARGET
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import com.devlance.dayscounter.ui.theme.palette
import com.devlance.dayscounter.widget.DaysWidgetProvider

/**
 * Settings → Home-screen widget (v1's WidgetSection): which countdown it shows ("Change" opens the
 * picker sheet), a button that asks the launcher to place the widget (manual steps where the
 * launcher can't), the resize hint and the lock-screen note.
 */
@Composable
fun WidgetSection() {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val context = LocalContext.current
  val now = LocalNow.current
  val state by AppStore.countdowns.collectAsStateWithLifecycle()
  val pinned = state.pinned
  var canRequestPin by remember { mutableStateOf(false) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  // Re-checked on every return to the app (the user may have switched launchers).
  LaunchedEffect(lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { canRequestPin = DaysWidgetProvider.canRequestPin(context) }
  }

  SectionLabel(s["settings.widget"])
  Card {
    Text(s["settings.widgetShows"], style = Type.caption, color = c.textMuted)
    Row(Modifier.fillMaxWidth().padding(top = Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      val p = pinned?.color?.palette
      IconBadge(pinned?.icon?.lucide ?: LucideIcons.LayoutGrid, p?.accent(theme.isDark) ?: c.primary, p?.soft(theme.isDark) ?: c.primarySoft)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(pinned?.let { countdownTitle(it, theme.lang) } ?: s["settings.widgetNone"], style = Type.bodyStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (pinned != null) Text(relativePhrase(remaining(pinned.target, now), s), style = Type.caption, color = c.textMuted)
      }
      if (state.countdowns.size > 1) {
        Box(
          Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(c.surfaceAlt)
            .clickable(role = Role.Button) { Sheets.picker = true }
            .semantics { contentDescription = "${s["common.change"]} — ${s["settings.chooseCountdown"]}" }
            .padding(horizontal = Spacing.md),
          contentAlignment = Alignment.Center,
        ) {
          Text(s["common.change"], style = Type.bodyStrong, color = c.primary)
        }
      }
    }

    if (canRequestPin) {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(top = Spacing.md)
          .heightIn(min = TOUCH_TARGET)
          .clip(RoundedCornerShape(Radius.md))
          .background(c.primary)
          .pressable {
            if (!DaysWidgetProvider.requestPin(context)) Toasts.show(s["settings.addWidgetManual"])
          }
          .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LucideIcon(LucideIcons.LayoutGrid, c.onPrimary, 20.dp)
        Text(s["settings.addWidget"], style = Type.bodyStrong, color = c.onPrimary, textAlign = TextAlign.Center)
      }
    } else {
      Note(LucideIcons.Info, s["settings.addWidgetManual"])
    }
    Note(LucideIcons.Info, s["settings.addWidgetHint"])
    Note(LucideIcons.Lock, s["settings.lockScreenNote"])
  }
}

@Composable
private fun Note(icon: LucideIconData, text: String) {
  val c = appTheme().colors
  Row(Modifier.fillMaxWidth().padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    LucideIcon(icon, c.textMuted, 16.dp, modifier = Modifier.padding(top = 1.dp))
    Text(text, style = Type.caption, color = c.textMuted, modifier = Modifier.weight(1f))
  }
}

/** Every countdown as a radio, to choose the one on the widget (and the Home hero). Applies on tap.
 * Opened from the widget section; mounted once over the app (AppRoot). */
@Composable
fun CountdownPickerSheet() {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val now = LocalNow.current
  val state by AppStore.countdowns.collectAsStateWithLifecycle()
  BottomSheet(visible = Sheets.picker, onClose = { Sheets.picker = false }, maxHeightFraction = 0.8f) {
    Text(
      s["settings.chooseCountdown"],
      style = Type.title,
      color = c.text,
      modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.md).semantics { heading() },
    )
    Column(
      Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg).selectableGroup(),
      verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
      for (countdown in sortCountdowns(state.countdowns, now)) {
        val active = countdown.id == state.pinnedId
        val p = countdown.color.palette
        val shape = RoundedCornerShape(Radius.lg)
        Row(
          Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (active) c.primarySoft else Color.Transparent)
            .border(1.5.dp, if (active) c.primary else c.border, shape)
            .selectable(active, role = Role.RadioButton) {
              AppStore.updateCountdowns { it.pin(countdown.id) }
              Sheets.picker = false
              Toasts.show(s["toast.pinned"])
            }
            .padding(Spacing.md),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
          IconBadge(countdown.icon.lucide, p.accent(theme.isDark), p.soft(theme.isDark))
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(countdownTitle(countdown, theme.lang), style = Type.bodyStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(relativePhrase(remaining(countdown.target, now), s), style = Type.caption, color = c.textMuted)
          }
          LucideIcon(if (active) LucideIcons.CircleCheck else LucideIcons.Circle, if (active) c.primary else c.textMuted, 22.dp)
        }
      }
    }
  }
}
