package com.devlance.dayscounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.data.Settings
import com.devlance.dayscounter.data.ThemeMode
import com.devlance.dayscounter.i18n.Strings

/**
 * "Dusk" — an ember-orange primary with a deep violet secondary, the two ends of a dusk-to-dawn
 * gradient (time passing). From the ui-ux-pro-max recommendation for a countdown/event app, with
 * orange leading so the app stays distinct from its siblings (the Hijri converter is indigo,
 * Mawakit emerald). Every text-bearing pair is ≥ 4.5:1 in its theme. Same tokens as v1's colors.ts
 * and the iOS app's Theme.swift.
 */
@Immutable
data class AppColors(
  val background: Color,
  val surface: Color,
  val surfaceAlt: Color,
  val primary: Color,
  val primaryDark: Color,
  val primarySoft: Color,
  val onPrimary: Color,
  val secondary: Color,
  val secondarySoft: Color,
  val text: Color,
  val textMuted: Color,
  val border: Color,
  val card: Color,
  val cardShadow: Color,
  val danger: Color,
  val dangerSoft: Color,
  val heroFrom: Color,
  val heroTo: Color,
  val onHero: Color,
  val onHeroMuted: Color,
  /** Translucent white panels on a gradient (the H/M/S tiles). */
  val onHeroPanel: Color,
  val scrim: Color,
  /** Switch "off" state — the default surfaces are too close to the card colour to read. */
  val switchTrackOff: Color,
  val switchThumbOff: Color,
)

val LightColors = AppColors(
  background = Color(0xFFFFF8F4),
  surface = Color(0xFFFFFFFF),
  surfaceAlt = Color(0xFFF8ECE6),
  primary = Color(0xFFC2410C),
  primaryDark = Color(0xFF9A3412),
  primarySoft = Color(0xFFFDE7DC),
  onPrimary = Color(0xFFFFFFFF),
  secondary = Color(0xFF6D28D9),
  secondarySoft = Color(0xFFEDE5FD),
  text = Color(0xFF1E1428),
  textMuted = Color(0xFF6A5D72),
  border = Color(0xFFEEDFD6),
  card = Color(0xFFFFFFFF),
  cardShadow = Color(120, 45, 18).copy(alpha = 0.14f),
  danger = Color(0xFFB91C1C),
  dangerSoft = Color(0xFFFDE2E2),
  heroFrom = Color(0xFF3B0764),
  heroTo = Color(0xFFC2410C),
  onHero = Color.White,
  onHeroMuted = Color.White.copy(alpha = 0.82f),
  onHeroPanel = Color.White.copy(alpha = 0.16f),
  scrim = Color(20, 10, 30).copy(alpha = 0.45f),
  switchTrackOff = Color(0xFFD9C9C0),
  switchThumbOff = Color(0xFFFFFFFF),
)

val DarkColors = AppColors(
  background = Color(0xFF120B18),
  surface = Color(0xFF1C1323),
  surfaceAlt = Color(0xFF271B30),
  primary = Color(0xFFFF8A50),
  primaryDark = Color(0xFFF97316),
  primarySoft = Color(0xFF3A1E14),
  onPrimary = Color(0xFF2A0D00),
  secondary = Color(0xFFB794F6),
  secondarySoft = Color(0xFF2A1E45),
  text = Color(0xFFFBF3F8),
  textMuted = Color(0xFFB6A8BE),
  border = Color(0xFF33243D),
  card = Color(0xFF1E1526),
  cardShadow = Color.Black.copy(alpha = 0.5f),
  danger = Color(0xFFF87171),
  dangerSoft = Color(0xFF3B1717),
  heroFrom = Color(0xFF2E0752),
  heroTo = Color(0xFFB23A0C),
  onHero = Color.White,
  onHeroMuted = Color.White.copy(alpha = 0.8f),
  onHeroPanel = Color.White.copy(alpha = 0.14f),
  scrim = Color.Black.copy(alpha = 0.6f),
  switchTrackOff = Color(0xFF3E2F49),
  switchThumbOff = Color(0xFFB6A8BE),
)

/** Fixed brand colors for the icon, the splash and the loading screen (same in both themes). */
object Brand {
  val night = Color(0xFF170B26)
  val violet = Color(0xFF6D28D9)
  val ember = Color(0xFFF97316)
  val gold = Color(0xFFFFC56E)
  val goldLight = Color(0xFFFFE0A3)
  val goldDeep = Color(0xFFFF9A4D)
  val light = Color(0xFFFFF4EC)
  val tileFrom = Color(0xFF3B0764)
  val tileTo = Color(0xFFC2410C)
}

/** System fonts on purpose: they ship proper Arabic shaping for free. Body text stays >= 16. Numbers
 * that tick use tabular figures ("tnum"), so a changing digit never shifts its neighbours. */
@Immutable
object Type {
  private const val TABULAR = "tnum"
  val hero = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, fontFeatureSettings = TABULAR)
  val displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
  val displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
  val digits = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR)
  val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
  val subtitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
  val body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
  val bodyStrong = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
  val caption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
  val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}

/** 4/8 dp rhythm (same scale as Mawakit). */
object Spacing {
  val xs: Dp = 4.dp
  val sm: Dp = 8.dp
  val md: Dp = 12.dp
  val lg: Dp = 16.dp
  val xl: Dp = 24.dp
  val xxl: Dp = 32.dp
  val xxxl: Dp = 48.dp
}

object Radius {
  val sm: Dp = 8.dp
  val md: Dp = 14.dp
  val lg: Dp = 20.dp
  val xl: Dp = 28.dp
}

/** Minimum touch target (Android 48 dp guideline). */
val TOUCH_TARGET: Dp = 48.dp

/** Everything a screen needs to draw itself in the user's choices. */
@Immutable
data class AppTheme(val colors: AppColors, val isDark: Boolean, val strings: Strings) {
  val lang: Lang get() = strings.lang
  val isRtl: Boolean get() = lang == Lang.AR
}

val LocalAppTheme = staticCompositionLocalOf<AppTheme> { error("DaysCounterTheme not set") }

/** Shorthand inside composables: `val theme = appTheme()`. */
@Composable
fun appTheme(): AppTheme = LocalAppTheme.current

@Composable
fun isDarkFor(settings: Settings): Boolean = when (settings.themeMode) {
  ThemeMode.SYSTEM -> isSystemInDarkTheme()
  ThemeMode.LIGHT -> false
  ThemeMode.DARK -> true
}

@Composable
fun DaysCounterTheme(settings: Settings, content: @Composable () -> Unit) {
  val isDark = isDarkFor(settings)
  val theme = remember(isDark, settings.language) {
    AppTheme(if (isDark) DarkColors else LightColors, isDark, Strings.of(settings.language))
  }
  val c = theme.colors
  val scheme = if (isDark) {
    darkColorScheme(primary = c.primary, onPrimary = c.onPrimary, background = c.background, surface = c.surface, onSurface = c.text, onBackground = c.text)
  } else {
    lightColorScheme(primary = c.primary, onPrimary = c.onPrimary, background = c.background, surface = c.surface, onSurface = c.text, onBackground = c.text)
  }
  MaterialTheme(colorScheme = scheme) {
    // RTL follows the app's language, not the phone's, and flips instantly when it changes.
    CompositionLocalProvider(
      LocalAppTheme provides theme,
      LocalLayoutDirection provides if (theme.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
      content = content,
    )
  }
}
