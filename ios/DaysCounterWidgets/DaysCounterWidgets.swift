import DaysCounterCore
import SwiftUI
import WidgetKit

/// The pinned countdown on the home screen and the lock screen. It reads the App Group store the
/// app writes (SharedStore), computes the count itself and gets a timeline entry at each day change;
/// the H:MM:SS in between is a self-updating timer. The app reloads it on every change.
struct CountdownEntry: TimelineEntry {
  let date: Date
  let snapshot: WidgetSnapshot
}

struct CountdownProvider: TimelineProvider {
  private func snapshotNow() -> WidgetSnapshot {
    WidgetSnapshot(settings: SharedStore.loadSettings(), state: SharedStore.loadCountdowns())
  }

  func placeholder(in context: Context) -> CountdownEntry {
    let sample = CountdownsState().seededIfNeeded(id: "sample", now: nowMs())
    return CountdownEntry(date: Date(), snapshot: WidgetSnapshot(settings: SharedStore.deviceSettings(), state: sample))
  }

  func getSnapshot(in context: Context, completion: @escaping (CountdownEntry) -> Void) {
    let snapshot = snapshotNow()
    completion(snapshot.countdown == nil && context.isPreview ? placeholder(in: context) : CountdownEntry(date: Date(), snapshot: snapshot))
  }

  func getTimeline(in context: Context, completion: @escaping (Timeline<CountdownEntry>) -> Void) {
    let snapshot = snapshotNow()
    let now = Date()
    var entries = [CountdownEntry(date: now, snapshot: snapshot)]
    if let countdown = snapshot.countdown {
      for change in WidgetTimeline.changes(countdown.target, after: nowMs(), count: 30) {
        let at = change + WidgetTimeline.entryDelayMs
        entries.append(CountdownEntry(date: Date(timeIntervalSince1970: TimeInterval(at) / 1000), snapshot: snapshot))
      }
    }
    completion(Timeline(entries: entries, policy: .atEnd))
  }
}

struct CountdownWidgetView: View {
  @Environment(\.widgetFamily) private var family
  let entry: CountdownEntry

  var body: some View {
    WidgetFace(snapshot: entry.snapshot, date: entry.date, family: family)
      .containerBackground(for: .widget) {
        if family.isAccessory {
          Color.clear
        } else {
          WidgetBackground(snapshot: entry.snapshot)
        }
      }
  }
}

struct CountdownWidget: Widget {
  var body: some WidgetConfiguration {
    StaticConfiguration(kind: "DaysCounterWidget", provider: CountdownProvider()) { entry in
      CountdownWidgetView(entry: entry)
    }
    .configurationDisplayName(Strings.of(SharedStore.deviceSettings().language)["app.name"])
    .description(Strings.of(SharedStore.deviceSettings().language)["settings.widgetShows"])
    .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular, .accessoryInline])
  }
}

@main
struct DaysCounterWidgets: WidgetBundle {
  var body: some Widget {
    CountdownWidget()
  }
}
