import Foundation

/// "10 months and 13 days" — the calendar span of the day count (empty when under a day).
public func spanPhrase(_ target: LocalDateTime, _ r: Remaining, _ lang: Lang, zone: TimeZone = TimeZones.device) -> String {
  let span = spanOf(target, r, zone: zone)
  var parts: [String] = []
  if span.years > 0 { parts.append(countPhrase(.year, span.years, lang)) }
  if span.months > 0 { parts.append(countPhrase(.month, span.months, lang)) }
  if span.days > 0 { parts.append(countPhrase(.day, span.days, lang)) }
  return joinParts(parts, lang)
}

/// "45 weeks and 1 day" (empty under a week).
public func weeksPhrase(_ r: Remaining, _ lang: Lang) -> String {
  let weeks = r.days / 7
  if weeks == 0 { return "" }
  let rest = r.days % 7
  var parts = [countPhrase(.week, weeks, lang)]
  if rest > 0 { parts.append(countPhrase(.day, rest, lang)) }
  return joinParts(parts, lang)
}

/// Whole hours in total, grouped: "7,593".
public func totalHours(_ r: Remaining, _ lang: Lang) -> String { formatNumber(r.totalMs / HOUR_MS, lang) }

/// The countdown's name, or its date when it has none ("Sunday, 5 September 2027").
public func countdownTitle(_ c: Countdown, _ lang: Lang) -> String {
  c.title.isEmpty ? formatLongDate(c.target, lang) : c.title
}

/// The line under the title: the date (unless the title already is the date) and, when the target
/// isn't midnight, its time.
public func countdownSubtitle(_ c: Countdown, _ strings: Strings, use24h: Bool, short: Bool = false) -> String {
  let lang = strings.lang
  var parts: [String] = []
  if !c.title.isEmpty { parts.append(short ? formatShortDate(c.target, lang) : formatLongDate(c.target, lang)) }
  if c.target.hour != 0 || c.target.minute != 0 {
    parts.append(strings.t("countdown.at", ("time", formatClock(c.target.hour, c.target.minute, lang, use24h))))
  }
  return parts.joined(separator: " · ")
}

public struct Headline: Equatable, Sendable {
  public var value: Int
  public var unit: CountUnit

  public init(value: Int, unit: CountUnit) {
    self.value = value
    self.unit = unit
  }
}

/// The biggest non-zero unit — "316 days", or "5 hours" on the last day.
public func headline(_ r: Remaining) -> Headline {
  if r.days > 0 { return Headline(value: r.days, unit: .day) }
  if r.hours > 0 { return Headline(value: r.hours, unit: .hour) }
  if r.minutes > 0 { return Headline(value: r.minutes, unit: .minute) }
  return Headline(value: r.seconds, unit: .second)
}

/// "in 316 days" / "بعد ٣١٦ يومًا", "3 days ago" / "منذ ٣ أيام", "Today" / "اليوم".
public func relativePhrase(_ r: Remaining, _ strings: Strings) -> String {
  if r.status == .arrived { return strings["countdown.today"] }
  let h = headline(r)
  let phrase = countPhrase(h.unit, h.value, strings.lang)
  return r.status == .upcoming ? strings.t("countdown.in", ("phrase", phrase)) : strings.t("countdown.ago", ("phrase", phrase))
}
