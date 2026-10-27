import Foundation

public enum ThemeMode: String, CaseIterable, Sendable {
  case system, light, dark
}

public struct Settings: Equatable, Sendable {
  public var language: Lang
  public var themeMode: ThemeMode
  /// Tick the countdown cards every second (off = minutes only, ticking once a minute).
  public var showSeconds: Bool
  /// After a date passes, count the time since it (off = just "This date has passed").
  public var countUp: Bool
  public var use24h: Bool

  public init(language: Lang, themeMode: ThemeMode = .system, showSeconds: Bool = true, countUp: Bool = true, use24h: Bool) {
    self.language = language
    self.themeMode = themeMode
    self.showSeconds = showSeconds
    self.countUp = countUp
    self.use24h = use24h
  }
}

/// What the editor hands the store.
public struct CountdownInput: Equatable, Sendable {
  public var title: String
  public var target: LocalDateTime
  public var color: CountdownColor
  public var icon: CountdownIcon

  public init(title: String, target: LocalDateTime, color: CountdownColor, icon: CountdownIcon) {
    self.title = title
    self.target = target
    self.color = color
    self.icon = icon
  }
}

/// The editor's date wheels span these years.
public let YEAR_MIN = 1900
public let YEAR_MAX = 2100

/// The editor's working copy; nothing is saved until Save.
public struct CountdownDraft: Equatable, Sendable {
  public var title: String
  public var target: LocalDateTime
  public var color: CountdownColor
  public var icon: CountdownIcon
  public var pin: Bool

  public init(title: String, target: LocalDateTime, color: CountdownColor, icon: CountdownIcon, pin: Bool) {
    self.title = title
    self.target = target
    self.color = color
    self.icon = icon
    self.pin = pin
  }

  public var input: CountdownInput { CountdownInput(title: title, target: target, color: color, icon: icon) }

  /// A new countdown starts one month from today at midnight (the day clamped to that month's
  /// end), in the first color not used yet, pinned if nothing is — as on Android.
  public static func new(existing: [Countdown], hasPinned: Bool, now: Int64, zone: TimeZone = TimeZones.device) -> CountdownDraft {
    let today = fromInstant(now, zone)
    let year = today.month == 12 ? today.year + 1 : today.year
    let month = today.month == 12 ? 1 : today.month + 1
    let target = LocalDateTime(year: min(max(year, YEAR_MIN), YEAR_MAX), month: month, day: min(today.day, daysInMonth(year, month)))
    let used = Set(existing.map(\.color))
    let color = CountdownColor.allCases.first { !used.contains($0) } ?? CountdownColor.allCases[existing.count % CountdownColor.allCases.count]
    return CountdownDraft(title: "", target: target, color: color, icon: .calendar, pin: !hasPinned)
  }
}

/// The user's own example (2026-10-23): count down to 5 September 2027. Seeded on first launch.
public let SEED_TARGET = LocalDateTime(year: 2027, month: 9, day: 5)

/// Upcoming first (soonest on top), then passed ones (most recent first).
public func sortCountdowns(_ list: [Countdown], now: Int64, zone: TimeZone = TimeZones.device) -> [Countdown] {
  list.sorted { a, b in
    let ta = toInstant(a.target, zone)
    let tb = toInstant(b.target, zone)
    let pa = ta <= now
    let pb = tb <= now
    if pa != pb { return !pa }
    return pa ? tb < ta : ta < tb
  }
}

/// The countdown list, as v1's countdown store kept it (and the Android app's CountdownsState):
/// `pinnedId` is the countdown on the Home hero and the widget, always set while any countdown
/// exists; `seeded` = the 5 Sep 2027 example was added once, so deleting everything leaves an
/// empty list instead of reviving it. Every change returns a new state.
public struct CountdownsState: Equatable, Sendable {
  public var countdowns: [Countdown]
  public var pinnedId: String?
  public var seeded: Bool

  public init(countdowns: [Countdown] = [], pinnedId: String? = nil, seeded: Bool = false) {
    self.countdowns = countdowns
    self.pinnedId = pinnedId
    self.seeded = seeded
  }

  /// The pinned countdown, or nil when the list is empty.
  public var pinned: Countdown? { countdowns.first { $0.id == pinnedId } ?? countdowns.first }

  public func add(_ input: CountdownInput, pin: Bool, id: String, now: Int64) -> CountdownsState {
    let countdown = Countdown(id: id, title: input.title.trimmingCharacters(in: .whitespacesAndNewlines), target: input.target, color: input.color, icon: input.icon, createdAt: now)
    return CountdownsState(countdowns: countdowns + [countdown], pinnedId: pin || pinnedId == nil ? id : pinnedId, seeded: seeded)
  }

  public func update(_ id: String, _ input: CountdownInput) -> CountdownsState {
    var next = self
    next.countdowns = countdowns.map { c in
      guard c.id == id else { return c }
      var updated = c
      updated.title = input.title.trimmingCharacters(in: .whitespacesAndNewlines)
      updated.target = input.target
      updated.color = input.color
      updated.icon = input.icon
      return updated
    }
    return next
  }

  /// Losing the pinned one hands the widget to the next countdown due.
  public func remove(_ id: String, now: Int64) -> CountdownsState {
    let rest = countdowns.filter { $0.id != id }
    return CountdownsState(countdowns: rest, pinnedId: pinnedId == id ? sortCountdowns(rest, now: now).first?.id : pinnedId, seeded: seeded)
  }

  public func pin(_ id: String) -> CountdownsState {
    guard countdowns.contains(where: { $0.id == id }) else { return self }
    var next = self
    next.pinnedId = id
    return next
  }

  public func removeAll() -> CountdownsState { CountdownsState(countdowns: [], pinnedId: nil, seeded: seeded) }

  /// First launch: add the user's example once.
  public func seededIfNeeded(id: String, now: Int64) -> CountdownsState {
    if seeded { return self }
    var marked = self
    marked.seeded = true
    if !countdowns.isEmpty { return marked }
    return marked.add(CountdownInput(title: "", target: SEED_TARGET, color: .ember, icon: .calendar), pin: true, id: id, now: now)
  }
}

/// Same id shape as v1 and the Android app: base-36 time + 6 random base-36 characters.
public func newCountdownId(now: Int64 = nowMs()) -> String {
  let alphabet = Array("0123456789abcdefghijklmnopqrstuvwxyz")
  let random = String((0..<6).map { _ in alphabet[Int.random(in: 0..<36)] })
  return String(now, radix: 36) + random
}
