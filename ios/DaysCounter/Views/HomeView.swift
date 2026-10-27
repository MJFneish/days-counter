import DaysCounterCore
import SwiftUI

/// Home: today's date and the countdowns. (The hero card, stats and list rows land in the Home
/// phase; this first version lists every countdown with its relative phrase.)
struct HomeView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    let now = clock.now
    ScrollView {
      VStack(alignment: .leading, spacing: Spacing.sm) {
        Text(s["home.title"]).font(TypeScale.displayMedium).foregroundStyle(c.text).accessibilityAddTraits(.isHeader)
        Text(formatLongDate(fromInstant(now), theme.lang)).font(TypeScale.caption).foregroundStyle(c.textMuted)
        if model.countdowns.countdowns.isEmpty {
          Text(s["home.emptyTitle"]).font(TypeScale.title).foregroundStyle(c.text).padding(.top, Spacing.xl)
        }
        ForEach(model.countdowns.countdowns) { countdown in
          Card(spacing: 2) {
            Text(countdownTitle(countdown, theme.lang)).font(TypeScale.bodyStrong).foregroundStyle(c.text)
            Text(relativePhrase(remaining(countdown.target, now: now), s)).font(TypeScale.caption).foregroundStyle(c.textMuted)
          }
        }
      }
      .padding(.horizontal, Spacing.lg)
      .padding(.top, Spacing.md)
      .padding(.bottom, Spacing.xxxl)
      .frame(maxWidth: .infinity, alignment: .leading)
    }
    .background(c.background.ignoresSafeArea())
  }
}
