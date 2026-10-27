import Foundation

/// The app's texts in one language, from Resources/i18n/<lang>.json (the same files as the Android
/// app): nested keys addressed with dots ("settings.language"), `{{name}}` placeholders. The app's
/// language is its own setting, independent of the phone's, and switches instantly — so texts are
/// looked up here rather than through Localizable.strings. Missing keys fall back to English. Same
/// class as Mawakit's.
public final class Strings: @unchecked Sendable {
  public let lang: Lang
  private let table: [String: String]
  private let fallback: Strings?

  private init(lang: Lang, table: [String: String], fallback: Strings?) {
    self.lang = lang
    self.table = table
    self.fallback = fallback
  }

  public subscript(_ key: String) -> String {
    table[key] ?? fallback?[key] ?? key
  }

  public func t(_ key: String, _ args: (String, Any)...) -> String {
    var text = self[key]
    for (name, value) in args { text = text.replacingOccurrences(of: "{{\(name)}}", with: "\(value)") }
    return text
  }

  private static let lock = NSLock()
  nonisolated(unsafe) private static var cache: [Lang: Strings] = [:]

  public static func of(_ lang: Lang) -> Strings {
    lock.lock()
    defer { lock.unlock() }
    return unlocked(lang)
  }

  /// Built while `lock` is held (English first, as everyone's fallback).
  private static func unlocked(_ lang: Lang) -> Strings {
    if let cached = cache[lang] { return cached }
    let fallback: Strings? = lang == .en ? nil : unlocked(.en)
    let strings = Strings(lang: lang, table: load(lang), fallback: fallback)
    cache[lang] = strings
    return strings
  }

  static func load(_ lang: Lang) -> [String: String] {
    guard let json = try? JSONSerialization.jsonObject(with: CoreResources.data("i18n/\(lang.rawValue).json")) as? [String: Any] else {
      return [:]
    }
    var out: [String: String] = [:]
    func flatten(_ o: [String: Any], _ prefix: String) {
      for (key, value) in o {
        if let nested = value as? [String: Any] {
          flatten(nested, "\(prefix)\(key).")
        } else {
          out["\(prefix)\(key)"] = "\(value)"
        }
      }
    }
    flatten(json, "")
    return out
  }
}
