import XCTest

/// Smoke test on the iOS simulator: every screen opens and shows its key content, in English and
/// Arabic, light and dark. Each launch starts from fresh data (DAYS_COUNTER_RESET) in a known
/// language and theme. Screenshots go to ~/dayscounter-screenshots on the machine running the
/// simulator (CI uploads them as the `ios-screenshots` artifact) and into the test result. Same
/// approach as Mawakit's UI test.
final class DaysCounterUITests: XCTestCase {
  override func setUp() {
    continueAfterFailure = false
  }

  private func launch(_ language: String, theme: String = "light") -> XCUIApplication {
    let app = XCUIApplication()
    app.launchEnvironment["DAYS_COUNTER_RESET"] = "1"
    app.launchEnvironment["DAYS_COUNTER_LANGUAGE"] = language
    app.launchEnvironment["DAYS_COUNTER_THEME"] = theme
    app.launch()
    return app
  }

  private func screenshot(_ name: String) {
    let shot = XCUIScreen.main.screenshot()
    let attachment = XCTAttachment(screenshot: shot)
    attachment.name = name
    attachment.lifetime = .keepAlways
    add(attachment)
    let home = ProcessInfo.processInfo.environment["SIMULATOR_HOST_HOME"] ?? NSTemporaryDirectory()
    let folder = URL(fileURLWithPath: home).appendingPathComponent("dayscounter-screenshots")
    try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
    try? shot.pngRepresentation.write(to: folder.appendingPathComponent("\(name).png"))
  }

  /// Any element whose label starts with `prefix`.
  private func element(_ app: XCUIApplication, labelStartingWith prefix: String) -> XCUIElement {
    app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
  }

  func testEnglishLight() {
    let app = launch("en")
    screenshot("00-loading-en")
    XCTAssertTrue(app.staticTexts["Countdowns"].waitForExistence(timeout: 15))
    // The seeded example: 5 September 2027, titled by its date.
    XCTAssertTrue(app.staticTexts["Sunday, 5 September 2027"].waitForExistence(timeout: 5))
    XCTAssertTrue(element(app, labelStartingWith: "in ").exists)
    screenshot("01-home-en")

    app.tabBars.buttons["Settings"].tap()
    XCTAssertTrue(app.staticTexts["LANGUAGE"].waitForExistence(timeout: 5))
    XCTAssertTrue(app.staticTexts["Follow system"].exists)
    screenshot("02-settings-en")

    // Launched in light, so "Follow system" is off and the Light / Dark tiles show.
    XCTAssertTrue(app.buttons["Dark"].exists)
    app.buttons["Dark"].tap()
    screenshot("03-settings-dark-en")
    // Following the system again hides the tiles. The switch sits at the row's trailing (right, in
    // English) edge; its label doesn't toggle it.
    app.switches.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5)).tap()
    let gone = expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: app.buttons["Dark"])
    wait(for: [gone], timeout: 5)

    app.swipeUp()
    XCTAssertTrue(element(app, labelStartingWith: "Version 2.0.0").waitForExistence(timeout: 5))
    screenshot("04-settings-about-en")

    // Delete all → confirm → empty Home.
    app.buttons["Delete all countdowns"].tap()
    XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))
    app.alerts.firstMatch.buttons["Delete"].tap()
    app.tabBars.buttons["Home"].tap()
    XCTAssertTrue(app.staticTexts["No countdowns yet"].waitForExistence(timeout: 5))
    screenshot("05-home-empty-dark-en")
  }

  func testArabicSwitchesInstantly() {
    let app = launch("en")
    XCTAssertTrue(app.staticTexts["Countdowns"].waitForExistence(timeout: 15))
    app.tabBars.buttons["Settings"].tap()
    app.buttons["العربية"].tap()
    XCTAssertTrue(app.staticTexts["الإعدادات"].waitForExistence(timeout: 5))
    XCTAssertTrue(app.staticTexts["اللغة"].exists)
    screenshot("11-settings-ar")
    app.tabBars.buttons["الرئيسية"].tap()
    XCTAssertTrue(app.staticTexts["الأحد ٥ أيلول ٢٠٢٧"].waitForExistence(timeout: 5))
    screenshot("12-home-ar")
  }

  func testArabicDark() {
    let app = launch("ar", theme: "dark")
    XCTAssertTrue(app.staticTexts["العدّادات"].waitForExistence(timeout: 15))
    screenshot("13-home-ar-dark")
    app.tabBars.buttons["الإعدادات"].tap()
    XCTAssertTrue(app.staticTexts["المظهر"].waitForExistence(timeout: 5))
    screenshot("14-settings-ar-dark")
  }
}
