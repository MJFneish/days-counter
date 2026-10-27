import DaysCounterCore
import Foundation
import SwiftUI

/// The app's state — settings and countdowns — persisted as JSON in the App Group's UserDefaults
/// (where the widget reads it too), saved on every change. The iOS twin of the Android app's
/// AppStore. Launch-environment overrides make the UI tests deterministic.
@MainActor
final class AppModel: ObservableObject {
  static let appGroup = "group.com.devlance.dayscounter"
  private static let keySettings = "settings"
  private static let keyCountdowns = "countdowns"

  @Published private(set) var settings: Settings
  @Published private(set) var countdowns: CountdownsState
  /// The latest confirmation toast ("Countdown added"); `toastId` changes with each one.
  @Published private(set) var toast: String?
  @Published private(set) var toastId = 0

  private let defaults: UserDefaults

  init() {
    let defaults = UserDefaults(suiteName: AppModel.appGroup) ?? .standard
    self.defaults = defaults
    let env = ProcessInfo.processInfo.environment
    if env["DAYS_COUNTER_RESET"] == "1" {
      defaults.removeObject(forKey: AppModel.keySettings)
      defaults.removeObject(forKey: AppModel.keyCountdowns)
    }

    // First-launch defaults, as on Android: the phone's language and 12/24-hour clock.
    let clockFormat = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: .current) ?? ""
    let device = Settings(
      language: (Locale.preferredLanguages.first ?? "en").hasPrefix("ar") ? .ar : .en,
      use24h: !clockFormat.contains("a")
    )
    var settings = StoreJson.decodeSettings(StoreJson.object(defaults.data(forKey: AppModel.keySettings)), defaults: device)
    if let lang = env["DAYS_COUNTER_LANGUAGE"] { settings.language = Lang.of(lang) }
    if let mode = env["DAYS_COUNTER_THEME"].flatMap(ThemeMode.init(rawValue:)) { settings.themeMode = mode }
    self.settings = settings

    let now = nowMs()
    let loaded = StoreJson.decodeCountdowns(StoreJson.object(defaults.data(forKey: AppModel.keyCountdowns)), now: now)
    self.countdowns = loaded.seededIfNeeded(id: newCountdownId(now: now), now: now)
    saveSettings()
    saveCountdowns()
  }

  func updateSettings(_ transform: (inout Settings) -> Void) {
    var next = settings
    transform(&next)
    guard next != settings else { return }
    settings = next
    saveSettings()
  }

  func updateCountdowns(_ transform: (CountdownsState) -> CountdownsState) {
    let next = transform(countdowns)
    guard next != countdowns else { return }
    countdowns = next
    saveCountdowns()
  }

  func showToast(_ text: String) {
    toast = text
    toastId += 1
  }

  private func saveSettings() {
    defaults.set(StoreJson.data(StoreJson.encodeSettings(settings)), forKey: AppModel.keySettings)
  }

  private func saveCountdowns() {
    defaults.set(StoreJson.data(StoreJson.encodeCountdowns(countdowns)), forKey: AppModel.keyCountdowns)
  }
}

/// The app's one clock (epoch ms). Every countdown on screen reads it, so they all tick together.
/// Ticks on whole seconds — or whole minutes when "Show seconds" is off — aligned to the wall
/// clock, and stops while the app is in the background (the Android app's ClockProvider).
@MainActor
final class Clock: ObservableObject {
  @Published private(set) var now: Int64 = nowMs()
  private var timer: Timer?
  private var everySecond = true
  private var running = false

  func start(everySecond: Bool) {
    self.everySecond = everySecond
    running = true
    tick()
  }

  func stop() {
    running = false
    timer?.invalidate()
    timer = nil
  }

  private func tick() {
    timer?.invalidate()
    guard running else { return }
    let t = nowMs()
    now = t
    let step: Int64 = everySecond ? 1000 : 60_000
    // +5 ms so the tick lands just after the boundary, never a hair before it.
    let delay = TimeInterval(step - t % step + 5) / 1000
    let next = Timer(timeInterval: delay, repeats: false) { [weak self] _ in
      Task { @MainActor in self?.tick() }
    }
    // .common: keep ticking while a scroll view is being dragged.
    RunLoop.main.add(next, forMode: .common)
    timer = next
  }
}
