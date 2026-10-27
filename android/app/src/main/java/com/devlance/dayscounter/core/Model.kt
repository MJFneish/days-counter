package com.devlance.dayscounter.core

/** The app's language — its own setting, independent of the phone's. */
enum class Lang(val code: String) {
  EN("en"),
  AR("ar");

  companion object {
    fun of(code: String?): Lang = if (code == "ar") AR else EN
  }
}

/**
 * A wall-clock date-time in the device's time zone (month 1-12). Countdowns store this rather than
 * an epoch instant, so "5 Sep 2027 at 00:00" stays midnight local time even if the phone's time
 * zone or DST rules change between now and then.
 */
data class LocalDateTime(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)

/** A calendar date (month 1-12). */
data class DateOnly(val year: Int, val month: Int, val day: Int)

enum class CountdownColor(val id: String) {
  EMBER("ember"),
  VIOLET("violet"),
  OCEAN("ocean"),
  TEAL("teal"),
  ROSE("rose"),
  FOREST("forest"),
  INDIGO("indigo"),
  GOLD("gold"),
  GRAPHITE("graphite");

  companion object {
    fun of(id: String?): CountdownColor? = entries.firstOrNull { it.id == id }
  }
}

enum class CountdownIcon(val id: String) {
  CALENDAR("calendar"),
  STAR("star"),
  HEART("heart"),
  CAKE("cake"),
  PLANE("plane"),
  GRADUATION("graduation"),
  BRIEFCASE("briefcase"),
  HOME("home"),
  BABY("baby"),
  TROPHY("trophy"),
  GIFT("gift"),
  FLAG("flag");

  companion object {
    fun of(id: String?): CountdownIcon? = entries.firstOrNull { it.id == id }
  }
}

data class Countdown(
  val id: String,
  /** May be empty — the UI then shows the formatted target date as the title. */
  val title: String,
  val target: LocalDateTime,
  val color: CountdownColor,
  val icon: CountdownIcon,
  /** Epoch ms the countdown was created — the start of its progress bar. */
  val createdAt: Long,
)
