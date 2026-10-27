import Foundation

public let SECOND_MS: Int64 = 1000
public let MINUTE_MS: Int64 = 60 * SECOND_MS
public let HOUR_MS: Int64 = 60 * MINUTE_MS
public let DAY_MS: Int64 = 24 * HOUR_MS

/// The time zone countdowns are counted in: the phone's, following it when it changes. Unit tests
/// pin it to Asia/Beirut through `override`.
public enum TimeZones {
  nonisolated(unsafe) public static var override: TimeZone?
  public static var device: TimeZone { override ?? TimeZone.autoupdatingCurrent }
}

/// Epoch ms now.
public func nowMs() -> Int64 { Int64((Date().timeIntervalSince1970 * 1000).rounded(.down)) }

public func floorDiv(_ a: Int64, _ b: Int64) -> Int64 {
  let q = a / b
  return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q
}

public func floorMod(_ a: Int64, _ b: Int64) -> Int64 { a - floorDiv(a, b) * b }

/*
 * Calendar arithmetic without Foundation's Calendar: plain day numbers plus the zone's UTC offset.
 * The Android app (core/Time.kt) runs the same algorithm, so both resolve every wall-clock time —
 * including the ones a DST change skips or repeats — to the same instant.
 */

/// Days since 1970-01-01 in the proleptic Gregorian calendar (H. Hinnant's days_from_civil).
public func epochDay(_ year: Int, _ month: Int, _ day: Int) -> Int64 {
  let y = Int64(month <= 2 ? year - 1 : year)
  let era = floorDiv(y, 400)
  let yoe = y - era * 400
  let mp = Int64((month + 9) % 12) // March = 0
  let doy = (153 * mp + 2) / 5 + Int64(day) - 1
  let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
  return era * 146097 + doe - 719468
}

/// The calendar date of a day number (civil_from_days).
public func dateOfEpochDay(_ epochDay: Int64) -> DateOnly {
  let z = epochDay + 719468
  let era = floorDiv(z, 146097)
  let doe = z - era * 146097
  let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
  let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
  let mp = (5 * doy + 2) / 153
  let day = Int(doy - (153 * mp + 2) / 5 + 1)
  let month = Int(mp < 10 ? mp + 3 : mp - 9)
  let year = Int(yoe + era * 400) + (month <= 2 ? 1 : 0)
  return DateOnly(year: year, month: month, day: day)
}

public func isLeapYear(_ year: Int) -> Bool { (year % 4 == 0 && year % 100 != 0) || year % 400 == 0 }

public func daysInMonth(_ year: Int, _ month: Int) -> Int {
  switch month {
  case 2: return isLeapYear(year) ? 29 : 28
  case 4, 6, 9, 11: return 30
  default: return 31
  }
}

public func isValidDate(_ year: Int, _ month: Int, _ day: Int) -> Bool {
  (1...12).contains(month) && day >= 1 && day <= daysInMonth(year, month)
}

/// Weekday of a date, 0 = Sunday (1970-01-01 was a Thursday).
public func weekdayOf(_ year: Int, _ month: Int, _ day: Int) -> Int { Int(floorMod(epochDay(year, month, day) + 4, 7)) }

public func weekdayOf(_ t: LocalDateTime) -> Int { weekdayOf(t.year, t.month, t.day) }

/// UTC offset of `zone` at an instant, in ms.
func offsetMs(_ zone: TimeZone, _ ms: Int64) -> Int64 {
  Int64(zone.secondsFromGMT(for: Date(timeIntervalSince1970: TimeInterval(ms) / 1000))) * 1000
}

/// The instant of a wall-clock time in `zone`. A time that a DST change skips (00:30 when clocks
/// jump 00:00 → 01:00) moves forward by the gap (01:30); one that it repeats takes its first
/// occurrence — JavaScript Date's rules, which v1 used, and java.time's.
public func wallToInstant(_ epochDay: Int64, _ hour: Int, _ minute: Int, _ zone: TimeZone) -> Int64 {
  let local = epochDay * DAY_MS + Int64(hour) * HOUR_MS + Int64(minute) * MINUTE_MS
  // The offsets on either side of any DST change near this time (changes are months apart).
  let before = offsetMs(zone, local - DAY_MS)
  let after = offsetMs(zone, local + DAY_MS)
  let early = local - max(before, after)
  let late = local - min(before, after)
  if offsetMs(zone, early) == local - early { return early }
  if offsetMs(zone, late) == local - late { return late }
  return local - before // skipped: read with the offset from before the jump, which lands after it
}

/// Epoch ms of the target's wall-clock time shifted by `days` calendar days (negative = earlier).
/// Day overflow normalizes (31 Mar − 31 days = 28/29 Feb's neighbour), as with JS Date.
public func shiftedInstant(_ t: LocalDateTime, _ days: Int, _ zone: TimeZone = TimeZones.device) -> Int64 {
  wallToInstant(epochDay(t.year, t.month, t.day) + Int64(days), t.hour, t.minute, zone)
}

public func toInstant(_ t: LocalDateTime, _ zone: TimeZone = TimeZones.device) -> Int64 { shiftedInstant(t, 0, zone) }

/// The wall-clock date-time of an instant in `zone` (seconds dropped).
public func fromInstant(_ ms: Int64, _ zone: TimeZone = TimeZones.device) -> LocalDateTime {
  let local = ms + offsetMs(zone, ms)
  let date = dateOfEpochDay(floorDiv(local, DAY_MS))
  let inDay = floorMod(local, DAY_MS)
  return LocalDateTime(year: date.year, month: date.month, day: date.day, hour: Int(inDay / HOUR_MS), minute: Int((inDay % HOUR_MS) / MINUTE_MS))
}

public func dateOf(_ ms: Int64, _ zone: TimeZone = TimeZones.device) -> DateOnly {
  dateOfEpochDay(floorDiv(ms + offsetMs(zone, ms), DAY_MS))
}
