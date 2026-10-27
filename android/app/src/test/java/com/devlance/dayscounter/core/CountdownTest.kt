package com.devlance.dayscounter.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

/** Port of v1's countdown.test.ts (Jest), run in Asia/Beirut like v1 so the DST cases cross a real
 * change. The iOS app has the same cases (DaysCounterCoreTests/CountdownTests.swift). */
class CountdownTest {
  companion object {
    val BEIRUT: TimeZone = TimeZone.getTimeZone("Asia/Beirut")

    @BeforeClass
    @JvmStatic
    fun beirut() {
      TimeZone.setDefault(BEIRUT)
    }

    /** Epoch ms of a Beirut wall-clock time (an ordinary one: not skipped or repeated by DST). */
    fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Long =
      GregorianCalendar(BEIRUT).apply {
        clear()
        set(year, month - 1, day, hour, minute, second)
      }.timeInMillis

    fun dt(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) = LocalDateTime(year, month, day, hour, minute)

    val USER_TARGET = dt(2027, 9, 5)

    fun hourOf(ms: Long): Int = fromInstant(ms, BEIRUT).hour
  }

  private fun assertRemaining(r: Remaining, status: CountdownStatus, days: Int, hours: Int, minutes: Int, seconds: Int) {
    assertEquals(status, r.status)
    assertEquals("days", days, r.days)
    assertEquals("hours", hours, r.hours)
    assertEquals("minutes", minutes, r.minutes)
    assertEquals("seconds", seconds, r.seconds)
  }

  // ---------------------------------------------------------------- time zone

  @Test
  fun runsInBeirut() {
    // Lebanon: UTC+3 in summer, UTC+2 in winter.
    assertEquals(3 * HOUR_MS, BEIRUT.getOffset(at(2027, 7, 1)).toLong())
    assertEquals(2 * HOUR_MS, BEIRUT.getOffset(at(2027, 1, 1)).toLong())
  }

  // ---------------------------------------------------------------- the user's example

  @Test
  fun userExampleFrom23Oct15h() {
    val r = remaining(USER_TARGET, at(2026, 10, 23, 15))
    assertRemaining(r, CountdownStatus.UPCOMING, 316, 9, 0, 0)
    assertEquals(at(2026, 10, 24), r.nextChangeAt)
  }

  @Test
  fun userExampleFromMidnight() {
    assertRemaining(remaining(USER_TARGET, at(2026, 10, 23)), CountdownStatus.UPCOMING, 317, 0, 0, 0)
  }

  @Test
  fun dayCountDropsAtEveryLocalMidnight() {
    var previous = remaining(USER_TARGET, at(2026, 10, 23, 12)).days
    val notMidnight = mutableListOf<String>()
    for (k in 1..316) {
      val boundary = shiftedInstant(USER_TARGET, -(317 - k))
      assertEquals(previous, remaining(USER_TARGET, boundary - 1000).days)
      val after = remaining(USER_TARGET, boundary + 1000)
      assertEquals(previous - 1, after.days)
      previous = after.days
      val b = fromInstant(boundary)
      if (b.hour != 0) notMidnight += "${b.year}-${b.month}-${b.day} ${b.hour}:00"
    }
    assertEquals(0, previous)
    // The only midnight that doesn't exist: Lebanon springs forward 00:00 → 01:00 that night.
    assertEquals(listOf("2027-3-28 1:00"), notMidnight)
  }

  @Test
  fun hmsAddUpToTheTimeBeforeTheNextChange() {
    var now = at(2026, 10, 23, 15, 7, 31)
    val end = toInstant(USER_TARGET)
    while (now < end) {
      val r = remaining(USER_TARGET, now)
      val shown = (r.hours * 3600L + r.minutes * 60L + r.seconds) * 1000L
      assertEquals(((r.nextChangeAt - now) / 1000) * 1000, shown)
      assertTrue(r.minutes < 60)
      assertTrue(r.seconds < 60)
      assertTrue(r.hours <= 24)
      now += 7 * HOUR_MS + 13 * MINUTE_MS + 17_000
    }
  }

  // ---------------------------------------------------------------- edges

  private val edge = dt(2027, 1, 1, 18, 30)

  @Test
  fun oneSecondBefore() {
    assertRemaining(remaining(edge, toInstant(edge) - 1000), CountdownStatus.UPCOMING, 0, 0, 0, 1)
  }

  @Test
  fun truncatesSecondsLikeChronometer() {
    assertEquals(1, remaining(edge, toInstant(edge) - 1999).seconds)
    assertEquals(0, remaining(edge, toInstant(edge) - 999).seconds)
  }

  @Test
  fun atTheInstantItHasArrived() {
    val r = remaining(edge, toInstant(edge))
    assertRemaining(r, CountdownStatus.ARRIVED, 0, 0, 0, 0)
    assertEquals(0L, r.totalMs)
    assertEquals(at(2027, 1, 2, 18, 30), r.nextChangeAt)
    assertEquals(toInstant(edge), r.boundaryAt)
  }

  @Test
  fun countsUpAfterwards() {
    val r = remaining(edge, at(2027, 1, 4, 20, 30))
    assertRemaining(r, CountdownStatus.PAST, 3, 2, 0, 0)
    assertEquals(at(2027, 1, 4, 18, 30), r.boundaryAt)
    assertEquals(at(2027, 1, 5, 18, 30), r.nextChangeAt)
  }

  @Test
  fun nonMidnightTargetFlipsAtItsOwnTimeOfDay() {
    assertEquals(2, remaining(edge, at(2026, 12, 30, 18, 29, 59)).days)
    assertEquals(1, remaining(edge, at(2026, 12, 30, 18, 30, 1)).days)
  }

  @Test
  fun leapDay() {
    assertEquals(366, remaining(dt(2028, 2, 29), at(2027, 2, 28)).days)
  }

  // ---------------------------------------------------------------- DST in Lebanon

  @Test
  fun autumnChangeStillFlipsAtMidnight() {
    // 25 Oct 2026, 00:00 → 23:00. 24 Oct → 1 Dec is 38 calendar days; from noon, 37 full days +
    // the rest of the 25-hour day.
    val target = dt(2026, 12, 1)
    val r = remaining(target, at(2026, 10, 24, 12))
    assertRemaining(r, CountdownStatus.UPCOMING, 37, 13, 0, 0)
    assertEquals(0, hourOf(r.nextChangeAt))
  }

  @Test
  fun springChangeSkippedMidnightResolvesTo1am() {
    // 28 Mar 2027, 00:00 → 01:00.
    val target = dt(2027, 4, 1)
    val r = remaining(target, at(2027, 3, 27, 12))
    assertEquals(4, r.days)
    assertEquals(1, hourOf(r.nextChangeAt))
    assertEquals(12, r.hours)
    // A naive 24-hour count would say 4 days 11 h here.
    assertEquals(4L, (toInstant(target) - at(2027, 3, 27, 12)) / DAY_MS)
  }

  @Test
  fun skippedWallTimeMovesForwardByTheGap() {
    // 00:30 doesn't exist on 28 Mar 2027 in Beirut: it reads as 01:30 (UTC+3), like JS Date.
    assertEquals(utc(2027, 3, 27, 22, 30), toInstant(dt(2027, 3, 28, 0, 30)))
    assertEquals(dt(2027, 3, 28, 1, 30), fromInstant(toInstant(dt(2027, 3, 28, 0, 30))))
  }

  @Test
  fun repeatedWallTimeTakesTheFirstOccurrence() {
    // 23:30 on 24 Oct 2026 happens twice (clocks go back at 00:00 on the 25th): the first is
    // still summer time, UTC+3.
    assertEquals(utc(2026, 10, 24, 20, 30), toInstant(dt(2026, 10, 24, 23, 30)))
    // Just after the repeated hour, winter time.
    assertEquals(utc(2026, 10, 24, 22, 30), toInstant(dt(2026, 10, 25, 0, 30)))
  }

  private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    epochDay(year, month, day) * DAY_MS + hour * HOUR_MS + minute * MINUTE_MS

  // ---------------------------------------------------------------- day numbers

  @Test
  fun dayNumbersRoundTrip() {
    assertEquals(0L, epochDay(1970, 1, 1))
    assertEquals(DateOnly(1970, 1, 1), dateOfEpochDay(0))
    assertEquals(DateOnly(1969, 12, 31), dateOfEpochDay(-1))
    var day = epochDay(1899, 12, 1)
    var date = DateOnly(1899, 12, 1)
    while (date.year <= 2101) {
      assertEquals(date, dateOfEpochDay(day))
      assertEquals(day, epochDay(date.year, date.month, date.day))
      date = if (date.day < daysInMonth(date.year, date.month)) date.copy(day = date.day + 1)
      else if (date.month < 12) DateOnly(date.year, date.month + 1, 1) else DateOnly(date.year + 1, 1, 1)
      day++
    }
  }

  @Test
  fun weekdays() {
    assertEquals(0, weekdayOf(USER_TARGET)) // Sunday, 5 Sep 2027
    assertEquals(4, weekdayOf(1970, 1, 1)) // Thursday
    assertEquals(5, weekdayOf(2026, 10, 23)) // Friday
  }

  @Test
  fun validDates() {
    assertTrue(isValidDate(2028, 2, 29))
    assertTrue(!isValidDate(2027, 2, 29))
    assertTrue(!isValidDate(2100, 2, 29))
    assertTrue(isValidDate(2000, 2, 29))
    assertTrue(!isValidDate(2027, 13, 1))
    assertTrue(!isValidDate(2027, 4, 31))
  }

  // ---------------------------------------------------------------- progress

  @Test
  fun progressRunsFromCreationToTheTarget() {
    val start = at(2026, 10, 23)
    val end = toInstant(USER_TARGET)
    assertEquals(0.0, progress(USER_TARGET, start, start), 0.0)
    assertEquals(0.5, progress(USER_TARGET, start, (start + end) / 2), 0.0001)
    assertEquals(1.0, progress(USER_TARGET, start, end + 1), 0.0)
    assertEquals(0.0, progress(USER_TARGET, end + 5, end - 5), 0.0)
  }

  // ---------------------------------------------------------------- calendar span

  @Test
  fun calendarSpans() {
    assertEquals(Ymd(0, 10, 13), calendarSpan(DateOnly(2026, 10, 23), DateOnly(2027, 9, 5)))
    assertEquals(Ymd(0, 1, 1), calendarSpan(DateOnly(2027, 1, 31), DateOnly(2027, 3, 1)))
    assertEquals(Ymd(1, 10, 13), calendarSpan(DateOnly(2025, 10, 23), DateOnly(2027, 9, 5)))
    assertEquals(Ymd(0, 0, 0), calendarSpan(DateOnly(2027, 9, 5), DateOnly(2027, 9, 5)))
  }

  @Test
  fun spanOfMatchesTheDayCount() {
    val r = remaining(USER_TARGET, at(2026, 10, 23, 15))
    // 316 days counted from 24 Oct → 5 Sep.
    assertEquals(Ymd(0, 10, 12), spanOf(USER_TARGET, r))
  }
}
