package com.devlance.dayscounter.widget

import java.util.Calendar

/** Wall-clock target in the device's time zone (month 1-12) — same shape as `LocalDateTime` in
 * src/core/types.ts. */
data class LocalDateTime(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)

enum class Status { UPCOMING, ARRIVED, PAST }

/** Port of `remaining()` in src/core/countdown.ts — keep the two in sync. */
data class Remaining(
  val status: Status,
  /** Calendar days anchored to the target's time of day. */
  val days: Int,
  /** The instant the day count last changed (count-up) or will next reach (countdown). */
  val boundaryAt: Long,
  /** When `days` changes next. */
  val nextChangeAt: Long,
)

object Countdown {
  private const val DAY_MS = 86_400_000L

  /** The target's wall-clock time shifted by `days` calendar days. Lenient Calendar normalizes
   * day overflow, and resolves a time skipped by a DST jump to the instant after it, like JS Date. */
  fun shiftedInstant(t: LocalDateTime, days: Int): Long {
    val c = Calendar.getInstance()
    c.isLenient = true
    c.clear()
    c.set(t.year, t.month - 1, t.day + days, t.hour, t.minute, 0)
    return c.timeInMillis
  }

  fun remaining(t: LocalDateTime, now: Long): Remaining {
    val targetAt = shiftedInstant(t, 0)
    if (now < targetAt) {
      var d = ((targetAt - now) / DAY_MS).toInt()
      while (shiftedInstant(t, -(d + 1)) >= now) d++
      while (d > 0 && shiftedInstant(t, -d) < now) d--
      val boundary = shiftedInstant(t, -d)
      return Remaining(Status.UPCOMING, d, boundary, boundary)
    }
    var d = ((now - targetAt) / DAY_MS).toInt()
    while (shiftedInstant(t, d + 1) <= now) d++
    while (d > 0 && shiftedInstant(t, d) > now) d--
    return Remaining(if (d == 0) Status.ARRIVED else Status.PAST, d, shiftedInstant(t, d), shiftedInstant(t, d + 1))
  }

  /** 0…1 from `startAt` to the target. */
  fun progress(t: LocalDateTime, startAt: Long, now: Long): Double {
    val targetAt = shiftedInstant(t, 0)
    if (now >= targetAt) return 1.0
    if (targetAt <= startAt) return 0.0
    return ((now - startAt).toDouble() / (targetAt - startAt)).coerceIn(0.0, 1.0)
  }
}
