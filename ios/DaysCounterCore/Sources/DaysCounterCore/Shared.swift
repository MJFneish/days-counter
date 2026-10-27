import Foundation

/// The App Group store both the app and its widget read: settings and countdowns as JSON (the same
/// shapes as the Android app's SharedPreferences), keyed `settings` / `countdowns`.
public enum SharedStore {
  public static let appGroup = "group.com.devlance.dayscounter"
  public static let keySettings = "settings"
  public static let keyCountdowns = "countdowns"

  /// The App Group's defaults; the app's own when the group isn't available (unsigned simulator
  /// builds), where the widget simply sees no data.
  public static var defaults: UserDefaults { UserDefaults(suiteName: appGroup) ?? .standard }

  /// First-launch defaults, as on Android: the phone's language and 12/24-hour clock.
  public static func deviceSettings() -> Settings {
    let clockFormat = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: .current) ?? ""
    return Settings(language: (Locale.preferredLanguages.first ?? "en").hasPrefix("ar") ? .ar : .en, use24h: !clockFormat.contains("a"))
  }

  public static func loadSettings(_ defaults: UserDefaults = SharedStore.defaults) -> Settings {
    StoreJson.decodeSettings(StoreJson.object(defaults.data(forKey: keySettings)), defaults: deviceSettings())
  }

  public static func loadCountdowns(_ defaults: UserDefaults = SharedStore.defaults, now: Int64 = nowMs()) -> CountdownsState {
    StoreJson.decodeCountdowns(StoreJson.object(defaults.data(forKey: keyCountdowns)), now: now)
  }

  public static func save(_ settings: Settings, _ defaults: UserDefaults = SharedStore.defaults) {
    defaults.set(StoreJson.data(StoreJson.encodeSettings(settings)), forKey: keySettings)
  }

  public static func save(_ state: CountdownsState, _ defaults: UserDefaults = SharedStore.defaults) {
    defaults.set(StoreJson.data(StoreJson.encodeCountdowns(state)), forKey: keyCountdowns)
  }
}

/// What the widget draws: the pinned countdown and how to word it (the iOS twin of the Android
/// app's WidgetPayload). The widget computes the numbers itself from the target.
public struct WidgetSnapshot: Equatable, Sendable {
  public let lang: Lang
  public let countUp: Bool
  public let countdown: Countdown?
  /// The name, or the date when it has none.
  public let title: String
  /// Second line: the date (when the title isn't already the date) and the time.
  public let detail: String

  public init(settings: Settings, state: CountdownsState) {
    let strings = Strings.of(settings.language)
    lang = settings.language
    countUp = settings.countUp
    countdown = state.pinned
    title = state.pinned.map { countdownTitle($0, settings.language) } ?? ""
    detail = state.pinned.map { countdownSubtitle($0, strings, use24h: settings.use24h, short: true) } ?? ""
  }

  public var strings: Strings { Strings.of(lang) }
}

public enum WidgetTimeline {
  /// The instants the day count changes after `now` (up to `count` of them) — the widget's timeline
  /// entries, so it shows the right count without the app running. The live H:MM:SS in between is a
  /// self-updating timer text. At a change instant itself `remaining` still shows the old count (it
  /// flips just after), so each step looks from 1 ms past the previous change, and the widget dates
  /// its entries half a second after each change.
  public static func changes(_ target: LocalDateTime, after now: Int64, count: Int, zone: TimeZone = TimeZones.device) -> [Int64] {
    var out: [Int64] = []
    var t = now
    while out.count < count {
      let next = remaining(target, now: t, zone: zone).nextChangeAt
      guard next >= t else { break }
      out.append(next)
      t = next + 1
    }
    return out
  }

  /// When the widget shows the count that starts at `change`.
  public static let entryDelayMs: Int64 = 500
}
