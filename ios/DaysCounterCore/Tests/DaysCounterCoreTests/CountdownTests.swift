import XCTest
@testable import DaysCounterCore

/// Same cases as the Android app's CountdownTest.kt (a port of v1's countdown.test.ts), in Beirut.
final class CountdownTests: XCTestCase {
  override func setUp() {
    pinBeirut()
  }

  private func assertRemaining(_ r: Remaining, _ status: CountdownStatus, _ days: Int, _ hours: Int, _ minutes: Int, _ seconds: Int, file: StaticString = #filePath, line: UInt = #line) {
    XCTAssertEqual(r.status, status, file: file, line: line)
    XCTAssertEqual(r.days, days, "days", file: file, line: line)
    XCTAssertEqual(r.hours, hours, "hours", file: file, line: line)
    XCTAssertEqual(r.minutes, minutes, "minutes", file: file, line: line)
    XCTAssertEqual(r.seconds, seconds, "seconds", file: file, line: line)
  }

  private func utc(_ year: Int, _ month: Int, _ day: Int, _ hour: Int, _ minute: Int) -> Int64 {
    epochDay(year, month, day) * DAY_MS + Int64(hour) * HOUR_MS + Int64(minute) * MINUTE_MS
  }

  // MARK: time zone

  func testRunsInBeirut() {
    XCTAssertEqual(offsetMs(beirut, at(2027, 7, 1)), 3 * HOUR_MS)
    XCTAssertEqual(offsetMs(beirut, at(2027, 1, 1)), 2 * HOUR_MS)
  }

  // MARK: the user's example

  func testUserExampleFrom23Oct15h() {
    let r = remaining(userTarget, now: at(2026, 10, 23, 15))
    assertRemaining(r, .upcoming, 316, 9, 0, 0)
    XCTAssertEqual(r.nextChangeAt, at(2026, 10, 24))
  }

  func testUserExampleFromMidnight() {
    assertRemaining(remaining(userTarget, now: at(2026, 10, 23)), .upcoming, 317, 0, 0, 0)
  }

  func testDayCountDropsAtEveryLocalMidnight() {
    var previous = remaining(userTarget, now: at(2026, 10, 23, 12)).days
    var notMidnight: [String] = []
    for k in 1...316 {
      let boundary = shiftedInstant(userTarget, -(317 - k))
      XCTAssertEqual(remaining(userTarget, now: boundary - 1000).days, previous)
      let after = remaining(userTarget, now: boundary + 1000)
      XCTAssertEqual(after.days, previous - 1)
      previous = after.days
      let b = fromInstant(boundary)
      if b.hour != 0 { notMidnight.append("\(b.year)-\(b.month)-\(b.day) \(b.hour):00") }
    }
    XCTAssertEqual(previous, 0)
    // The only midnight that doesn't exist: Lebanon springs forward 00:00 → 01:00 that night.
    XCTAssertEqual(notMidnight, ["2027-3-28 1:00"])
  }

  func testHmsAddUpToTheTimeBeforeTheNextChange() {
    var now = at(2026, 10, 23, 15, 7, 31)
    let end = toInstant(userTarget)
    while now < end {
      let r = remaining(userTarget, now: now)
      let shown = (Int64(r.hours) * 3600 + Int64(r.minutes) * 60 + Int64(r.seconds)) * 1000
      XCTAssertEqual(shown, ((r.nextChangeAt - now) / 1000) * 1000)
      XCTAssertLessThan(r.minutes, 60)
      XCTAssertLessThan(r.seconds, 60)
      XCTAssertLessThanOrEqual(r.hours, 24)
      now += 7 * HOUR_MS + 13 * MINUTE_MS + 17_000
    }
  }

  // MARK: edges

  private let edge = dt(2027, 1, 1, 18, 30)

  func testOneSecondBefore() {
    assertRemaining(remaining(edge, now: toInstant(edge) - 1000), .upcoming, 0, 0, 0, 1)
  }

  func testTruncatesSecondsLikeChronometer() {
    XCTAssertEqual(remaining(edge, now: toInstant(edge) - 1999).seconds, 1)
    XCTAssertEqual(remaining(edge, now: toInstant(edge) - 999).seconds, 0)
  }

  func testAtTheInstantItHasArrived() {
    let r = remaining(edge, now: toInstant(edge))
    assertRemaining(r, .arrived, 0, 0, 0, 0)
    XCTAssertEqual(r.totalMs, 0)
    XCTAssertEqual(r.nextChangeAt, at(2027, 1, 2, 18, 30))
    XCTAssertEqual(r.boundaryAt, toInstant(edge))
  }

  func testCountsUpAfterwards() {
    let r = remaining(edge, now: at(2027, 1, 4, 20, 30))
    assertRemaining(r, .past, 3, 2, 0, 0)
    XCTAssertEqual(r.boundaryAt, at(2027, 1, 4, 18, 30))
    XCTAssertEqual(r.nextChangeAt, at(2027, 1, 5, 18, 30))
  }

  func testNonMidnightTargetFlipsAtItsOwnTimeOfDay() {
    XCTAssertEqual(remaining(edge, now: at(2026, 12, 30, 18, 29, 59)).days, 2)
    XCTAssertEqual(remaining(edge, now: at(2026, 12, 30, 18, 30, 1)).days, 1)
  }

  func testLeapDay() {
    XCTAssertEqual(remaining(dt(2028, 2, 29), now: at(2027, 2, 28)).days, 366)
  }

  // MARK: DST in Lebanon

  func testAutumnChangeStillFlipsAtMidnight() {
    let r = remaining(dt(2026, 12, 1), now: at(2026, 10, 24, 12))
    assertRemaining(r, .upcoming, 37, 13, 0, 0)
    XCTAssertEqual(fromInstant(r.nextChangeAt).hour, 0)
  }

  func testSpringChangeSkippedMidnightResolvesTo1am() {
    let target = dt(2027, 4, 1)
    let r = remaining(target, now: at(2027, 3, 27, 12))
    XCTAssertEqual(r.days, 4)
    XCTAssertEqual(fromInstant(r.nextChangeAt).hour, 1)
    XCTAssertEqual(r.hours, 12)
    // A naive 24-hour count would say 4 days 11 h here.
    XCTAssertEqual((toInstant(target) - at(2027, 3, 27, 12)) / DAY_MS, 4)
  }

  func testSkippedWallTimeMovesForwardByTheGap() {
    XCTAssertEqual(toInstant(dt(2027, 3, 28, 0, 30)), utc(2027, 3, 27, 22, 30))
    XCTAssertEqual(fromInstant(toInstant(dt(2027, 3, 28, 0, 30))), dt(2027, 3, 28, 1, 30))
  }

  func testRepeatedWallTimeTakesTheFirstOccurrence() {
    XCTAssertEqual(toInstant(dt(2026, 10, 24, 23, 30)), utc(2026, 10, 24, 20, 30))
    XCTAssertEqual(toInstant(dt(2026, 10, 25, 0, 30)), utc(2026, 10, 24, 22, 30))
  }

  // MARK: day numbers

  func testDayNumbersRoundTrip() {
    XCTAssertEqual(epochDay(1970, 1, 1), 0)
    XCTAssertEqual(dateOfEpochDay(0), DateOnly(year: 1970, month: 1, day: 1))
    XCTAssertEqual(dateOfEpochDay(-1), DateOnly(year: 1969, month: 12, day: 31))
    var day = epochDay(1899, 12, 1)
    var date = DateOnly(year: 1899, month: 12, day: 1)
    while date.year <= 2101 {
      XCTAssertEqual(dateOfEpochDay(day), date)
      XCTAssertEqual(epochDay(date.year, date.month, date.day), day)
      if date.day < daysInMonth(date.year, date.month) {
        date.day += 1
      } else if date.month < 12 {
        date = DateOnly(year: date.year, month: date.month + 1, day: 1)
      } else {
        date = DateOnly(year: date.year + 1, month: 1, day: 1)
      }
      day += 1
    }
  }

  func testWeekdays() {
    XCTAssertEqual(weekdayOf(userTarget), 0)
    XCTAssertEqual(weekdayOf(1970, 1, 1), 4)
    XCTAssertEqual(weekdayOf(2026, 10, 23), 5)
  }

  func testValidDates() {
    XCTAssertTrue(isValidDate(2028, 2, 29))
    XCTAssertFalse(isValidDate(2027, 2, 29))
    XCTAssertFalse(isValidDate(2100, 2, 29))
    XCTAssertTrue(isValidDate(2000, 2, 29))
    XCTAssertFalse(isValidDate(2027, 13, 1))
    XCTAssertFalse(isValidDate(2027, 4, 31))
  }

  // MARK: progress

  func testProgressRunsFromCreationToTheTarget() {
    let start = at(2026, 10, 23)
    let end = toInstant(userTarget)
    XCTAssertEqual(progress(userTarget, startAt: start, now: start), 0)
    XCTAssertEqual(progress(userTarget, startAt: start, now: (start + end) / 2), 0.5, accuracy: 0.0001)
    XCTAssertEqual(progress(userTarget, startAt: start, now: end + 1), 1)
    XCTAssertEqual(progress(userTarget, startAt: end + 5, now: end - 5), 0)
  }

  // MARK: calendar span

  func testCalendarSpans() {
    XCTAssertEqual(calendarSpan(DateOnly(year: 2026, month: 10, day: 23), DateOnly(year: 2027, month: 9, day: 5)), Ymd(years: 0, months: 10, days: 13))
    XCTAssertEqual(calendarSpan(DateOnly(year: 2027, month: 1, day: 31), DateOnly(year: 2027, month: 3, day: 1)), Ymd(years: 0, months: 1, days: 1))
    XCTAssertEqual(calendarSpan(DateOnly(year: 2025, month: 10, day: 23), DateOnly(year: 2027, month: 9, day: 5)), Ymd(years: 1, months: 10, days: 13))
    XCTAssertEqual(calendarSpan(DateOnly(year: 2027, month: 9, day: 5), DateOnly(year: 2027, month: 9, day: 5)), Ymd(years: 0, months: 0, days: 0))
  }

  func testSpanOfMatchesTheDayCount() {
    let r = remaining(userTarget, now: at(2026, 10, 23, 15))
    XCTAssertEqual(spanOf(userTarget, r), Ymd(years: 0, months: 10, days: 12))
  }
}
