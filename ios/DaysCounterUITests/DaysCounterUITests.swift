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

  /// The hero card (one accessibility element: "<title>. in N days").
  private func hero(_ app: XCUIApplication) -> XCUIElement {
    app.descendants(matching: .any)["hero"].firstMatch
  }

  /// Scrolls the editor sheet until `target` can be tapped, by dragging section labels towards the
  /// sheet's title — never the middle of the screen, where a wheel would turn instead.
  private func scrollEditor(_ app: XCUIApplication, to target: XCUIElement, title: String) {
    let top = app.staticTexts[title]
    for _ in 0..<8 where !(target.exists && target.isHittable) {
      let handles = ["NAME", "DATE", "TIME", "COLOR", "ICON", "Optional — the date is shown when it's empty"].map { app.staticTexts[$0] }
      guard let handle = handles.first(where: { $0.exists && $0.isHittable && $0.frame.minY > top.frame.maxY + 120 }) else { break }
      handle.press(forDuration: 0.05, thenDragTo: top)
    }
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
    // The hero is one element: "<title>. in N days".
    XCTAssertTrue(element(app, labelStartingWith: "Sunday, 5 September 2027. in ").waitForExistence(timeout: 5))
    XCTAssertTrue(app.staticTexts["The wait in numbers"].exists)
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
    // The version sits in an LTR isolate ("Version \u{2066}2.0.0-…\u{2069}"), so match the number alone.
    let version = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "2.0.0")).firstMatch
    XCTAssertTrue(version.waitForExistence(timeout: 5))
    screenshot("04-settings-about-en")

    // Delete all → confirm → empty Home.
    app.buttons["Delete all countdowns"].tap()
    XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))
    app.alerts.firstMatch.buttons["Delete"].tap()
    app.tabBars.buttons["Home"].tap()
    XCTAssertTrue(app.staticTexts["No countdowns yet"].waitForExistence(timeout: 5))
    screenshot("05-home-empty-en")
  }

  func testAddPinEditDelete() {
    let app = launch("en")
    XCTAssertTrue(app.staticTexts["Countdowns"].waitForExistence(timeout: 15))

    // Add: name, December, Ocean, Trophy → Save.
    app.buttons["New countdown"].tap()
    XCTAssertTrue(app.staticTexts["New countdown"].waitForExistence(timeout: 5))
    let name = app.textFields.firstMatch
    name.tap()
    name.typeText("Graduation\n") // return closes the keyboard
    app.pickerWheels.element(boundBy: 1).adjust(toPickerWheelValue: "December")
    screenshot("06-editor-top-en")
    scrollEditor(app, to: app.buttons["Trophy"], title: "New countdown")
    app.buttons["Ocean"].tap()
    app.buttons["Trophy"].tap()
    screenshot("07-editor-bottom-en")
    app.buttons["Save"].tap()
    XCTAssertTrue(app.staticTexts["OTHER COUNTDOWNS"].waitForExistence(timeout: 5))
    XCTAssertTrue(element(app, labelStartingWith: "Graduation.").exists)
    screenshot("08-home-two-en")

    // Settings → widget → Change → Graduation: it becomes the hero (and the widget's countdown).
    app.tabBars.buttons["Settings"].tap()
    app.swipeUp()
    XCTAssertTrue(app.staticTexts["HOME-SCREEN WIDGET"].waitForExistence(timeout: 5))
    screenshot("10-settings-widget-en")
    app.buttons["Change — Countdown on the widget"].tap()
    XCTAssertTrue(app.staticTexts["Countdown on the widget"].waitForExistence(timeout: 5))
    screenshot("10b-picker-en")
    app.buttons["Graduation"].tap()
    app.tabBars.buttons["Home"].tap()
    XCTAssertTrue(app.buttons["Show Sunday, 5 September 2027 on the widget"].waitForExistence(timeout: 5))
    screenshot("09-home-pinned-en")

    // Pin the first one back from its row.
    app.buttons["Show Sunday, 5 September 2027 on the widget"].tap()
    XCTAssertTrue(app.buttons["Show Graduation on the widget"].waitForExistence(timeout: 5))

    // Edit Graduation through its row, then delete it.
    element(app, labelStartingWith: "Graduation.").tap()
    XCTAssertTrue(app.staticTexts["Edit countdown"].waitForExistence(timeout: 5))
    scrollEditor(app, to: app.buttons["Delete countdown"], title: "Edit countdown")
    app.buttons["Delete countdown"].tap()
    XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 5))
    app.alerts.firstMatch.buttons["Delete"].tap()
    XCTAssertTrue(element(app, labelStartingWith: "Sunday, 5 September 2027.").waitForExistence(timeout: 5))
    XCTAssertFalse(element(app, labelStartingWith: "Graduation.").exists)
  }

  /// The widget's faces (small, medium, lock-screen) in the in-app gallery.
  func testWidgetFaces() {
    for (lang, name) in [("en", "20-widgets-en"), ("ar", "21-widgets-ar")] {
      let app = XCUIApplication()
      app.launchEnvironment["DAYS_COUNTER_RESET"] = "1"
      app.launchEnvironment["DAYS_COUNTER_LANGUAGE"] = lang
      app.launchEnvironment["DAYS_COUNTER_WIDGET_GALLERY"] = "1"
      app.launch()
      XCTAssertTrue(app.scrollViews["widgetGallery"].waitForExistence(timeout: 15) || app.otherElements["widgetGallery"].exists)
      sleep(1)
      screenshot(name)
      app.terminate()
    }
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
    XCTAssertTrue(hero(app).waitForExistence(timeout: 5))
    XCTAssertTrue(hero(app).label.hasPrefix("الأحد ٥ أيلول ٢٠٢٧"), "hero label: \(hero(app).label)")
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
