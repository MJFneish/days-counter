package com.devlance.dayscounter.ui.theme

import androidx.compose.ui.graphics.Color
import com.devlance.dayscounter.core.CountdownColor

/**
 * Per-countdown colors: a dark→mid gradient whose both ends carry white text at ≥ 4.5:1 (the hero
 * card and the widget), an accent for light and dark surfaces, and soft badge backgrounds. Same
 * values as v1's countdownColors.ts and the iOS app; the widget's res/drawable/widget_bg_*.xml carry
 * the same gradients.
 */
data class CountdownPalette(val from: Color, val to: Color, val light: Color, val dark: Color, val softLight: Color, val softDark: Color) {
  fun accent(isDark: Boolean): Color = if (isDark) dark else light
  fun soft(isDark: Boolean): Color = if (isDark) softDark else softLight
}

private fun palette(from: Long, to: Long, light: Long, dark: Long, softLight: Long, softDark: Long) =
  CountdownPalette(Color(from), Color(to), Color(light), Color(dark), Color(softLight), Color(softDark))

val COUNTDOWN_PALETTE: Map<CountdownColor, CountdownPalette> = mapOf(
  CountdownColor.EMBER to palette(0xFF7C2D12, 0xFFC2410C, 0xFFC2410C, 0xFFFF8A50, 0xFFFDE7DC, 0xFF3A1E14),
  CountdownColor.VIOLET to palette(0xFF3B0764, 0xFF7C3AED, 0xFF6D28D9, 0xFFB794F6, 0xFFEDE5FD, 0xFF2A1E45),
  CountdownColor.OCEAN to palette(0xFF0C4A6E, 0xFF0369A1, 0xFF0369A1, 0xFF7DD3FC, 0xFFE0F2FE, 0xFF0F2A3A),
  CountdownColor.TEAL to palette(0xFF134E4A, 0xFF0F766E, 0xFF0F766E, 0xFF5EEAD4, 0xFFDDF5F1, 0xFF0F2E2B),
  CountdownColor.ROSE to palette(0xFF881337, 0xFFE11D48, 0xFFBE123C, 0xFFFDA4AF, 0xFFFFE4E8, 0xFF3B1520),
  CountdownColor.FOREST to palette(0xFF14532D, 0xFF15803D, 0xFF15803D, 0xFF86EFAC, 0xFFDCF5E4, 0xFF13301E),
  CountdownColor.INDIGO to palette(0xFF312E81, 0xFF4F46E5, 0xFF4338CA, 0xFFA5B4FC, 0xFFE4E6FD, 0xFF1E1F45),
  CountdownColor.GOLD to palette(0xFF78350F, 0xFFB45309, 0xFFB45309, 0xFFFCD34D, 0xFFFDF0D5, 0xFF3A2A10),
  CountdownColor.GRAPHITE to palette(0xFF1E293B, 0xFF475569, 0xFF475569, 0xFFCBD5E1, 0xFFE8ECF1, 0xFF232B36),
)

val CountdownColor.palette: CountdownPalette get() = COUNTDOWN_PALETTE.getValue(this)
