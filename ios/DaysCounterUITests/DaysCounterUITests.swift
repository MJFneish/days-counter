import XCTest

/// Smoke test on the iOS simulator: the app starts and shows its screens. Screenshots go to
/// ~/dayscounter-screenshots on the machine running the simulator (CI uploads them as the
/// `ios-screenshots` artifact) and into the test result. Same approach as Mawakit's UI test.
final class DaysCounterUITests: XCTestCase {
  override func setUp() {
    continueAfterFailure = false
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

  func testLaunches() {
    let app = XCUIApplication()
    app.launch()
    XCTAssertTrue(app.staticTexts["appName"].waitForExistence(timeout: 15))
    screenshot("01-launch")
  }
}
