import DaysCounterCore
import SwiftUI

@main
struct DaysCounterApp: App {
  var body: some Scene {
    WindowGroup {
      ZStack {
        Color(red: 0x17 / 255, green: 0x0B / 255, blue: 0x26 / 255).ignoresSafeArea()
        Text("Days Counter")
          .font(.title.bold())
          .foregroundStyle(.white)
          .accessibilityIdentifier("appName")
      }
    }
  }
}
