import Foundation

public enum CountdownStatus: Sendable {
  case upcoming, arrived, past
}

public struct Remaining: Equatable, Sendable {
  /// `.upcoming` until the target instant, `.arrived` for the first calendar day after it, then `.past`.
  public var status: CountdownStatus
  /// Whole calendar days to the target (upcoming) or since it (arrived/past). Days are anchored to
  /// the target's wall-clock time, so a midnight countdown flips its day count at local midnight —
  /// also across a DST change, where counting 24-hour blocks would flip it at 23:00 or 01:00.
  public var days: Int
  /// The rest, below one calendar day (on a DST day that rest can reach 23 or 24 hours).
  public var hours: Int
  public var minutes: Int
  public var seconds: Int
  /// |target − now| in ms.
  public var totalMs: Int64
  /// When `days` changes next — the widget counts down to this.
  public var nextChangeAt: Int64
  /// When `days` last changed (counting up) or will next change (counting down) — the widget counts
  /// up from this after the date.
  public var boundaryAt: Int64
}

private func split(_ ms: Int64) -> (Int, Int, Int) {
  let total = ms / SECOND_MS
  return (Int(total / 3600), Int((total % 3600) / 60), Int(total % 60))
}

/// Time left until (or elapsed since) `target` at `now` (epoch ms). Seconds are truncated, like
/// Android's Chronometer. Same algorithm as the Android app's `remaining()` (core/Remaining.kt).
public func remaining(_ target: LocalDateTime, now: Int64, zone: TimeZone = TimeZones.device) -> Remaining {
  let targetAt = toInstant(target, zone)

  if now < targetAt {
    // Largest d with (target − d calendar days) >= now. The 24h estimate is off by at most one
    // around DST changes; the loops correct it.
    var d = Int((targetAt - now) / DAY_MS)
    while shiftedInstant(target, -(d + 1), zone) >= now { d += 1 }
    while d > 0 && shiftedInstant(target, -d, zone) < now { d -= 1 }
    let boundary = shiftedInstant(target, -d, zone)
    let (h, m, s) = split(boundary - now)
    return Remaining(status: .upcoming, days: d, hours: h, minutes: m, seconds: s, totalMs: targetAt - now, nextChangeAt: boundary, boundaryAt: boundary)
  }

  // Largest d with (target + d calendar days) <= now.
  var d = Int((now - targetAt) / DAY_MS)
  while shiftedInstant(target, d + 1, zone) <= now { d += 1 }
  while d > 0 && shiftedInstant(target, d, zone) > now { d -= 1 }
  let boundary = shiftedInstant(target, d, zone)
  let (h, m, s) = split(now - boundary)
  return Remaining(
    status: d == 0 ? .arrived : .past,
    days: d, hours: h, minutes: m, seconds: s,
    totalMs: now - targetAt,
    nextChangeAt: shiftedInstant(target, d + 1, zone),
    boundaryAt: boundary
  )
}

/// Share of the way from `startAt` to the target, 0…1 (1 once the target has passed).
public func progress(_ target: LocalDateTime, startAt: Int64, now: Int64, zone: TimeZone = TimeZones.device) -> Double {
  let targetAt = toInstant(target, zone)
  if now >= targetAt { return 1 }
  if targetAt <= startAt { return 0 }
  return min(1, max(0, Double(now - startAt) / Double(targetAt - startAt)))
}

public struct Ymd: Equatable, Sendable {
  public var years: Int
  public var months: Int
  public var days: Int

  public init(years: Int, months: Int, days: Int) {
    self.years = years
    self.months = months
    self.days = days
  }
}

/// `a` plus whole months, clamped to the month's last day (31 Jan + 1 month = 28/29 Feb).
private func addMonths(_ a: DateOnly, _ months: Int) -> DateOnly {
  let total = Int64(a.year * 12 + (a.month - 1) + months)
  let year = Int(floorDiv(total, 12))
  let month = Int(floorMod(total, 12)) + 1
  return DateOnly(year: year, month: month, day: min(a.day, daysInMonth(year, month)))
}

private func dayNumber(_ d: DateOnly) -> Int64 { epochDay(d.year, d.month, d.day) }

/// Calendar span between two dates (a <= b), as a person would say it: 5 Oct → 5 Sep next year is
/// "11 months", 23 Oct → 5 Sep is "10 months and 13 days", 31 Jan → 1 Mar is "1 month and 1 day"
/// (the most whole months that fit, each landing on the same day-of-month or the month's last day,
/// then the leftover days).
public func calendarSpan(_ a: DateOnly, _ b: DateOnly) -> Ymd {
  var months = max(0, (b.year - a.year) * 12 + (b.month - a.month))
  while months > 0 && dayNumber(addMonths(a, months)) > dayNumber(b) { months -= 1 }
  let days = Int(max(0, dayNumber(b) - dayNumber(addMonths(a, months))))
  return Ymd(years: months / 12, months: months % 12, days: days)
}

/// The Y/M/D span covered by `r.days` — from the date the current day count started (or ends) to
/// the target's date.
public func spanOf(_ target: LocalDateTime, _ r: Remaining, zone: TimeZone = TimeZones.device) -> Ymd {
  let upcoming = r.status == .upcoming
  let from = dateOf(shiftedInstant(target, upcoming ? -r.days : 0, zone), zone)
  let to = dateOf(shiftedInstant(target, upcoming ? 0 : r.days, zone), zone)
  return calendarSpan(from, to)
}
