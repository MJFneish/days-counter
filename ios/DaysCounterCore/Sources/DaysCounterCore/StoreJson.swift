import Foundation

/// The persisted JSON — the same shapes as the Android app (and v1's zustand stores). Decoding
/// sanitizes: anything malformed falls back to a default instead of failing the launch.
public enum StoreJson {
  public static func encodeSettings(_ s: Settings) -> [String: Any] {
    ["language": s.language.code, "themeMode": s.themeMode.rawValue, "showSeconds": s.showSeconds, "countUp": s.countUp, "use24h": s.use24h]
  }

  public static func decodeSettings(_ o: [String: Any]?, defaults: Settings) -> Settings {
    guard let o else { return defaults }
    var s = defaults
    if let language = o["language"] as? String, language == "en" || language == "ar" { s.language = Lang.of(language) }
    if let mode = (o["themeMode"] as? String).flatMap(ThemeMode.init(rawValue:)) { s.themeMode = mode }
    if let value = bool(o["showSeconds"]) { s.showSeconds = value }
    if let value = bool(o["countUp"]) { s.countUp = value }
    if let value = bool(o["use24h"]) { s.use24h = value }
    return s
  }

  public static func encodeCountdowns(_ state: CountdownsState) -> [String: Any] {
    [
      "countdowns": state.countdowns.map { c -> [String: Any] in
        [
          "id": c.id,
          "title": c.title,
          "target": ["year": c.target.year, "month": c.target.month, "day": c.target.day, "hour": c.target.hour, "minute": c.target.minute],
          "color": c.color.rawValue,
          "icon": c.icon.rawValue,
          "createdAt": c.createdAt,
        ]
      },
      "pinnedId": state.pinnedId ?? NSNull(),
      "seeded": state.seeded,
    ]
  }

  public static func decodeCountdowns(_ o: [String: Any]?, now: Int64) -> CountdownsState {
    guard let o else { return CountdownsState() }
    var countdowns: [Countdown] = []
    for item in (o["countdowns"] as? [Any]) ?? [] {
      guard let c = item as? [String: Any], let id = c["id"] as? String, let target = decodeTarget(c["target"]) else { continue }
      countdowns.append(Countdown(
        id: id,
        title: (c["title"] as? String) ?? "",
        target: target,
        color: (c["color"] as? String).flatMap(CountdownColor.init(rawValue:)) ?? .ember,
        icon: (c["icon"] as? String).flatMap(CountdownIcon.init(rawValue:)) ?? .calendar,
        createdAt: number(c["createdAt"]).map { Int64($0) } ?? now
      ))
    }
    let stored = o["pinnedId"] as? String
    let pinnedId = countdowns.contains { $0.id == stored } ? stored : countdowns.first?.id
    return CountdownsState(countdowns: countdowns, pinnedId: pinnedId, seeded: bool(o["seeded"]) == true)
  }

  public static func data(_ object: [String: Any]) -> Data {
    (try? JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])) ?? Data("{}".utf8)
  }

  public static func object(_ data: Data?) -> [String: Any]? {
    guard let data else { return nil }
    return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
  }

  private static func decodeTarget(_ value: Any?) -> LocalDateTime? {
    guard let t = value as? [String: Any] else { return nil }
    func int(_ key: String) -> Int? {
      guard let n = number(t[key]), n == n.rounded(.down) else { return nil }
      return Int(n)
    }
    guard let year = int("year"), let month = int("month"), let day = int("day"), let hour = int("hour"), let minute = int("minute") else { return nil }
    guard isValidDate(year, month, day), (0...23).contains(hour), (0...59).contains(minute) else { return nil }
    return LocalDateTime(year: year, month: month, day: day, hour: hour, minute: minute)
  }

  /// A JSON number (not a Bool — NSNumber bridges both).
  private static func number(_ value: Any?) -> Double? {
    guard let n = value as? NSNumber, !isBool(n) else { return nil }
    return n.doubleValue
  }

  /// A JSON boolean (not a number).
  private static func bool(_ value: Any?) -> Bool? {
    guard let n = value as? NSNumber, isBool(n) else { return nil }
    return n.boolValue
  }

  private static func isBool(_ n: NSNumber) -> Bool { CFGetTypeID(n) == CFBooleanGetTypeID() }
}
