import XCTest
@testable import DaysCounterCore

/// The bundled i18n files (identical copies of the Android app's) load, and every key in one
/// language exists in the other.
final class LocalesTests: XCTestCase {
  private func keyPaths(_ lang: String) throws -> Set<String> {
    let json = try JSONSerialization.jsonObject(with: CoreResources.data("i18n/\(lang).json"))
    var out = Set<String>()
    func walk(_ node: Any, _ prefix: String) {
      if let object = node as? [String: Any] {
        for (key, value) in object { walk(value, prefix.isEmpty ? key : "\(prefix).\(key)") }
      } else {
        out.insert(prefix)
      }
    }
    walk(json, "")
    return out
  }

  func testEnglishAndArabicHaveTheSameKeys() throws {
    let en = try keyPaths("en")
    let ar = try keyPaths("ar")
    XCTAssertGreaterThan(en.count, 100)
    XCTAssertEqual(en.subtracting(ar), [], "missing in ar.json")
    XCTAssertEqual(ar.subtracting(en), [], "missing in en.json")
  }
}
