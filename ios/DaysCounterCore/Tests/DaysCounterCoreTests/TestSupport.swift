import Foundation
@testable import DaysCounterCore

/// Asia/Beirut, like v1's Jest setup: UTC+3 in summer, UTC+2 in winter, changing at midnight.
let beirut = TimeZone(identifier: "Asia/Beirut")!

/// Pins the engine's zone to Beirut for every test (idempotent).
func pinBeirut() {
  TimeZones.override = beirut
}

/// Epoch ms of a Beirut wall-clock time (an ordinary one: not skipped or repeated by DST), computed
/// with Foundation's Calendar — independent of the engine's own arithmetic.
func at(_ year: Int, _ month: Int, _ day: Int, _ hour: Int = 0, _ minute: Int = 0, _ second: Int = 0) -> Int64 {
  var calendar = Calendar(identifier: .gregorian)
  calendar.timeZone = beirut
  let date = calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour, minute: minute, second: second))!
  return Int64((date.timeIntervalSince1970 * 1000).rounded())
}

func dt(_ year: Int, _ month: Int, _ day: Int, _ hour: Int = 0, _ minute: Int = 0) -> LocalDateTime {
  LocalDateTime(year: year, month: month, day: day, hour: hour, minute: minute)
}

let userTarget = dt(2027, 9, 5)
