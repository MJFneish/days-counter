package com.devlance.dayscounter.data

import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownColor
import com.devlance.dayscounter.core.CountdownIcon
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.core.LocalDateTime
import com.devlance.dayscounter.core.toInstant
import java.util.TimeZone

enum class ThemeMode(val id: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun of(id: String?): ThemeMode? = entries.firstOrNull { it.id == id }
  }
}

data class Settings(
  val language: Lang,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  /** Tick the countdown cards every second (off = minutes only, ticking once a minute). */
  val showSeconds: Boolean = true,
  /** After a date passes, count the time since it (off = just "This date has passed"). */
  val countUp: Boolean = true,
  val use24h: Boolean,
)

/** What the editor hands the store. */
data class CountdownInput(val title: String, val target: LocalDateTime, val color: CountdownColor, val icon: CountdownIcon)

/** The user's own example (2026-10-23): count down to 5 September 2027. Seeded on first launch. */
val SEED_TARGET = LocalDateTime(2027, 9, 5, 0, 0)

/** Upcoming first (soonest on top), then passed ones (most recent first). */
fun sortCountdowns(list: List<Countdown>, now: Long, zone: TimeZone = TimeZone.getDefault()): List<Countdown> =
  list.sortedWith { a, b ->
    val ta = toInstant(a.target, zone)
    val tb = toInstant(b.target, zone)
    val pa = ta <= now
    val pb = tb <= now
    when {
      pa != pb -> if (pa) 1 else -1
      pa -> tb.compareTo(ta)
      else -> ta.compareTo(tb)
    }
  }

/**
 * The countdown list, as v1's countdown store kept it: `pinnedId` is the countdown on the Home hero
 * and the widget, always set while any countdown exists; `seeded` = the 5 Sep 2027 example was
 * added once, so deleting everything leaves an empty list instead of reviving it. Every change
 * returns a new state (the store persists it).
 */
data class CountdownsState(val countdowns: List<Countdown> = emptyList(), val pinnedId: String? = null, val seeded: Boolean = false) {
  /** The pinned countdown, or null when the list is empty. */
  val pinned: Countdown? get() = countdowns.firstOrNull { it.id == pinnedId } ?: countdowns.firstOrNull()

  fun add(input: CountdownInput, pin: Boolean, id: String, now: Long): CountdownsState {
    val countdown = Countdown(id, input.title.trim(), input.target, input.color, input.icon, now)
    return copy(countdowns = countdowns + countdown, pinnedId = if (pin || pinnedId == null) id else pinnedId)
  }

  fun update(id: String, input: CountdownInput): CountdownsState = copy(
    countdowns = countdowns.map {
      if (it.id == id) it.copy(title = input.title.trim(), target = input.target, color = input.color, icon = input.icon) else it
    },
  )

  /** Losing the pinned one hands the widget to the next countdown due. */
  fun remove(id: String, now: Long): CountdownsState {
    val rest = countdowns.filter { it.id != id }
    return copy(countdowns = rest, pinnedId = if (pinnedId == id) sortCountdowns(rest, now).firstOrNull()?.id else pinnedId)
  }

  fun pin(id: String): CountdownsState = if (countdowns.any { it.id == id }) copy(pinnedId = id) else this

  fun removeAll(): CountdownsState = copy(countdowns = emptyList(), pinnedId = null)

  /** First launch: add the user's example once. */
  fun seeded(id: String, now: Long): CountdownsState {
    if (seeded) return this
    val marked = copy(seeded = true)
    if (countdowns.isNotEmpty()) return marked
    return marked.add(CountdownInput("", SEED_TARGET, CountdownColor.EMBER, CountdownIcon.CALENDAR), pin = true, id = id, now = now)
  }
}

/** Same id shape as v1: base-36 time + 6 random base-36 characters. */
fun newCountdownId(now: Long = System.currentTimeMillis()): String {
  val random = (1..6).map { "0123456789abcdefghijklmnopqrstuvwxyz"[(Math.random() * 36).toInt()] }.joinToString("")
  return now.toString(36) + random
}
