package com.devlance.dayscounter.widget

import com.devlance.dayscounter.R

/** Countdown color id → widget background (res/drawable/widget_bg_*.xml). Each drawable is the
 * same dark→mid gradient as `CountdownPalette.kt` — keep in sync. */
object WidgetColors {
  fun background(color: String): Int = when (color) {
    "violet" -> R.drawable.widget_bg_violet
    "ocean" -> R.drawable.widget_bg_ocean
    "teal" -> R.drawable.widget_bg_teal
    "rose" -> R.drawable.widget_bg_rose
    "forest" -> R.drawable.widget_bg_forest
    "indigo" -> R.drawable.widget_bg_indigo
    "gold" -> R.drawable.widget_bg_gold
    "graphite" -> R.drawable.widget_bg_graphite
    else -> R.drawable.widget_bg_ember
  }
}
