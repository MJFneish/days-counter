import XCTest
@testable import DaysCounterCore

/// Same cases as the Android app's PhrasesTest.kt, over the real locale files.
final class PhrasesTests: XCTestCase {
  override func setUp() {
    pinBeirut()
  }

  private let en = Strings.of(.en)
  private let ar = Strings.of(.ar)
  private var r: Remaining { remaining(userTarget, now: at(2026, 10, 23, 15)) }
  private var seed: Countdown { Countdown(id: "seed", title: "", target: userTarget, color: .ember, icon: .calendar, createdAt: at(2026, 10, 23)) }
  private var named: Countdown {
    var c = seed
    c.id = "named"
    c.title = "Graduation"
    c.target.hour = 18
    c.target.minute = 30
    return c
  }

  func testEnglish() {
    XCTAssertEqual(spanPhrase(userTarget, r, .en), "10 months and 12 days")
    XCTAssertEqual(weeksPhrase(r, .en), "45 weeks and 1 day")
    XCTAssertEqual(totalHours(r, .en), "7,593")
    XCTAssertEqual(countPhrase(.day, 1, .en), "1 day")
    XCTAssertEqual(unitWord(.second, 1, .en), "second")
    XCTAssertEqual(formatLongDate(userTarget, .en), "Sunday, 5 September 2027")
    XCTAssertEqual(formatShortDate(userTarget, .en), "5 Sep 2027")
    XCTAssertEqual(formatClock(18, 5, .en, false), "6:05 PM")
    XCTAssertEqual(formatClock(0, 0, .en, false), "12:00 AM")
    XCTAssertEqual(formatClock(0, 0, .en, true), "00:00")
    XCTAssertEqual(formatPercent(62, .en), "62%")
    XCTAssertEqual(joinParts(["2 years", "3 months", "5 days"], .en), "2 years, 3 months and 5 days")
    XCTAssertEqual(formatNumber(9780, .en), "9,780")
    XCTAssertEqual(formatNumber(1_234_567, .en), "1,234,567")
    XCTAssertEqual(formatNumber(-12, .en), "-12")
  }

  func testArabicPluralForms() {
    XCTAssertEqual(countPhrase(.day, 1, .ar), "يوم واحد")
    XCTAssertEqual(countPhrase(.day, 2, .ar), "يومان")
    XCTAssertEqual(countPhrase(.day, 5, .ar), "٥ أيام")
    XCTAssertEqual(countPhrase(.day, 12, .ar), "١٢ يومًا")
    XCTAssertEqual(countPhrase(.day, 316, .ar), "٣١٦ يومًا")
    XCTAssertEqual(countPhrase(.day, 300, .ar), "٣٠٠ يوم")
    XCTAssertEqual(countPhrase(.hour, 0, .ar), "٠ ساعة")
    XCTAssertEqual(unitWord(.day, 316, .ar), "يومًا")
    XCTAssertEqual(unitWord(.minute, 7, .ar), "دقائق")
    XCTAssertEqual(spanPhrase(userTarget, r, .ar), "١٠ أشهر و١٢ يومًا")
    XCTAssertEqual(formatLongDate(userTarget, .ar), "الأحد ٥ أيلول ٢٠٢٧")
    XCTAssertEqual(formatShortDate(userTarget, .ar), "٥ أيلول ٢٠٢٧")
    XCTAssertEqual(formatClock(18, 5, .ar, false), "٦:٠٥ م")
    XCTAssertEqual(formatPercent(62, .ar), "٦٢٪")
    XCTAssertEqual(formatNumber(9780, .ar), "٩٬٧٨٠")
    XCTAssertEqual(joinParts(["سنتان", "٣ أشهر", "٥ أيام"], .ar), "سنتان و٣ أشهر و٥ أيام")
  }

  func testUnnamedCountdownIsTitledByItsDate() {
    XCTAssertEqual(countdownTitle(seed, .en), "Sunday, 5 September 2027")
    XCTAssertEqual(countdownSubtitle(seed, en, use24h: true), "")
  }

  func testNamedCountdownShowsDateAndTimeUnderneath() {
    XCTAssertEqual(countdownTitle(named, .en), "Graduation")
    XCTAssertEqual(countdownSubtitle(named, en, use24h: true), "Sunday, 5 September 2027 · at 18:30")
    XCTAssertEqual(countdownSubtitle(named, en, use24h: false, short: true), "5 Sep 2027 · at 6:30 PM")
    XCTAssertEqual(countdownSubtitle(named, ar, use24h: false, short: true), "٥ أيلول ٢٠٢٧ · الساعة ٦:٣٠ م")
  }

  func testRelativePhrases() {
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2026, 10, 23, 15)), en), "in 316 days")
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2027, 9, 4, 19)), en), "in 5 hours")
    XCTAssertEqual(headline(remaining(seed.target, now: at(2027, 9, 4, 23, 59))), Headline(value: 1, unit: .minute))
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2027, 9, 5, 9)), en), "Today")
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2027, 9, 8, 9)), en), "3 days ago")
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2026, 10, 23, 15)), ar), "بعد ٣١٦ يومًا")
    XCTAssertEqual(relativePhrase(remaining(seed.target, now: at(2027, 9, 8, 9)), ar), "منذ ٣ أيام")
  }

  func testVersionsStayLeftToRightInArabic() {
    XCTAssertEqual(ltr(localizeDigits("2.0.0-alpha.5", .ar)), "\u{2066}٢.٠.٠-alpha.٥\u{2069}")
  }

  func testStringsInterpolateAndFallBack() {
    XCTAssertEqual(en.t("home.edit", ("name", "Graduation")), "Edit Graduation")
    XCTAssertEqual(en["app.name"], "Days Counter")
    XCTAssertEqual(ar["app.name"], "عدّاد الأيام")
    XCTAssertEqual(ar["no.such.key"], "no.such.key")
  }
}
