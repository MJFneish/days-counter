import DaysCounterCore
import SwiftUI
import WidgetKit

/// The widget's faces, compiled into both the widget extension and the app (whose UI test shows
/// them in a gallery, since a widget can't be screenshotted on CI). The Android widget's layouts:
/// small = number + unit + live H:MM:SS + name; medium = name/date | number/H:MM:SS + progress; the
/// lock-screen families show the count in the system's own style.
struct WidgetFace: View {
  let snapshot: WidgetSnapshot
  /// The moment this face is for (a timeline entry's date).
  let date: Date
  let family: WidgetFamily

  private var lang: Lang { snapshot.lang }
  private var strings: Strings { snapshot.strings }

  var body: some View {
    Group {
      if let countdown = snapshot.countdown {
        content(countdown)
      } else {
        empty
      }
    }
    .environment(\.layoutDirection, lang == .ar ? .rightToLeft : .leftToRight)
  }

  // MARK: content

  @ViewBuilder
  private func content(_ c: Countdown) -> some View {
    let now = Int64((date.timeIntervalSince1970 * 1000).rounded(.down))
    let r = remaining(c.target, now: now)
    let upcoming = r.status == .upcoming
    let showCount = upcoming || snapshot.countUp
    switch family {
    case .accessoryInline:
      Text(showCount ? "\(formatNumber(r.days, lang)) \(unitLabel(r))" : statusText(r))
    case .accessoryCircular:
      VStack(spacing: 0) {
        Text(showCount ? formatNumber(r.days, lang) : "✓").font(.system(size: 20, weight: .bold)).minimumScaleFactor(0.5)
        Text(unitWord(.day, r.days, lang)).font(.system(size: 10, weight: .medium)).lineLimit(1)
      }
      .widgetAccentable()
    case .accessoryRectangular:
      VStack(alignment: .leading, spacing: 1) {
        Text(snapshot.title).font(.headline).lineLimit(1).widgetAccentable()
        Text(showCount ? "\(formatNumber(r.days, lang)) \(unitLabel(r))" : statusText(r)).font(.body).lineLimit(1)
        if showCount { timer(r).font(.body.monospacedDigit()) }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
    case .systemMedium, .systemLarge, .systemExtraLarge:
      medium(c, r, showCount: showCount, now: now)
    default:
      small(r, showCount: showCount)
    }
  }

  private func small(_ r: Remaining, showCount: Bool) -> some View {
    VStack(alignment: .leading, spacing: 2) {
      Text(snapshot.title).font(.system(size: 14, weight: .bold)).lineLimit(2).foregroundStyle(.white)
      Spacer(minLength: 0)
      if showCount {
        Text(formatNumber(r.days, lang)).font(.system(size: 44, weight: .heavy).monospacedDigit()).minimumScaleFactor(0.4).lineLimit(1).foregroundStyle(.white)
        Text(r.status == .arrived ? strings["countdown.arrived"] : unitLabel(r)).font(.system(size: 12, weight: .medium)).lineLimit(2).foregroundStyle(.white.opacity(0.82))
        timer(r).font(.system(size: 17, weight: .bold).monospacedDigit()).foregroundStyle(.white)
      } else {
        Text(statusText(r)).font(.system(size: 15, weight: .bold)).foregroundStyle(.white)
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
  }

  private func medium(_ c: Countdown, _ r: Remaining, showCount: Bool, now: Int64) -> some View {
    VStack(alignment: .leading, spacing: 2) {
      Text(snapshot.title).font(.system(size: 16, weight: .bold)).lineLimit(1).foregroundStyle(.white)
      if !snapshot.detail.isEmpty {
        Text(snapshot.detail).font(.system(size: 12)).lineLimit(1).foregroundStyle(.white.opacity(0.82))
      }
      Spacer(minLength: 0)
      if showCount {
        HStack(alignment: .bottom, spacing: 10) {
          Text(formatNumber(r.days, lang)).font(.system(size: 48, weight: .heavy).monospacedDigit()).minimumScaleFactor(0.5).lineLimit(1)
          VStack(alignment: .leading, spacing: 0) {
            Text(unitLabel(r)).font(.system(size: 13, weight: .medium)).lineLimit(2).foregroundStyle(.white.opacity(0.82))
            timer(r).font(.system(size: 22, weight: .bold).monospacedDigit())
          }
        }
        .foregroundStyle(.white)
      } else {
        Text(statusText(r)).font(.system(size: 17, weight: .bold)).foregroundStyle(.white)
      }
      if r.status == .upcoming {
        let p = progress(c.target, startAt: c.createdAt, now: now)
        GeometryReader { geo in
          ZStack(alignment: .leading) {
            Capsule().fill(.white.opacity(0.2))
            Capsule().fill(.white).frame(width: max(geo.size.width * 0.02, geo.size.width * CGFloat(p)))
          }
        }
        .frame(height: 5)
        .padding(.top, 8)
        Text(strings.t("countdown.progress", ("percent", formatPercent(Int((p * 100).rounded(.down)), lang))))
          .font(.system(size: 11)).foregroundStyle(.white.opacity(0.82)).lineLimit(1)
      } else if r.status == .arrived && showCount {
        // "The day is here!" under the count — unless the status line already says it.
        Text(strings["countdown.arrived"]).font(.system(size: 12, weight: .semibold)).foregroundStyle(.white.opacity(0.9))
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
  }

  private var empty: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(strings["app.name"]).font(.system(size: 14, weight: .bold))
      Text(strings["home.emptyTitle"]).font(.system(size: 13))
    }
    .foregroundStyle(family.isAccessory ? Color.primary : Color.white)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
  }

  // MARK: pieces

  /// "days to go" / "days since".
  private func unitLabel(_ r: Remaining) -> String {
    "\(unitWord(.day, r.days, lang)) \(r.status == .upcoming ? strings["countdown.toGo"] : strings["countdown.since"])"
  }

  private func statusText(_ r: Remaining) -> String {
    r.status == .arrived ? strings["countdown.arrived"] : strings["countdown.doneNoCountUp"]
  }

  /// The live H:MM:SS: down to the next day change, or up from the last one after the date. It ticks
  /// by itself (WidgetKit renders it), like the Android widget's Chronometer.
  private func timer(_ r: Remaining) -> Text {
    let next = Date(timeIntervalSince1970: TimeInterval(r.nextChangeAt) / 1000)
    let last = Date(timeIntervalSince1970: TimeInterval(r.boundaryAt) / 1000)
    if r.status == .upcoming {
      return Text(timerInterval: date...max(date, next), countsDown: true)
    }
    return Text(timerInterval: last...next, countsDown: false)
  }
}

extension WidgetFamily {
  var isAccessory: Bool {
    switch self {
    case .accessoryCircular, .accessoryRectangular, .accessoryInline: return true
    default: return false
    }
  }
}

/// The widget's background: the pinned countdown's gradient (the Android widget's widget_bg_*).
struct WidgetBackground: View {
  let snapshot: WidgetSnapshot

  var body: some View {
    let palette = CountdownPalette.of(snapshot.countdown?.color ?? .ember)
    LinearGradient(colors: [palette.from, palette.to], startPoint: .topLeading, endPoint: .bottomTrailing)
      .environment(\.layoutDirection, .leftToRight)
  }
}
