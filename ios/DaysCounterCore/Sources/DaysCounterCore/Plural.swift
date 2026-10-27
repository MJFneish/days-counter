import Foundation

/// Counted phrases ("3 days", "٣ أيام"). Arabic needs all six CLDR forms (zero/one/two/few/many/
/// other) to read naturally, so they're spelled out here. Same tables as the Android app
/// (core/Plural.kt) and v1's plural.ts.
public enum CountUnit: CaseIterable, Sendable {
  case year, month, week, day, hour, minute, second
}

private struct ArabicForms {
  let one: String
  let two: String
  let few: String
  let many: String
  let other: String

  func pick(_ form: ArabicForm) -> String {
    switch form {
    case .one: return one
    case .two: return two
    case .few: return few
    case .many: return many
    case .other: return other
    }
  }
}

/// Full phrases: "سنة واحدة", "سنتان", "٣ سنوات", "١١ سنة"…
private let arPhrases: [CountUnit: ArabicForms] = [
  .year: ArabicForms(one: "سنة واحدة", two: "سنتان", few: "{n} سنوات", many: "{n} سنة", other: "{n} سنة"),
  .month: ArabicForms(one: "شهر واحد", two: "شهران", few: "{n} أشهر", many: "{n} شهرًا", other: "{n} شهر"),
  .week: ArabicForms(one: "أسبوع واحد", two: "أسبوعان", few: "{n} أسابيع", many: "{n} أسبوعًا", other: "{n} أسبوع"),
  .day: ArabicForms(one: "يوم واحد", two: "يومان", few: "{n} أيام", many: "{n} يومًا", other: "{n} يوم"),
  .hour: ArabicForms(one: "ساعة واحدة", two: "ساعتان", few: "{n} ساعات", many: "{n} ساعة", other: "{n} ساعة"),
  .minute: ArabicForms(one: "دقيقة واحدة", two: "دقيقتان", few: "{n} دقائق", many: "{n} دقيقة", other: "{n} دقيقة"),
  .second: ArabicForms(one: "ثانية واحدة", two: "ثانيتان", few: "{n} ثوانٍ", many: "{n} ثانية", other: "{n} ثانية"),
]

/// The noun alone, for a label under a big number: "١٢" over "يومًا".
private let arWords: [CountUnit: ArabicForms] = [
  .year: ArabicForms(one: "سنة", two: "سنتان", few: "سنوات", many: "سنة", other: "سنة"),
  .month: ArabicForms(one: "شهر", two: "شهران", few: "أشهر", many: "شهرًا", other: "شهر"),
  .week: ArabicForms(one: "أسبوع", two: "أسبوعان", few: "أسابيع", many: "أسبوعًا", other: "أسبوع"),
  .day: ArabicForms(one: "يوم", two: "يومان", few: "أيام", many: "يومًا", other: "يوم"),
  .hour: ArabicForms(one: "ساعة", two: "ساعتان", few: "ساعات", many: "ساعة", other: "ساعة"),
  .minute: ArabicForms(one: "دقيقة", two: "دقيقتان", few: "دقائق", many: "دقيقة", other: "دقيقة"),
  .second: ArabicForms(one: "ثانية", two: "ثانيتان", few: "ثوانٍ", many: "ثانية", other: "ثانية"),
]

private let enWords: [CountUnit: (String, String)] = [
  .year: ("year", "years"),
  .month: ("month", "months"),
  .week: ("week", "weeks"),
  .day: ("day", "days"),
  .hour: ("hour", "hours"),
  .minute: ("minute", "minutes"),
  .second: ("second", "seconds"),
]

/// "9,780" / "٩٬٧٨٠".
public func formatNumber(_ n: Int64, _ lang: Lang) -> String {
  let digits = Array(String(n.magnitude))
  var grouped = ""
  for (i, c) in digits.enumerated() {
    if i > 0 && (digits.count - i) % 3 == 0 { grouped.append(lang == .ar ? "٬" : ",") }
    grouped.append(c)
  }
  return localizeDigits((n < 0 ? "-" : "") + grouped, lang)
}

public func formatNumber(_ n: Int, _ lang: Lang) -> String { formatNumber(Int64(n), lang) }

public enum ArabicForm: Sendable {
  case one, two, few, many, other
}

public func arabicForm(_ n: Int64) -> ArabicForm {
  let mod100 = n % 100
  if n == 1 { return .one }
  if n == 2 { return .two }
  if (3...10).contains(mod100) { return .few }
  if (11...99).contains(mod100) { return .many }
  return .other
}

public func countPhrase(_ unit: CountUnit, _ n: Int64, _ lang: Lang) -> String {
  if lang == .ar {
    if n == 0 { return "\(localizeDigits(0, .ar)) \(arWords[unit]!.other)" }
    return arPhrases[unit]!.pick(arabicForm(n)).replacingOccurrences(of: "{n}", with: formatNumber(n, .ar))
  }
  let (one, many) = enWords[unit]!
  return "\(formatNumber(n, .en)) \(n == 1 ? one : many)"
}

public func countPhrase(_ unit: CountUnit, _ n: Int, _ lang: Lang) -> String { countPhrase(unit, Int64(n), lang) }

/// Unit word that agrees with `n`: "day"/"days", "يوم"/"يومان"/"أيام"/"يومًا".
public func unitWord(_ unit: CountUnit, _ n: Int64, _ lang: Lang) -> String {
  if lang == .ar { return n == 0 ? arWords[unit]!.other : arWords[unit]!.pick(arabicForm(n)) }
  let (one, many) = enWords[unit]!
  return n == 1 ? one : many
}

public func unitWord(_ unit: CountUnit, _ n: Int, _ lang: Lang) -> String { unitWord(unit, Int64(n), lang) }

/// Joins non-zero parts: "2 years, 3 months and 5 days" / "سنتان و٣ أشهر و٥ أيام".
public func joinParts(_ parts: [String], _ lang: Lang) -> String {
  if parts.count <= 1 { return parts.joined() }
  if lang == .ar { return parts.joined(separator: " و") }
  return "\(parts.dropLast().joined(separator: ", ")) and \(parts.last!)"
}
