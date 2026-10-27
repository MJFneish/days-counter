import XCTest
@testable import DaysCounterCore

/// The widget's data: the snapshot it draws (the same strings as the Android payload test) and the
/// day changes its timeline is built from.
final class WidgetTests: XCTestCase {
  override func setUp() {
    pinBeirut()
  }

  private var named: Countdown {
    Countdown(id: "named", title: "Graduation", target: dt(2027, 9, 5, 18, 30), color: .ember, icon: .calendar, createdAt: at(2026, 10, 23))
  }

  func testSnapshotOfThePinnedCountdown() {
    let state = CountdownsState(countdowns: [named], pinnedId: "named", seeded: true)
    let ar = WidgetSnapshot(settings: Settings(language: .ar, use24h: true), state: state)
    XCTAssertEqual(ar.title, "Graduation")
    XCTAssertEqual(ar.detail, "٥ أيلول ٢٠٢٧ · الساعة ١٨:٣٠")
    XCTAssertEqual(ar.countdown?.id, "named")
    XCTAssertTrue(ar.countUp)
    XCTAssertEqual(ar.strings.t("countdown.progress", ("percent", "{p}")), "مضى {p} من الانتظار")

    let unnamed = CountdownsState().seededIfNeeded(id: "seed", now: at(2026, 10, 23))
    let en = WidgetSnapshot(settings: Settings(language: .en, countUp: false, use24h: false), state: unnamed)
    XCTAssertEqual(en.title, "Sunday, 5 September 2027")
    XCTAssertEqual(en.detail, "")
    XCTAssertFalse(en.countUp)

    let empty = WidgetSnapshot(settings: Settings(language: .en, use24h: false), state: CountdownsState(seeded: true))
    XCTAssertNil(empty.countdown)
    XCTAssertEqual(empty.title, "")
  }

  func testTimelineEntriesAreTheDayChanges() {
    // A midnight target: every local midnight (the DST nights are covered by CountdownTests).
    let changes = WidgetTimeline.changes(userTarget, after: at(2026, 11, 1, 15), count: 4)
    XCTAssertEqual(changes, [at(2026, 11, 2), at(2026, 11, 3), at(2026, 11, 4), at(2026, 11, 5)])
    // Around the target: the arrival, then each day after it (counting up).
    let around = WidgetTimeline.changes(dt(2027, 1, 1, 18, 30), after: at(2026, 12, 31, 20), count: 3)
    XCTAssertEqual(around, [at(2027, 1, 1, 18, 30), at(2027, 1, 2, 18, 30), at(2027, 1, 3, 18, 30)])
  }

  func testSharedStoreRoundTrips() {
    let suite = "dayscounter.tests.\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: suite)!
    defer { defaults.removePersistentDomain(forName: suite) }
    let settings = Settings(language: .ar, themeMode: .dark, showSeconds: false, countUp: true, use24h: true)
    let state = CountdownsState(countdowns: [named], pinnedId: "named", seeded: true)
    SharedStore.save(settings, defaults)
    SharedStore.save(state, defaults)
    XCTAssertEqual(SharedStore.loadSettings(defaults), settings)
    XCTAssertEqual(SharedStore.loadCountdowns(defaults), state)
  }
}
