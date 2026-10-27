package com.devlance.dayscounter.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountUnit
import com.devlance.dayscounter.core.countPhrase
import com.devlance.dayscounter.core.localizeDigits
import com.devlance.dayscounter.core.pad2
import com.devlance.dayscounter.core.spanPhrase
import com.devlance.dayscounter.core.totalHours
import com.devlance.dayscounter.core.unitWord
import com.devlance.dayscounter.core.weeksPhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.components.Card
import com.devlance.dayscounter.ui.components.IconBadge
import com.devlance.dayscounter.ui.icons.LucideIconData
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.theme.CountdownPalette
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import com.devlance.dayscounter.ui.theme.palette

/** Decorative soft circles on a gradient: x, y (fractions of the size), radius (fraction of the
 * width), opacity — v1's GradientFill circles. */
val HERO_CIRCLES = listOf(floatArrayOf(0.95f, 0.05f, 0.32f, 0.08f), floatArrayOf(0.02f, 1.02f, 0.26f, 0.06f), floatArrayOf(0.72f, 0.78f, 0.08f, 0.05f))

/** The countdown's diagonal gradient (top-left → bottom-right in both languages, like v1) with
 * its soft circles. */
fun Modifier.gradientFill(palette: CountdownPalette, circles: List<FloatArray> = emptyList()): Modifier = drawBehind {
  drawRect(Brush.linearGradient(listOf(palette.from, palette.to), Offset.Zero, Offset(size.width, size.height)))
  for (c in circles) drawCircle(Color.White.copy(alpha = c[3]), c[2] * size.width, Offset(c[0] * size.width, c[1] * size.height))
}

/** Fills from the reading start: left in English, right in Arabic (the layout direction does it). */
@Composable
fun ProgressBar(value: Float, color: Color, track: Color, height: Dp = 6.dp, modifier: Modifier = Modifier) {
  val animated by animateFloatAsState(value, tween(600), label = "progress")
  Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
    Box(Modifier.fillMaxWidth(animated.coerceIn(0.02f, 1f)).height(height).clip(CircleShape).background(color))
  }
}

/** Text that slides in from above when its value changes (an odometer rolling down). */
@Composable
fun RollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
  AnimatedContent(
    targetState = text,
    transitionSpec = { (slideInVertically(tween(220)) { -it / 2 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120)) },
    label = "roll",
    modifier = modifier,
  ) { value ->
    Text(value, style = style, color = color, maxLines = 1)
  }
}

/** One H/M/S tile on the hero. */
@Composable
fun TimeTile(value: Int, unit: CountUnit, modifier: Modifier = Modifier) {
  val theme = appTheme()
  val c = theme.colors
  Column(
    modifier
      .clip(RoundedCornerShape(Radius.md))
      .background(c.onHeroPanel)
      .padding(vertical = Spacing.sm)
      .clearAndSetSemantics { contentDescription = countPhrase(unit, value, theme.lang) },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
      RollingText(localizeDigits(pad2(value), theme.lang), Type.digits, c.onHero)
    }
    Text(unitWord(unit, value, theme.lang), style = Type.caption, color = c.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

/** "The wait in numbers" under the hero: the same wait on the calendar, in weeks and in hours.
 * Rows that would read "0" (under a day / a week) are left out. */
@Composable
fun StatsCard(countdown: Countdown) {
  val theme = appTheme()
  val c = theme.colors
  val r = remaining(countdown.target, LocalNow.current)
  val p = countdown.color.palette
  val span = spanPhrase(countdown.target, r, theme.lang)
  val weeks = weeksPhrase(r, theme.lang)
  Card {
    Text(theme.strings["home.details"], style = Type.label.copy(letterSpacing = Type.caption.letterSpacing), color = c.textMuted)
    Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
      if (span.isNotEmpty()) Stat(LucideIcons.CalendarDays, theme.strings["home.statSpan"], span, p.accent(theme.isDark), p.soft(theme.isDark))
      if (weeks.isNotEmpty()) Stat(LucideIcons.CalendarClock, theme.strings["home.statWeeks"], weeks, p.accent(theme.isDark), p.soft(theme.isDark))
      Stat(LucideIcons.Clock, theme.strings["home.statHours"], totalHours(r, theme.lang), p.accent(theme.isDark), p.soft(theme.isDark))
    }
  }
}

@Composable
private fun Stat(icon: LucideIconData, label: String, value: String, accent: Color, soft: Color) {
  val c = appTheme().colors
  Row(
    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
  ) {
    IconBadge(icon, accent, soft, size = 36.dp, iconSize = 18.dp)
    Column(Modifier.weight(1f)) {
      Text(label, style = Type.caption, color = c.textMuted)
      Text(value, style = Type.bodyStrong, color = c.text)
    }
  }
}

/** A round icon bubble on a gradient (hero header, editor preview). */
@Composable
fun HeroBubble(icon: LucideIconData, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
  val c = appTheme().colors
  Box(Modifier.size(size).clip(CircleShape).background(c.onHeroPanel), contentAlignment = Alignment.Center) {
    com.devlance.dayscounter.ui.icons.LucideIcon(icon, c.onHero, iconSize)
  }
}
