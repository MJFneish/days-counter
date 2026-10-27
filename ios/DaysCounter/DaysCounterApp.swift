import DaysCounterCore
import SwiftUI

@main
struct DaysCounterApp: App {
  @StateObject private var model = AppModel()
  @StateObject private var clock = Clock()

  var body: some Scene {
    WindowGroup {
      // The objects go outside Themed: the environment flows down, and Themed reads the model.
      RootView()
        .modifier(Themed())
        .environmentObject(model)
        .environmentObject(clock)
    }
  }
}

/// The loading screen stays up at least this long even when startup is instant — a flash reads as
/// a glitch, not as speed (same value as the Android app).
private let minLoadingVisible: TimeInterval = 1.2

/// Home and Settings in the native tab bar (mirrored in Arabic), the loading screen over them at
/// launch, toasts on top.
struct RootView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  @Environment(\.scenePhase) private var scenePhase
  @State private var tab = 0
  @State private var loading = true

  var body: some View {
    ZStack {
      TabView(selection: $tab) {
        HomeView()
          .tabItem { Label(theme.strings["tabs.home"], systemImage: "hourglass") }
          .tag(0)
        SettingsView()
          .tabItem { Label(theme.strings["tabs.settings"], systemImage: "gearshape") }
          .tag(1)
      }
      ToastOverlay()
      if loading {
        LoadingView().transition(.opacity)
      }
    }
    .background(theme.colors.background.ignoresSafeArea())
    .task {
      try? await Task.sleep(nanoseconds: UInt64(minLoadingVisible * 1_000_000_000))
      withAnimation(.easeOut(duration: 0.38)) { loading = false }
    }
    .onAppear { clock.start(everySecond: model.settings.showSeconds) }
    .onChange(of: model.settings.showSeconds) { _, everySecond in clock.start(everySecond: everySecond) }
    .onChange(of: scenePhase) { _, phase in
      switch phase {
      case .active: clock.start(everySecond: model.settings.showSeconds)
      case .background: clock.stop()
      default: break
      }
    }
  }
}
