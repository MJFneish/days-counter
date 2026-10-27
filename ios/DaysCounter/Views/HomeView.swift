import DaysCounterCore
import SwiftUI

/// Home (the Android app's HomeScreen): today's date, the pinned countdown as the hero (the one on
/// the widget) with "the wait in numbers", then every other countdown — upcoming soonest first,
/// passed ones after. Tapping any of them opens the editor sheet; the pin button moves one to the
/// hero + widget.
struct HomeView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    let now = clock.now
    let pinned = model.countdowns.pinned
    let others = sortCountdowns(model.countdowns.countdowns.filter { $0.id != pinned?.id }, now: now)

    ScrollView {
      VStack(alignment: .leading, spacing: 0) {
        HStack(spacing: Spacing.md) {
          VStack(alignment: .leading, spacing: 0) {
            Text(s["home.title"]).font(TypeScale.displayMedium).foregroundStyle(c.text).accessibilityAddTraits(.isHeader)
            Text(formatLongDate(fromInstant(now), theme.lang)).font(TypeScale.caption).foregroundStyle(c.textMuted)
          }
          .frame(maxWidth: .infinity, alignment: .leading)
          Button {
            model.openEditor(nil)
          } label: {
            Image(systemName: "plus")
              .font(.system(size: 20, weight: .semibold))
              .foregroundStyle(c.onPrimary)
              .frame(width: 48, height: 48)
              .background(c.primary, in: Circle())
          }
          .buttonStyle(PressScale())
          .accessibilityLabel(s["home.add"])
        }
        .padding(.bottom, Spacing.lg)

        if let pinned {
          HeroCard(countdown: pinned)
          if remaining(pinned.target, now: now).status == .upcoming || model.settings.countUp {
            StatsCard(countdown: pinned).padding(.top, Spacing.md)
          }
        } else {
          EmptyStateView()
        }

        if !others.isEmpty {
          SectionLabel(text: s["home.others"])
          VStack(spacing: Spacing.sm) {
            ForEach(others) { countdown in
              CountdownRowView(countdown: countdown)
            }
          }
          .animation(.easeInOut(duration: 0.22), value: others.map(\.id))
        }
      }
      .padding(.horizontal, Spacing.lg)
      .padding(.top, Spacing.md)
      .padding(.bottom, Spacing.xxxl)
    }
    .background(c.background.ignoresSafeArea())
  }
}
