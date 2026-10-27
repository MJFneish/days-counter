package com.devlance.dayscounter.ui.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.core.CountdownIcon
import java.util.concurrent.ConcurrentHashMap

/** One Lucide icon: its shapes as SVG path data on a 24×24 grid (LucideIcons.kt is generated). */
class LucideIconData(vararg val paths: String)

private val cache = ConcurrentHashMap<Pair<LucideIconData, Float>, ImageVector>()

private fun vectorOf(icon: LucideIconData, strokeWidth: Float): ImageVector = cache.getOrPut(icon to strokeWidth) {
  ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
    for (d in icon.paths) {
      addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      )
    }
  }.build()
}

/** A Lucide icon (stroke 2 by default, like v1's lucide-react-native). Decorative unless a
 * `contentDescription` is given. */
@Composable
fun LucideIcon(icon: LucideIconData, tint: Color, size: Dp = 24.dp, strokeWidth: Float = 2f, contentDescription: String? = null, modifier: Modifier = Modifier) {
  val vector = remember(icon, strokeWidth) { vectorOf(icon, strokeWidth) }
  Icon(vector, contentDescription, modifier.size(size), tint = tint)
}

/** The icon a countdown can carry (editor grid, hero card, list rows) — v1's COUNTDOWN_ICON_COMPONENTS. */
val CountdownIcon.lucide: LucideIconData
  get() = when (this) {
    CountdownIcon.CALENDAR -> LucideIcons.CalendarDays
    CountdownIcon.STAR -> LucideIcons.Star
    CountdownIcon.HEART -> LucideIcons.Heart
    CountdownIcon.CAKE -> LucideIcons.Cake
    CountdownIcon.PLANE -> LucideIcons.Plane
    CountdownIcon.GRADUATION -> LucideIcons.GraduationCap
    CountdownIcon.BRIEFCASE -> LucideIcons.Briefcase
    CountdownIcon.HOME -> LucideIcons.House
    CountdownIcon.BABY -> LucideIcons.Baby
    CountdownIcon.TROPHY -> LucideIcons.Trophy
    CountdownIcon.GIFT -> LucideIcons.Gift
    CountdownIcon.FLAG -> LucideIcons.Flag
  }
