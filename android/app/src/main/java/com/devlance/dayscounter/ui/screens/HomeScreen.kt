package com.devlance.dayscounter.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.core.CountdownStatus
import com.devlance.dayscounter.core.formatLongDate
import com.devlance.dayscounter.core.fromInstant
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.data.sortCountdowns
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.Sheets
import com.devlance.dayscounter.ui.components.SectionLabel
import com.devlance.dayscounter.ui.components.Toasts
import com.devlance.dayscounter.ui.components.pressable
import com.devlance.dayscounter.ui.home.CountdownRow
import com.devlance.dayscounter.ui.home.EmptyState
import com.devlance.dayscounter.ui.home.HeroCountdown
import com.devlance.dayscounter.ui.home.StatsCard
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.TOUCH_TARGET
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme

/**
 * Home (v1's HomeScreen): today's date, the pinned countdown as the hero (the one on the widget)
 * with "the wait in numbers", then every other countdown — upcoming soonest first, passed ones
 * after. Tapping any of them opens the editor sheet; the pin button moves one to the hero + widget.
 */
@Composable
fun HomeScreen() {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val now = LocalNow.current
  val state by AppStore.countdowns.collectAsStateWithLifecycle()
  val settings by AppStore.settings.collectAsStateWithLifecycle()
  val pinned = state.pinned
  val others = sortCountdowns(state.countdowns.filter { it.id != pinned?.id }, now)

  Column(
    Modifier
      .fillMaxSize()
      .background(c.background)
      .windowInsetsPadding(WindowInsets.statusBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = Spacing.lg)
      .padding(top = Spacing.md, bottom = Spacing.xxxl),
  ) {
    Row(Modifier.padding(bottom = Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      Column(Modifier.weight(1f)) {
        Text(s["home.title"], style = Type.displayMedium, color = c.text, modifier = Modifier.semantics { heading() })
        Text(formatLongDate(fromInstant(now), theme.lang), style = Type.caption, color = c.textMuted)
      }
      Box(
        Modifier
          .size(TOUCH_TARGET)
          .clip(CircleShape)
          .background(c.primary)
          .pressable(label = s["home.add"]) { Sheets.openEditor(null) }
          .semantics { contentDescription = s["home.add"] },
        contentAlignment = Alignment.Center,
      ) {
        LucideIcon(LucideIcons.Plus, c.onPrimary, 24.dp)
      }
    }

    if (pinned != null) {
      HeroCountdown(pinned) { Sheets.openEditor(pinned.id) }
      if (remaining(pinned.target, now).status == CountdownStatus.UPCOMING || settings.countUp) {
        Box(Modifier.padding(top = Spacing.md)) { StatsCard(pinned) }
      }
    } else {
      EmptyState { Sheets.openEditor(null) }
    }

    if (others.isNotEmpty()) {
      SectionLabel(s["home.others"])
      Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (countdown in others) {
          androidx.compose.runtime.key(countdown.id) {
            CountdownRow(
              countdown,
              onClick = { Sheets.openEditor(countdown.id) },
              onPin = {
                AppStore.updateCountdowns { it.pin(countdown.id) }
                Toasts.show(s["toast.pinned"])
              },
            )
          }
        }
      }
    }
  }
}
