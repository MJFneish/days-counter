package com.devlance.dayscounter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownStatus
import com.devlance.dayscounter.core.countdownSubtitle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.core.formatNumber
import com.devlance.dayscounter.core.headline
import com.devlance.dayscounter.core.progress
import com.devlance.dayscounter.core.relativePhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.core.unitWord
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.brand.AppLogo
import com.devlance.dayscounter.ui.components.IconBadge
import com.devlance.dayscounter.ui.components.pressable
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.icons.lucide
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.TOUCH_TARGET
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import com.devlance.dayscounter.ui.theme.palette

/**
 * A countdown in the Home list (v1's CountdownRow): icon, name, date, a thin progress line, the
 * headline number ("316 days" — hours/minutes on the last day) and a pin button that moves it to
 * the hero and the widget. Tapping the row opens the editor.
 */
@Composable
fun CountdownRow(countdown: Countdown, onClick: () -> Unit, onPin: () -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val settings by AppStore.settings.collectAsStateWithLifecycle()
  val now = LocalNow.current
  val r = remaining(countdown.target, now)
  val p = countdown.color.palette
  val accent = p.accent(theme.isDark)
  val soft = p.soft(theme.isDark)
  val title = countdownTitle(countdown, theme.lang)
  val subtitle = countdownSubtitle(countdown, s, settings.use24h, short = true)
  val (value, unit) = headline(r)
  val passed = r.status != CountdownStatus.UPCOMING
  val shape = RoundedCornerShape(Radius.lg)

  Row(
    Modifier
      .fillMaxWidth()
      .then(if (theme.isDark) Modifier else Modifier.shadow(2.dp, shape, ambientColor = c.cardShadow, spotColor = c.cardShadow))
      .clip(shape)
      .background(c.card)
      .then(if (theme.isDark) Modifier.border(1.dp, c.border, shape) else Modifier)
      .pressable(label = s.t("home.edit", "name" to title), onClick = onClick)
      .padding(Spacing.md)
      .semantics { contentDescription = "$title. ${relativePhrase(r, s)}" },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
  ) {
    IconBadge(countdown.icon.lucide, accent, soft, size = 44.dp, iconSize = 22.dp)

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = Type.bodyStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (subtitle.isNotEmpty()) Text(subtitle, style = Type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (!passed) {
        ProgressBar(progress(countdown.target, countdown.createdAt, now).toFloat(), accent, soft, height = 4.dp, modifier = Modifier.padding(top = Spacing.sm))
      }
    }

    Column(Modifier.widthIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      when {
        r.status == CountdownStatus.ARRIVED -> Text(s["countdown.today"], style = Type.bodyStrong, color = accent)
        passed && !settings.countUp -> Text(s["countdown.finished"], style = Type.caption, color = c.textMuted)
        else -> {
          Text(formatNumber(value, theme.lang), style = Type.digits.copy(fontSize = 24.sp), color = if (passed) c.textMuted else accent)
          Text(unitWord(unit, value, theme.lang), style = Type.caption, color = c.textMuted, maxLines = 1)
        }
      }
    }

    Box(
      Modifier
        .size(width = TOUCH_TARGET - 8.dp, height = TOUCH_TARGET)
        .clip(CircleShape)
        .clickable(role = Role.Button, onClickLabel = s.t("home.pin", "name" to title), onClick = onPin)
        .semantics { contentDescription = s.t("home.pin", "name" to title) },
      contentAlignment = Alignment.Center,
    ) {
      LucideIcon(LucideIcons.Pin, c.textMuted, 18.dp)
    }
  }
}

/** Nothing to count down to: the emblem, a line of encouragement and the add button. */
@Composable
fun EmptyState(onAdd: () -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  Column(
    Modifier.fillMaxWidth().padding(vertical = Spacing.xxxl, horizontal = Spacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Spacing.md),
  ) {
    Box(Modifier.padding(bottom = Spacing.sm).clip(RoundedCornerShape(Radius.xl)).background(c.surfaceAlt).padding(Spacing.lg)) {
      AppLogo(96.dp, tile = true)
    }
    Text(theme.strings["home.emptyTitle"], style = Type.title, color = c.text, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Text(theme.strings["home.emptyBody"], style = Type.body, color = c.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Row(
      Modifier
        .padding(top = Spacing.sm)
        .clip(CircleShape)
        .background(c.primary)
        .pressable(onClick = onAdd)
        .padding(horizontal = Spacing.xl, vertical = Spacing.md),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
      LucideIcon(LucideIcons.Plus, c.onPrimary, 20.dp)
      Text(theme.strings["home.add"], style = Type.bodyStrong, color = c.onPrimary)
    }
  }
}
