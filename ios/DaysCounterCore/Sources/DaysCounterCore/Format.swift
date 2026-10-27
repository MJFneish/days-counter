import Foundation

private let arabicIndic: [Character] = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"]

/// Arabic UI shows Arabic-Indic digits (٢٠٢٧), like the sibling apps.
public func localizeDigits(_ value: Any, _ lang: Lang) -> String {
  let s = "\(value)"
  guard lang == .ar else { return s }
  return String(s.map { c -> Character in
    if let digit = c.wholeNumberValue, c.isASCII { return arabicIndic[digit] }
    return c
  })
}

/// Levantine month names (كانون الثاني…), as Lebanese calendars print them — same as the Hijri app.
private let monthsAr = [
  "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
  "تمّوز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
]
private let monthsEn = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
]
private let monthsEnShort = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
private let weekdaysAr = ["الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"]
private let weekdaysEn = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]

public func monthName(_ month: Int, _ lang: Lang) -> String { (lang == .ar ? monthsAr : monthsEn)[month - 1] }

public func weekdayName(_ weekday: Int, _ lang: Lang) -> String { (lang == .ar ? weekdaysAr : weekdaysEn)[weekday] }

/// "Sunday, 5 September 2027" / "الأحد ٥ أيلول ٢٠٢٧".
public func formatLongDate(_ year: Int, _ month: Int, _ day: Int, _ lang: Lang) -> String {
  let weekday = weekdayName(weekdayOf(year, month, day), lang)
  if lang == .ar {
    return localizeDigits("\(weekday) \(day) \(monthName(month, lang)) \(year)", lang)
  }
  return "\(weekday), \(day) \(monthName(month, lang)) \(year)"
}

public func formatLongDate(_ t: LocalDateTime, _ lang: Lang) -> String { formatLongDate(t.year, t.month, t.day, lang) }

/// "5 Sep 2027" / "٥ أيلول ٢٠٢٧" — Arabic has no customary short month names.
public func formatShortDate(_ t: LocalDateTime, _ lang: Lang) -> String {
  if lang == .ar { return localizeDigits("\(t.day) \(monthName(t.month, lang)) \(t.year)", lang) }
  return "\(t.day) \(monthsEnShort[t.month - 1]) \(t.year)"
}

public func pad2(_ n: Int) -> String { n < 10 && n >= 0 ? "0\(n)" : "\(n)" }

/// The AM/PM marker: "AM"/"PM", "ص"/"م".
public func dayPeriod(_ hour: Int, _ lang: Lang) -> String {
  lang == .ar ? (hour < 12 ? "ص" : "م") : (hour < 12 ? "AM" : "PM")
}

/// 12-hour clock hour: 0 → 12, 13 → 1.
public func hour12(_ hour: Int) -> Int { hour % 12 == 0 ? 12 : hour % 12 }

/// "00:00" (24h) or "12:00 AM" / "١٢:٠٠ ص" (12h).
public func formatClock(_ hour: Int, _ minute: Int, _ lang: Lang, _ use24h: Bool) -> String {
  if use24h { return localizeDigits("\(pad2(hour)):\(pad2(minute))", lang) }
  return localizeDigits("\(hour12(hour)):\(pad2(minute)) \(dayPeriod(hour, lang))", lang)
}

/// Keeps a left-to-right run (a version like "٢.٠.٠-alpha.٥") in order inside Arabic text: without
/// the isolate, Arabic-Indic digits next to Latin letters get reordered ("٥.alpha-٢.٠.٠").
public func ltr(_ text: String) -> String { "\u{2066}\(text)\u{2069}" }

/// "62%" / "٦٢٪".
public func formatPercent(_ percent: Int, _ lang: Lang) -> String {
  lang == .ar ? "\(localizeDigits(percent, lang))٪" : "\(percent)%"
}
