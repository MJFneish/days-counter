import XCTest
@testable import DaysCounterCore

/// Same cases as the Android app's StoreTest.kt: the countdown list's rules and the persisted JSON.
final class StoreTests: XCTestCase {
  override func setUp() {
    pinBeirut()
  }

  private var now: Int64 { at(2026, 10, 27, 12) }
  private let defaults = Settings(language: .ar, use24h: true)

  private func input(_ title: String, _ target: LocalDateTime, _ color: CountdownColor = .violet) -> CountdownInput {
    CountdownInput(title: title, target: target, color: color, icon: .star)
  }

  private func json(_ text: String) -> [String: Any] {
    try! JSONSerialization.jsonObject(with: Data(text.utf8)) as! [String: Any]
  }

  func testFirstLaunchSeedsTheUsersExampleOnce() {
    let seeded = CountdownsState().seededIfNeeded(id: "seed", now: now)
    XCTAssertTrue(seeded.seeded)
    XCTAssertEqual(seeded.countdowns.count, 1)
    let c = seeded.countdowns[0]
    XCTAssertEqual(c.title, "")
    XCTAssertEqual(c.target, SEED_TARGET)
    XCTAssertEqual(c.color, .ember)
    XCTAssertEqual(c.icon, .calendar)
    XCTAssertEqual(c.createdAt, now)
    XCTAssertEqual(seeded.pinnedId, "seed")
    let emptied = seeded.removeAll()
    XCTAssertEqual(emptied.seededIfNeeded(id: "again", now: now), emptied)
    XCTAssertTrue(emptied.countdowns.isEmpty)
    XCTAssertNil(emptied.pinnedId)
  }

  func testAddTrimsAndPinsTheFirstOrWhenAsked() {
    var s = CountdownsState(seeded: true)
    s = s.add(input("  Trip  ", dt(2027, 1, 10)), pin: false, id: "a", now: now)
    XCTAssertEqual(s.countdowns.first?.title, "Trip")
    XCTAssertEqual(s.pinnedId, "a")
    s = s.add(input("Exam", dt(2026, 12, 1, 9)), pin: false, id: "b", now: now)
    XCTAssertEqual(s.pinnedId, "a")
    s = s.add(input("Birthday", dt(2027, 3, 3)), pin: true, id: "c", now: now)
    XCTAssertEqual(s.pinnedId, "c")
    XCTAssertEqual(s.pinned?.id, "c")
  }

  func testUpdateKeepsIdAndCreationTime() {
    var s = CountdownsState(seeded: true).add(input("Trip", dt(2027, 1, 10)), pin: false, id: "a", now: now)
    s = s.update("a", CountdownInput(title: " Trip to Paris ", target: dt(2027, 2, 1, 8, 30), color: .ocean, icon: .plane))
    let c = s.countdowns[0]
    XCTAssertEqual(c.id, "a")
    XCTAssertEqual(c.createdAt, now)
    XCTAssertEqual(c.title, "Trip to Paris")
    XCTAssertEqual(c.color, .ocean)
    XCTAssertEqual(c.icon, .plane)
  }

  func testRemovingThePinnedOneHandsItToTheNextDue() {
    var s = CountdownsState(seeded: true)
    s = s.add(input("Passed", dt(2026, 1, 1)), pin: false, id: "past", now: now)
    s = s.add(input("Far", dt(2028, 1, 1)), pin: false, id: "far", now: now)
    s = s.add(input("Soon", dt(2026, 11, 2)), pin: true, id: "soon", now: now)
    XCTAssertEqual(s.remove("soon", now: now).pinnedId, "far")
    XCTAssertEqual(s.remove("far", now: now).pinnedId, "soon")
    XCTAssertEqual(s.pin("nope"), s)
    XCTAssertEqual(s.pin("past").pinnedId, "past")
  }

  func testSortsUpcomingSoonestFirstThenPassedMostRecentFirst() {
    var s = CountdownsState(seeded: true)
    s = s.add(input("old", dt(2025, 1, 1)), pin: false, id: "old", now: now)
    s = s.add(input("far", dt(2028, 1, 1)), pin: false, id: "far", now: now)
    s = s.add(input("recent", dt(2026, 10, 1)), pin: false, id: "recent", now: now)
    s = s.add(input("soon", dt(2026, 11, 2)), pin: false, id: "soon", now: now)
    XCTAssertEqual(sortCountdowns(s.countdowns, now: now).map(\.id), ["soon", "far", "recent", "old"])
  }

  func testJsonRoundTrips() {
    var s = CountdownsState().seededIfNeeded(id: "seed", now: now)
    s = s.add(CountdownInput(title: "Graduation", target: dt(2027, 6, 30, 18, 30), color: .gold, icon: .graduation), pin: true, id: "g", now: now)
    let back = StoreJson.decodeCountdowns(StoreJson.object(StoreJson.data(StoreJson.encodeCountdowns(s))), now: 0)
    XCTAssertEqual(back, s)
    let settings = Settings(language: .ar, themeMode: .dark, showSeconds: false, countUp: false, use24h: true)
    XCTAssertEqual(StoreJson.decodeSettings(StoreJson.object(StoreJson.data(StoreJson.encodeSettings(settings))), defaults: Settings(language: .en, use24h: false)), settings)
  }

  func testReadsTheAndroidAppsAndV1sShape() {
    // What v1 1.0.1 stored (the `state` of its zustand stores) — the Android app writes the same.
    let settings = StoreJson.decodeSettings(json(#"{"language":"en","themeMode":"system","showSeconds":true,"countUp":true,"use24h":false}"#), defaults: defaults)
    XCTAssertEqual(settings, Settings(language: .en, themeMode: .system, showSeconds: true, countUp: true, use24h: false))
    let state = StoreJson.decodeCountdowns(json(#"{"countdowns":[{"id":"mvqw3prnwzmfi8","title":"","target":{"year":2027,"month":9,"day":5,"hour":0,"minute":0},"color":"ember","icon":"calendar","createdAt":1793117904179}],"pinnedId":"mvqw3prnwzmfi8","seeded":true}"#), now: now)
    XCTAssertTrue(state.seeded)
    XCTAssertEqual(state.pinnedId, "mvqw3prnwzmfi8")
    XCTAssertEqual(state.countdowns.first?.target, dt(2027, 9, 5))
    XCTAssertEqual(state.countdowns.first?.createdAt, 1793117904179)
    XCTAssertEqual(state.seededIfNeeded(id: "x", now: now), state)
  }

  func testSanitizesWhatItDoesntUnderstand() {
    let state = StoreJson.decodeCountdowns(json(#"""
      {"countdowns":[
        {"id":"ok","title":5,"target":{"year":2027,"month":2,"day":3,"hour":4,"minute":5},"color":"neon","icon":"rocket"},
        {"id":"bad-date","target":{"year":2027,"month":2,"day":30,"hour":0,"minute":0}},
        {"id":"bad-hour","target":{"year":2027,"month":2,"day":3,"hour":24,"minute":0}},
        {"id":"fraction","target":{"year":2027,"month":2,"day":3.5,"hour":0,"minute":0}},
        {"title":"no id","target":{"year":2027,"month":2,"day":3,"hour":0,"minute":0}},
        "junk"
      ],"pinnedId":"gone","seeded":"yes"}
      """#), now: now)
    XCTAssertEqual(state.countdowns.count, 1)
    let c = state.countdowns[0]
    XCTAssertEqual(c.id, "ok")
    XCTAssertEqual(c.title, "")
    XCTAssertEqual(c.color, .ember)
    XCTAssertEqual(c.icon, .calendar)
    XCTAssertEqual(c.createdAt, now)
    XCTAssertEqual(state.pinnedId, "ok")
    XCTAssertFalse(state.seeded)

    let settings = StoreJson.decodeSettings(json(#"{"language":"fr","themeMode":"neon","showSeconds":"no","use24h":false}"#), defaults: defaults)
    var expected = defaults
    expected.use24h = false
    XCTAssertEqual(settings, expected)
    XCTAssertEqual(StoreJson.decodeSettings(nil, defaults: defaults), defaults)
    XCTAssertEqual(StoreJson.decodeCountdowns(nil, now: now), CountdownsState())
  }

  func testIdsLookLikeV1s() {
    let id = newCountdownId(now: 1793117904179)
    XCTAssertTrue(id.hasPrefix(String(Int64(1793117904179), radix: 36)))
    XCTAssertEqual(id.count, String(Int64(1793117904179), radix: 36).count + 6)
  }
}
