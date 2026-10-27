import DaysCounterCore
import SwiftUI

/// The countdown's diagonal gradient (top-left → bottom-right in both languages, like the Android
/// app and v1) with soft circles: x, y (fractions of the size), radius (fraction of the width),
/// opacity.
struct GradientFill: View {
  let palette: CountdownPalette
  var circles: [(CGFloat, CGFloat, CGFloat, Double)] = []

  static let heroCircles: [(CGFloat, CGFloat, CGFloat, Double)] = [(0.95, 0.05, 0.32, 0.08), (0.02, 1.02, 0.26, 0.06), (0.72, 0.78, 0.08, 0.05)]

  var body: some View {
    Canvas { context, size in
      let rect = CGRect(origin: .zero, size: size)
      context.fill(Path(rect), with: .linearGradient(Gradient(colors: [palette.from, palette.to]), startPoint: .zero, endPoint: CGPoint(x: size.width, y: size.height)))
      for circle in circles {
        let r = circle.2 * size.width
        let center = CGPoint(x: circle.0 * size.width, y: circle.1 * size.height)
        context.fill(Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: 2 * r, height: 2 * r)), with: .color(.white.opacity(circle.3)))
      }
    }
    .environment(\.layoutDirection, .leftToRight)
  }
}

/// Fills from the reading start: left in English, right in Arabic (the layout direction does it).
struct ProgressLine: View {
  let value: Double
  let color: Color
  let track: Color
  var height: CGFloat = 6

  var body: some View {
    GeometryReader { geo in
      ZStack(alignment: .leading) {
        Capsule().fill(track)
        Capsule().fill(color).frame(width: max(geo.size.width * 0.02, geo.size.width * CGFloat(min(1, max(0, value)))))
      }
    }
    .frame(height: height)
    .animation(.easeOut(duration: 0.6), value: value)
  }
}

/// A round icon bubble on a gradient (hero header, editor preview).
struct HeroBubble: View {
  @Environment(\.theme) private var theme
  let symbol: String
  var size: CGFloat = 44

  var body: some View {
    Image(systemName: symbol)
      .font(.system(size: size * 0.45, weight: .medium))
      .foregroundStyle(theme.colors.onHero)
      .frame(width: size, height: size)
      .background(theme.colors.onHeroPanel, in: Circle())
      .accessibilityHidden(true)
  }
}

/// One H/M/S tile on the hero; its digits roll when they change.
struct TimeTile: View {
  @Environment(\.theme) private var theme
  let value: Int
  let unit: CountUnit

  var body: some View {
    VStack(spacing: 0) {
      Text(localizeDigits(pad2(value), theme.lang))
        .font(TypeScale.digits)
        .foregroundStyle(theme.colors.onHero)
        .contentTransition(.numericText(countsDown: true))
        .animation(.snappy(duration: 0.22), value: value)
        .frame(height: 36)
      Text(unitWord(unit, value, theme.lang))
        .font(TypeScale.caption)
        .foregroundStyle(theme.colors.onHeroMuted)
        .lineLimit(1)
    }
    .frame(maxWidth: .infinity)
    .padding(.vertical, Spacing.sm)
    .background(theme.colors.onHeroPanel, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(countPhrase(unit, value, theme.lang))
  }
}

/// The pinned countdown, big (the Android app's HeroCountdown): icon + name, the day count, live
/// H/M/S tiles and the progress of the wait, in the countdown's own gradient. Tapping it edits it.
struct HeroCard: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  let countdown: Countdown

  var body: some View {
    let r = remaining(countdown.target, now: clock.now)
    let title = countdownTitle(countdown, theme.lang)
    return Button {
      model.openEditor(countdown.id)
    } label: {
      VStack(alignment: .leading, spacing: 0) {
        header(title)
        if r.status == .arrived { arrived }
        if r.status == .upcoming || model.settings.countUp {
          count(r)
        } else {
          Text(theme.strings["countdown.doneNoCountUp"]).font(TypeScale.title).foregroundStyle(theme.colors.onHero).padding(.top, Spacing.lg)
        }
        if r.status == .upcoming { progressBlock }
        if r.status == .arrived && model.settings.countUp {
          Text(theme.strings["countdown.arrivedBody"]).font(TypeScale.caption).foregroundStyle(theme.colors.onHeroMuted).padding(.top, Spacing.lg)
        }
      }
      .padding(Spacing.lg)
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(GradientFill(palette: CountdownPalette.of(countdown.color), circles: GradientFill.heroCircles))
      .clipShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
      .contentShape(RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
    }
    .buttonStyle(PressScale())
    .accessibilityElement(children: .ignore)
    // Verbatim: an interpolated literal would be a LocalizedStringKey, which wraps each value in
    // invisible bidi isolates in Arabic.
    .accessibilityLabel(Text(verbatim: "\(title). \(relativePhrase(r, theme.strings))"))
    .accessibilityHint(theme.t("home.edit", ("name", title)))
    .accessibilityAddTraits(.isButton)
    .accessibilityIdentifier("hero")
  }

  private func header(_ title: String) -> some View {
    let c = theme.colors
    let subtitle = countdownSubtitle(countdown, theme.strings, use24h: model.settings.use24h)
    return HStack(alignment: .center, spacing: Spacing.md) {
      HeroBubble(symbol: countdown.icon.symbol)
      VStack(alignment: .leading, spacing: 2) {
        Text(title).font(TypeScale.subtitle).foregroundStyle(c.onHero).lineLimit(2)
        if !subtitle.isEmpty { Text(subtitle).font(TypeScale.caption).foregroundStyle(c.onHeroMuted).lineLimit(1) }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      HStack(spacing: 4) {
        Image(systemName: "pin").font(.system(size: 11, weight: .bold))
        Text(theme.strings["home.pinned"]).font(TypeScale.label)
      }
      .foregroundStyle(c.onHero)
      .padding(.horizontal, Spacing.sm)
      .padding(.vertical, 4)
      .background(c.onHeroPanel, in: Capsule())
      .frame(maxHeight: .infinity, alignment: .top)
    }
    .fixedSize(horizontal: false, vertical: true)
  }

  private var arrived: some View {
    HStack(spacing: Spacing.sm) {
      Image(systemName: "party.popper").font(.system(size: 20, weight: .semibold))
      Text(theme.strings["countdown.arrived"]).font(TypeScale.title)
    }
    .foregroundStyle(theme.colors.onHero)
    .padding(.top, Spacing.lg)
  }

  /// The number above its label (side by side, the label wrapped in Arabic on v1), then the tiles.
  private func count(_ r: Remaining) -> some View {
    let c = theme.colors
    let passed = r.status != .upcoming
    let label = "\(unitWord(.day, r.days, theme.lang)) \(passed ? theme.strings["countdown.since"] : theme.strings["countdown.toGo"])"
    return VStack(alignment: .leading, spacing: 0) {
      Text(formatNumber(r.days, theme.lang))
        .font(TypeScale.hero)
        .foregroundStyle(c.onHero)
        .minimumScaleFactor(0.5)
        .lineLimit(1)
        .contentTransition(.numericText(countsDown: !passed))
        .animation(.snappy(duration: 0.26), value: r.days)
      Text(label).font(TypeScale.subtitle).foregroundStyle(c.onHeroMuted)
      HStack(spacing: Spacing.sm) {
        TimeTile(value: r.hours, unit: .hour)
        TimeTile(value: r.minutes, unit: .minute)
        if model.settings.showSeconds { TimeTile(value: r.seconds, unit: .second) }
      }
      .padding(.top, Spacing.md)
    }
    .padding(.top, Spacing.md)
  }

  private var progressBlock: some View {
    let pct = progress(countdown.target, startAt: countdown.createdAt, now: clock.now)
    let percent = formatPercent(Int((pct * 100).rounded(.down)), theme.lang)
    return VStack(alignment: .leading, spacing: Spacing.xs) {
      ProgressLine(value: pct, color: theme.colors.onHero, track: theme.colors.onHeroPanel)
      Text(theme.t("countdown.progress", ("percent", percent))).font(TypeScale.caption).foregroundStyle(theme.colors.onHeroMuted)
    }
    .padding(.top, Spacing.lg)
  }
}

/// "The wait in numbers": the same wait on the calendar, in weeks and in hours (rows that would
/// read "0" are left out).
struct StatsCard: View {
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  let countdown: Countdown

  var body: some View {
    let r = remaining(countdown.target, now: clock.now)
    let palette = CountdownPalette.of(countdown.color)
    let accent = palette.accent(theme.isDark)
    let soft = palette.soft(theme.isDark)
    let span = spanPhrase(countdown.target, r, theme.lang)
    let weeks = weeksPhrase(r, theme.lang)
    Card(spacing: Spacing.md) {
      Text(theme.strings["home.details"]).font(TypeScale.label).foregroundStyle(theme.colors.textMuted)
      if !span.isEmpty { stat("calendar", theme.strings["home.statSpan"], span, accent, soft) }
      if !weeks.isEmpty { stat("calendar.badge.clock", theme.strings["home.statWeeks"], weeks, accent, soft) }
      stat("clock", theme.strings["home.statHours"], totalHours(r, theme.lang), accent, soft)
    }
  }

  private func stat(_ symbol: String, _ label: String, _ value: String, _ accent: Color, _ soft: Color) -> some View {
    HStack(spacing: Spacing.md) {
      IconBadge(symbol: symbol, tint: accent, background: soft, size: 36, iconSize: 16)
      VStack(alignment: .leading, spacing: 0) {
        Text(label).font(TypeScale.caption).foregroundStyle(theme.colors.textMuted)
        Text(value).font(TypeScale.bodyStrong).foregroundStyle(theme.colors.text)
      }
      .frame(maxWidth: .infinity, alignment: .leading)
    }
    .accessibilityElement(children: .combine)
  }
}

/// A countdown in the Home list: icon, name, date, a thin progress line, the headline number and a
/// pin button that moves it to the hero and the widget. Tapping the row edits it.
struct CountdownRowView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  let countdown: Countdown

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    let now = clock.now
    let r = remaining(countdown.target, now: now)
    let palette = CountdownPalette.of(countdown.color)
    let accent = palette.accent(theme.isDark)
    let soft = palette.soft(theme.isDark)
    let title = countdownTitle(countdown, theme.lang)
    let subtitle = countdownSubtitle(countdown, s, use24h: model.settings.use24h, short: true)
    let h = headline(r)
    let passed = r.status != .upcoming

    HStack(spacing: Spacing.md) {
      Button {
        model.openEditor(countdown.id)
      } label: {
        HStack(spacing: Spacing.md) {
          IconBadge(symbol: countdown.icon.symbol, tint: accent, background: soft, size: 44, iconSize: 20)
          VStack(alignment: .leading, spacing: 2) {
            Text(title).font(TypeScale.bodyStrong).foregroundStyle(c.text).lineLimit(1)
            if !subtitle.isEmpty { Text(subtitle).font(TypeScale.caption).foregroundStyle(c.textMuted).lineLimit(1) }
            if !passed {
              ProgressLine(value: progress(countdown.target, startAt: countdown.createdAt, now: now), color: accent, track: soft, height: 4)
                .padding(.top, Spacing.sm)
            }
          }
          .frame(maxWidth: .infinity, alignment: .leading)
          VStack(spacing: 0) {
            if r.status == .arrived {
              Text(s["countdown.today"]).font(TypeScale.bodyStrong).foregroundStyle(accent)
            } else if passed && !model.settings.countUp {
              Text(s["countdown.finished"]).font(TypeScale.caption).foregroundStyle(c.textMuted)
            } else {
              Text(formatNumber(h.value, theme.lang)).font(.system(size: 24, weight: .bold).monospacedDigit()).foregroundStyle(passed ? c.textMuted : accent)
              Text(unitWord(h.unit, h.value, theme.lang)).font(TypeScale.caption).foregroundStyle(c.textMuted).lineLimit(1)
            }
          }
          .frame(minWidth: 56)
        }
        .contentShape(Rectangle())
      }
      .buttonStyle(PressScale())
      .accessibilityElement(children: .ignore)
      .accessibilityLabel(Text(verbatim: "\(title). \(relativePhrase(r, s))"))
      .accessibilityHint(theme.t("home.edit", ("name", title)))
      .accessibilityAddTraits(.isButton)

      Button {
        model.updateCountdowns { $0.pin(countdown.id) }
        model.showToast(s["toast.pinned"])
      } label: {
        Image(systemName: "pin")
          .font(.system(size: 16, weight: .medium))
          .foregroundStyle(c.textMuted)
          .frame(width: 40, height: TOUCH_TARGET)
          .contentShape(Rectangle())
      }
      .buttonStyle(.plain)
      .accessibilityLabel(theme.t("home.pin", ("name", title)))
    }
    .padding(Spacing.md)
    .background(c.card, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
    .overlay {
      if theme.isDark { RoundedRectangle(cornerRadius: Radius.lg, style: .continuous).strokeBorder(c.border, lineWidth: 1) }
    }
    .shadow(color: theme.isDark ? .clear : c.cardShadow, radius: 6, y: 2)
  }
}

/// Nothing to count down to: the emblem, a line of encouragement and the add button.
struct EmptyStateView: View {
  @EnvironmentObject private var model: AppModel
  @Environment(\.theme) private var theme

  var body: some View {
    VStack(spacing: Spacing.md) {
      AppLogo(size: 96, tile: true)
        .padding(Spacing.lg)
        .background(theme.colors.surfaceAlt, in: RoundedRectangle(cornerRadius: Radius.xl, style: .continuous))
        .padding(.bottom, Spacing.sm)
      Text(theme.strings["home.emptyTitle"]).font(TypeScale.title).foregroundStyle(theme.colors.text)
      Text(theme.strings["home.emptyBody"]).font(TypeScale.body).foregroundStyle(theme.colors.textMuted)
      Button {
        model.openEditor(nil)
      } label: {
        HStack(spacing: Spacing.sm) {
          Image(systemName: "plus").font(.system(size: 17, weight: .semibold))
          Text(theme.strings["home.add"]).font(TypeScale.bodyStrong)
        }
        .foregroundStyle(theme.colors.onPrimary)
        .padding(.horizontal, Spacing.xl)
        .frame(minHeight: TOUCH_TARGET)
        .background(theme.colors.primary, in: Capsule())
      }
      .buttonStyle(PressScale())
      .padding(.top, Spacing.sm)
    }
    .multilineTextAlignment(.center)
    .frame(maxWidth: .infinity)
    .padding(.vertical, Spacing.xxxl)
    .padding(.horizontal, Spacing.lg)
  }
}
