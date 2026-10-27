package com.devlance.dayscounter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownStatus
import com.devlance.dayscounter.core.CountUnit
import com.devlance.dayscounter.core.countdownSubtitle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.core.formatNumber
import com.devlance.dayscounter.core.formatPercent
import com.devlance.dayscounter.core.progress
import com.devlance.dayscounter.core.relativePhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.core.unitWord
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.components.pressable
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.icons.lucide
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import com.devlance.dayscounter.ui.theme.palette
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlin.math.floor

/**
 * The pinned countdown, big (v1's HeroCountdown): icon + name, the day count, live H/M/S tiles and
 * the progress of the wait — in the countdown's own gradient. The same countdown the widget shows.
 * Tapping it opens the editor.
 */
@Composable
fun HeroCountdown(countdown: Countdown, onClick: () -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val settings by AppStore.settings.collectAsStateWithLifecycle()
  val now = LocalNow.current
  val r = remaining(countdown.target, now)
  val title = countdownTitle(countdown, theme.lang)
  val subtitle = countdownSubtitle(countdown, s, settings.use24h)
  val passed = r.status != CountdownStatus.UPCOMING
  val showCount = !passed || settings.countUp
  val pct = progress(countdown.target, countdown.createdAt, now)

  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Radius.xl))
      .pressable(label = s.t("home.edit", "name" to title), onClick = onClick)
      .gradientFill(countdown.color.palette, HERO_CIRCLES)
      .padding(Spacing.lg)
      .semantics { contentDescription = "$title. ${relativePhrase(r, s)}" },
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      HeroBubble(countdown.icon.lucide)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = Type.subtitle, color = c.onHero, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotEmpty()) Text(subtitle, style = Type.caption, color = c.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Row(
        Modifier.align(Alignment.Top).clip(CircleShape).background(c.onHeroPanel).padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        LucideIcon(LucideIcons.Pin, c.onHero, 13.dp)
        Text(s["home.pinned"], style = Type.label.copy(letterSpacing = Type.caption.letterSpacing), color = c.onHero)
      }
    }

    if (r.status == CountdownStatus.ARRIVED) {
      Row(Modifier.padding(top = Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        LucideIcon(LucideIcons.PartyPopper, c.onHero, 22.dp)
        Text(s["countdown.arrived"], style = Type.title, color = c.onHero)
      }
    }

    if (showCount) {
      // The number above its label, not beside it: side by side, the label wrapped and lost a word
      // in Arabic (seen on v1's first emulator run) and a 5-digit count would crowd it out.
      Column(Modifier.padding(top = Spacing.md)) {
        RollingText(formatNumber(r.days, theme.lang), Type.hero, c.onHero)
        Text("${unitWord(CountUnit.DAY, r.days, theme.lang)} ${if (passed) s["countdown.since"] else s["countdown.toGo"]}", style = Type.subtitle, color = c.onHeroMuted)
      }
      Row(Modifier.fillMaxWidth().padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TimeTile(r.hours, CountUnit.HOUR, Modifier.weight(1f))
        TimeTile(r.minutes, CountUnit.MINUTE, Modifier.weight(1f))
        if (settings.showSeconds) TimeTile(r.seconds, CountUnit.SECOND, Modifier.weight(1f))
      }
    } else {
      Text(s["countdown.doneNoCountUp"], style = Type.title, color = c.onHero, modifier = Modifier.padding(top = Spacing.lg))
    }

    if (!passed) {
      Column(Modifier.padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ProgressBar(pct.toFloat(), c.onHero, c.onHeroPanel)
        Text(s.t("countdown.progress", "percent" to formatPercent(floor(pct * 100).toInt(), theme.lang)), style = Type.caption, color = c.onHeroMuted)
      }
    }
    if (r.status == CountdownStatus.ARRIVED && settings.countUp) {
      Text(s["countdown.arrivedBody"], style = Type.caption, color = c.onHeroMuted, modifier = Modifier.padding(top = Spacing.lg))
    }
  }
}
