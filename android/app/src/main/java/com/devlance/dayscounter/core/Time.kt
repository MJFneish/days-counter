package com.devlance.dayscounter.core

import java.util.TimeZone

const val SECOND_MS = 1000L
const val MINUTE_MS = 60 * SECOND_MS
const val HOUR_MS = 60 * MINUTE_MS
const val DAY_MS = 24 * HOUR_MS

/*
 * Calendar arithmetic without java.util.Calendar or java.time: plain day numbers plus the zone's
 * UTC offset. The iOS app (DaysCounterCore/Time.swift) runs the same algorithm, so both resolve
 * every wall-clock time — including the ones a DST change skips or repeats — to the same instant.
 */

/** Days since 1970-01-01 in the proleptic Gregorian calendar (H. Hinnant's days_from_civil). */
fun epochDay(year: Int, month: Int, day: Int): Long {
  val y = (if (month <= 2) year - 1 else year).toLong()
  val era = Math.floorDiv(y, 400L)
  val yoe = y - era * 400
  val mp = (month + 9) % 12 // March = 0
  val doy = (153 * mp + 2) / 5 + day - 1
  val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
  return era * 146097 + doe - 719468
}

/** The calendar date of a day number (civil_from_days). */
fun dateOfEpochDay(epochDay: Long): DateOnly {
  val z = epochDay + 719468
  val era = Math.floorDiv(z, 146097L)
  val doe = z - era * 146097
  val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
  val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
  val mp = (5 * doy + 2) / 153
  val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
  val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
  val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
  return DateOnly(year, month, day)
}

fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

fun daysInMonth(year: Int, month: Int): Int = when (month) {
  2 -> if (isLeapYear(year)) 29 else 28
  4, 6, 9, 11 -> 30
  else -> 31
}

fun isValidDate(year: Int, month: Int, day: Int): Boolean =
  month in 1..12 && day >= 1 && day <= daysInMonth(year, month)

/** Weekday of a date, 0 = Sunday (1970-01-01 was a Thursday). */
fun weekdayOf(year: Int, month: Int, day: Int): Int = Math.floorMod(epochDay(year, month, day) + 4, 7L).toInt()

fun weekdayOf(t: LocalDateTime): Int = weekdayOf(t.year, t.month, t.day)

/**
 * The instant of a wall-clock time in `zone`. A time that a DST change skips (00:30 when clocks jump
 * 00:00 → 01:00) moves forward by the gap (01:30); one that it repeats takes its first occurrence —
 * JavaScript Date's rules, which v1 used, and java.time's.
 */
fun wallToInstant(epochDay: Long, hour: Int, minute: Int, zone: TimeZone): Long {
  val local = epochDay * DAY_MS + hour * HOUR_MS + minute * MINUTE_MS
  // The offsets on either side of any DST change near this time (changes are months apart).
  val before = zone.getOffset(local - DAY_MS).toLong()
  val after = zone.getOffset(local + DAY_MS).toLong()
  val early = local - maxOf(before, after)
  val late = local - minOf(before, after)
  val earlyValid = zone.getOffset(early).toLong() == local - early
  val lateValid = zone.getOffset(late).toLong() == local - late
  return when {
    earlyValid -> early
    lateValid -> late
    else -> local - before // skipped: read with the offset from before the jump, which lands after it
  }
}

/** Epoch ms of the target's wall-clock time shifted by `days` calendar days (negative = earlier).
 * Day overflow normalizes (31 Mar − 31 days = 28/29 Feb's neighbour), as with JS Date. */
fun shiftedInstant(t: LocalDateTime, days: Int, zone: TimeZone = TimeZone.getDefault()): Long =
  wallToInstant(epochDay(t.year, t.month, t.day) + days, t.hour, t.minute, zone)

fun toInstant(t: LocalDateTime, zone: TimeZone = TimeZone.getDefault()): Long = shiftedInstant(t, 0, zone)

/** The wall-clock date-time of an instant in `zone` (seconds dropped). */
fun fromInstant(ms: Long, zone: TimeZone = TimeZone.getDefault()): LocalDateTime {
  val local = ms + zone.getOffset(ms)
  val date = dateOfEpochDay(Math.floorDiv(local, DAY_MS))
  val inDay = Math.floorMod(local, DAY_MS)
  return LocalDateTime(date.year, date.month, date.day, (inDay / HOUR_MS).toInt(), ((inDay % HOUR_MS) / MINUTE_MS).toInt())
}

fun dateOf(ms: Long, zone: TimeZone = TimeZone.getDefault()): DateOnly =
  dateOfEpochDay(Math.floorDiv(ms + zone.getOffset(ms), DAY_MS))
