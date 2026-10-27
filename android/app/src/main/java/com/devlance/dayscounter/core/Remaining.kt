package com.devlance.dayscounter.core

import java.util.TimeZone

enum class CountdownStatus { UPCOMING, ARRIVED, PAST }

data class Remaining(
  /** UPCOMING until the target instant, ARRIVED for the first calendar day after it, then PAST. */
  val status: CountdownStatus,
  /**
   * Whole calendar days to the target (upcoming) or since it (arrived/past). Days are anchored to
   * the target's wall-clock time, so a midnight countdown flips its day count at local midnight —
   * also across a DST change, where counting 24-hour blocks would flip it at 23:00 or 01:00.
   */
  val days: Int,
  /** The rest, below one calendar day (on a DST day that rest can reach 23 or 24 hours). */
  val hours: Int,
  val minutes: Int,
  val seconds: Int,
  /** |target − now| in ms. */
  val totalMs: Long,
  /** When `days` changes next — the widget's Chronometer counts down to this. */
  val nextChangeAt: Long,
  /** When `days` last changed (counting up) or will next change (counting down) — the widget's
   * Chronometer counts up from this after the date. */
  val boundaryAt: Long,
)

private data class Hms(val hours: Int, val minutes: Int, val seconds: Int)

private fun split(ms: Long): Hms {
  val total = ms / SECOND_MS
  return Hms((total / 3600).toInt(), ((total % 3600) / 60).toInt(), (total % 60).toInt())
}

/**
 * Time left until (or elapsed since) `target` at `now`. Seconds are truncated, like Android's
 * Chronometer, so the app and the widget tick over on the same second. Same algorithm as v1's
 * `remaining()` (src/core/countdown.ts) and the iOS app's `remaining(_:now:)`.
 */
fun remaining(target: LocalDateTime, now: Long, zone: TimeZone = TimeZone.getDefault()): Remaining {
  val targetAt = toInstant(target, zone)

  if (now < targetAt) {
    // Largest d with (target − d calendar days) >= now. The 24h estimate is off by at most one
    // around DST changes; the loops correct it.
    var d = ((targetAt - now) / DAY_MS).toInt()
    while (shiftedInstant(target, -(d + 1), zone) >= now) d++
    while (d > 0 && shiftedInstant(target, -d, zone) < now) d--
    val boundary = shiftedInstant(target, -d, zone)
    val (h, m, s) = split(boundary - now)
    return Remaining(CountdownStatus.UPCOMING, d, h, m, s, targetAt - now, boundary, boundary)
  }

  // Largest d with (target + d calendar days) <= now.
  var d = ((now - targetAt) / DAY_MS).toInt()
  while (shiftedInstant(target, d + 1, zone) <= now) d++
  while (d > 0 && shiftedInstant(target, d, zone) > now) d--
  val boundary = shiftedInstant(target, d, zone)
  val (h, m, s) = split(now - boundary)
  return Remaining(
    if (d == 0) CountdownStatus.ARRIVED else CountdownStatus.PAST,
    d, h, m, s,
    now - targetAt,
    shiftedInstant(target, d + 1, zone),
    boundary,
  )
}

/** Share of the way from `startAt` to the target, 0…1 (1 once the target has passed). */
fun progress(target: LocalDateTime, startAt: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): Double {
  val targetAt = toInstant(target, zone)
  if (now >= targetAt) return 1.0
  if (targetAt <= startAt) return 0.0
  return ((now - startAt).toDouble() / (targetAt - startAt)).coerceIn(0.0, 1.0)
}

data class Ymd(val years: Int, val months: Int, val days: Int)

/** `a` plus whole months, clamped to the month's last day (31 Jan + 1 month = 28/29 Feb). */
private fun addMonths(a: DateOnly, months: Int): DateOnly {
  val total = a.year * 12 + (a.month - 1) + months
  val year = Math.floorDiv(total, 12)
  val month = Math.floorMod(total, 12) + 1
  return DateOnly(year, month, minOf(a.day, daysInMonth(year, month)))
}

private fun dayNumber(d: DateOnly): Long = epochDay(d.year, d.month, d.day)

/**
 * Calendar span between two dates (a <= b), as a person would say it: 5 Oct → 5 Sep next year is
 * "11 months", 23 Oct → 5 Sep is "10 months and 13 days", 31 Jan → 1 Mar is "1 month and 1 day"
 * (the most whole months that fit, each landing on the same day-of-month or the month's last day,
 * then the leftover days).
 */
fun calendarSpan(a: DateOnly, b: DateOnly): Ymd {
  var months = maxOf(0, (b.year - a.year) * 12 + (b.month - a.month))
  while (months > 0 && dayNumber(addMonths(a, months)) > dayNumber(b)) months--
  val days = maxOf(0L, dayNumber(b) - dayNumber(addMonths(a, months))).toInt()
  return Ymd(months / 12, months % 12, days)
}

/** The Y/M/D span covered by `r.days` — from the date the current day count started (or ends) to
 * the target's date. */
fun spanOf(target: LocalDateTime, r: Remaining, zone: TimeZone = TimeZone.getDefault()): Ymd {
  val upcoming = r.status == CountdownStatus.UPCOMING
  val from = dateOf(shiftedInstant(target, if (upcoming) -r.days else 0, zone), zone)
  val to = dateOf(shiftedInstant(target, if (upcoming) 0 else r.days, zone), zone)
  return calendarSpan(from, to)
}
