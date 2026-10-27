import Foundation

/// The app's language — its own setting, independent of the phone's.
public enum Lang: String, CaseIterable, Codable, Sendable {
  case en
  case ar

  public static func of(_ code: String?) -> Lang { code == "ar" ? .ar : .en }

  public var code: String { rawValue }
  public var isRTL: Bool { self == .ar }
}

/// A wall-clock date-time in the device's time zone (month 1-12). Countdowns store this rather than
/// an epoch instant, so "5 Sep 2027 at 00:00" stays midnight local time even if the phone's time
/// zone or DST rules change between now and then.
public struct LocalDateTime: Equatable, Hashable, Codable, Sendable {
  public var year: Int
  public var month: Int
  public var day: Int
  public var hour: Int
  public var minute: Int

  public init(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) {
    self.year = year
    self.month = month
    self.day = day
    self.hour = hour
    self.minute = minute
  }
}

/// A calendar date (month 1-12).
public struct DateOnly: Equatable, Hashable, Sendable {
  public var year: Int
  public var month: Int
  public var day: Int

  public init(year: Int, month: Int, day: Int) {
    self.year = year
    self.month = month
    self.day = day
  }
}

public enum CountdownColor: String, CaseIterable, Codable, Sendable {
  case ember, violet, ocean, teal, rose, forest, indigo, gold, graphite

  public var id: String { rawValue }
}

public enum CountdownIcon: String, CaseIterable, Codable, Sendable {
  case calendar, star, heart, cake, plane, graduation, briefcase, home, baby, trophy, gift, flag

  public var id: String { rawValue }
}

public struct Countdown: Equatable, Identifiable, Sendable {
  public var id: String
  /// May be empty — the UI then shows the formatted target date as the title.
  public var title: String
  public var target: LocalDateTime
  public var color: CountdownColor
  public var icon: CountdownIcon
  /// Epoch ms the countdown was created — the start of its progress bar.
  public var createdAt: Int64

  public init(id: String, title: String, target: LocalDateTime, color: CountdownColor, icon: CountdownIcon, createdAt: Int64) {
    self.id = id
    self.title = title
    self.target = target
    self.color = color
    self.icon = icon
    self.createdAt = createdAt
  }
}
