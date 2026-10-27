import DaysCounterCore
import SwiftUI
import WidgetKit

/// Settings → Home-screen widget (the Android app's WidgetSection): which countdown the widget
/// shows ("Change" opens the picker), and how to add it — iOS has no in-app "add widget" request,
/// so the steps are shown for the home screen and the lock screen (where iOS does allow it).
struct WidgetSection: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    let pinned = model.countdowns.pinned
    SectionLabel(text: s["settings.widget"])
    Card(spacing: Spacing.md) {
      Text(s["settings.widgetShows"]).font(TypeScale.caption).foregroundStyle(c.textMuted)
      HStack(spacing: Spacing.md) {
        let palette = pinned.map { CountdownPalette.of($0.color) }
        IconBadge(
          symbol: pinned?.icon.symbol ?? "square.grid.2x2",
          tint: palette?.accent(theme.isDark) ?? c.primary,
          background: palette?.soft(theme.isDark) ?? c.primarySoft
        )
        VStack(alignment: .leading, spacing: 2) {
          Text(pinned.map { countdownTitle($0, theme.lang) } ?? s["settings.widgetNone"]).font(TypeScale.bodyStrong).foregroundStyle(c.text).lineLimit(1)
          if let pinned {
            Text(relativePhrase(remaining(pinned.target, now: clock.now), s)).font(TypeScale.caption).foregroundStyle(c.textMuted)
          }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        if model.countdowns.countdowns.count > 1 {
          Button(s["common.change"]) { model.pickerOpen = true }
            .font(TypeScale.bodyStrong)
            .foregroundStyle(c.primary)
            .padding(.horizontal, Spacing.md)
            .frame(minHeight: 40)
            .background(c.surfaceAlt, in: Capsule())
            .buttonStyle(.plain)
            .accessibilityLabel(Text(verbatim: "\(s["common.change"]) — \(s["settings.chooseCountdown"])"))
        }
      }
      note("info.circle", s["settings.addWidgetIos"])
      note("lock", s["settings.lockScreenIos"])
    }
  }

  private func note(_ symbol: String, _ text: String) -> some View {
    HStack(alignment: .top, spacing: Spacing.sm) {
      Image(systemName: symbol).font(.system(size: 14)).foregroundStyle(theme.colors.textMuted).accessibilityHidden(true)
      Text(text).font(TypeScale.caption).foregroundStyle(theme.colors.textMuted).frame(maxWidth: .infinity, alignment: .leading)
    }
  }
}

/// Every countdown as a radio, to choose the one on the widget (and the Home hero). Applies on tap.
struct CountdownPickerView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  @Environment(\.dismiss) private var dismiss

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    VStack(alignment: .leading, spacing: 0) {
      Text(s["settings.chooseCountdown"])
        .font(TypeScale.title)
        .foregroundStyle(c.text)
        .padding(.horizontal, Spacing.lg)
        .padding(.top, Spacing.xl)
        .padding(.bottom, Spacing.md)
        .accessibilityAddTraits(.isHeader)
      ScrollView {
        VStack(spacing: Spacing.sm) {
          ForEach(sortCountdowns(model.countdowns.countdowns, now: clock.now)) { countdown in
            row(countdown)
          }
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.bottom, Spacing.lg)
      }
    }
    .background(c.surface.ignoresSafeArea())
  }

  private func row(_ countdown: Countdown) -> some View {
    let c = theme.colors
    let active = countdown.id == model.countdowns.pinnedId
    let palette = CountdownPalette.of(countdown.color)
    let title = countdownTitle(countdown, theme.lang)
    return Button {
      model.updateCountdowns { $0.pin(countdown.id) }
      model.showToast(theme.strings["toast.pinned"])
      dismiss()
    } label: {
      HStack(spacing: Spacing.md) {
        IconBadge(symbol: countdown.icon.symbol, tint: palette.accent(theme.isDark), background: palette.soft(theme.isDark))
        VStack(alignment: .leading, spacing: 2) {
          Text(title).font(TypeScale.bodyStrong).foregroundStyle(c.text).lineLimit(1)
          Text(relativePhrase(remaining(countdown.target, now: clock.now), theme.strings)).font(TypeScale.caption).foregroundStyle(c.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        Image(systemName: active ? "checkmark.circle.fill" : "circle")
          .font(.system(size: 20))
          .foregroundStyle(active ? c.primary : c.textMuted)
      }
      .padding(Spacing.md)
      .frame(minHeight: 64)
      .background(active ? c.primarySoft : .clear, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
      .overlay(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous).strokeBorder(active ? c.primary : c.border, lineWidth: 1.5))
      .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
    .accessibilityLabel(title)
    .accessibilityAddTraits(active ? [.isSelected] : [])
  }
}

/// The widget's faces at their real sizes, for the UI test's screenshots (DAYS_COUNTER_WIDGET_GALLERY
/// = 1): a widget itself can't be captured on CI. Same views as the widget extension.
struct WidgetGallery: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock

  var body: some View {
    let snapshot = WidgetSnapshot(settings: model.settings, state: model.countdowns)
    let date = Date(timeIntervalSince1970: TimeInterval(clock.now) / 1000)
    ScrollView {
      VStack(spacing: 20) {
        HStack(spacing: 20) {
          tile(snapshot, date, .systemSmall, width: 170, height: 170)
          VStack(spacing: 12) {
            WidgetFace(snapshot: snapshot, date: date, family: .accessoryCircular)
              .frame(width: 76, height: 76)
              .background(.white.opacity(0.15), in: Circle())
            WidgetFace(snapshot: snapshot, date: date, family: .accessoryInline)
              .font(.system(size: 13, weight: .semibold))
              .frame(width: 150)
          }
          .foregroundStyle(.white)
        }
        tile(snapshot, date, .systemMedium, width: 364, height: 170)
        WidgetFace(snapshot: snapshot, date: date, family: .accessoryRectangular)
          .foregroundStyle(.white)
          .padding(8)
          .frame(width: 172, height: 76)
          .background(.white.opacity(0.15), in: RoundedRectangle(cornerRadius: 12))
      }
      .padding(.vertical, 60)
      .frame(maxWidth: .infinity)
    }
    .background(Color(hex: 0x2B2B3A).ignoresSafeArea())
    .accessibilityIdentifier("widgetGallery")
  }

  private func tile(_ snapshot: WidgetSnapshot, _ date: Date, _ family: WidgetFamily, width: CGFloat, height: CGFloat) -> some View {
    WidgetFace(snapshot: snapshot, date: date, family: family)
      .padding(16)
      .frame(width: width, height: height)
      .background(WidgetBackground(snapshot: snapshot))
      .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
  }
}
