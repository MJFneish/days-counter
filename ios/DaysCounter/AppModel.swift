import DaysCounterCore
import Foundation
import SwiftUI
import WidgetKit

/// The app's state — settings and countdowns — persisted as JSON in the App Group's UserDefaults
/// (SharedStore, where the widget reads it too), saved on every change, after which the widget is
/// reloaded. The iOS twin of the Android app's AppStore + WidgetSync. Launch-environment overrides
/// make the UI tests deterministic.
@MainActor
final class AppModel: ObservableObject {
  @Published private(set) var settings: Settings
  @Published private(set) var countdowns: CountdownsState
  /// The add/edit sheet: open with a countdown's id, or `nil` for a new one.
  @Published var editor: EditorRequest?
  /// Settings → widget → "Change": the countdown picker sheet.
  @Published var pickerOpen = false
  /// The latest confirmation toast ("Countdown added"); `toastId` changes with each one.
  @Published private(set) var toast: String?
  @Published private(set) var toastId = 0

  private let defaults: UserDefaults

  init() {
    let defaults = SharedStore.defaults
    self.defaults = defaults
    let env = ProcessInfo.processInfo.environment
    if env["DAYS_COUNTER_RESET"] == "1" {
      defaults.removeObject(forKey: SharedStore.keySettings)
      defaults.removeObject(forKey: SharedStore.keyCountdowns)
    }

    var settings = SharedStore.loadSettings(defaults)
    if let lang = env["DAYS_COUNTER_LANGUAGE"] { settings.language = Lang.of(lang) }
    if let mode = env["DAYS_COUNTER_THEME"].flatMap(ThemeMode.init(rawValue:)) { settings.themeMode = mode }
    self.settings = settings

    let now = nowMs()
    self.countdowns = SharedStore.loadCountdowns(defaults, now: now).seededIfNeeded(id: newCountdownId(now: now), now: now)
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

  func openEditor(_ countdownId: String?) {
    editor = EditorRequest(countdownId: countdownId)
  }

  func showToast(_ text: String) {
    toast = text
    toastId += 1
  }

  /// The widget follows every change (and every return to the foreground).
  func reloadWidget() {
    WidgetCenter.shared.reloadAllTimelines()
  }

  private func saveSettings() {
    SharedStore.save(settings, defaults)
    reloadWidget()
  }

  private func saveCountdowns() {
    SharedStore.save(countdowns, defaults)
    reloadWidget()
  }
}

/// Which countdown the editor sheet edits (`nil` = a new one); a fresh id for every opening.
struct EditorRequest: Identifiable {
  let id = UUID()
  let countdownId: String?
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
