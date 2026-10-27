import DaysCounterCore
import SwiftUI
import UIKit

private let titleMax = 40

/// The add/edit sheet (the Android app's CountdownEditor): a live preview in the chosen gradient,
/// name, date and time wheels, color, icon, "show on the widget", delete. Nothing is saved until
/// Save.
struct EditorView: View {
  @EnvironmentObject private var model: AppModel
  @EnvironmentObject private var clock: Clock
  @Environment(\.theme) private var theme
  @Environment(\.dismiss) private var dismiss
  let countdownId: String?
  @State private var draft: CountdownDraft
  @State private var confirmDelete = false

  init(countdownId: String?, initial: CountdownDraft) {
    self.countdownId = countdownId
    _draft = State(initialValue: initial)
  }

  private var countdown: Countdown? { countdownId.flatMap { id in model.countdowns.countdowns.first { $0.id == id } } }
  private var isPinned: Bool { countdown != nil && countdown?.id == model.countdowns.pinnedId }

  var body: some View {
    let c = theme.colors
    let s = theme.strings
    VStack(spacing: 0) {
      HStack(spacing: Spacing.md) {
        Text(countdown != nil ? s["editor.editTitle"] : s["editor.newTitle"])
          .font(TypeScale.title)
          .foregroundStyle(c.text)
          .frame(maxWidth: .infinity, alignment: .leading)
          .accessibilityAddTraits(.isHeader)
        Button { dismiss() } label: {
          Image(systemName: "xmark")
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(c.text)
            .frame(width: 40, height: 40)
            .background(c.surfaceAlt, in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(s["common.close"])
      }
      .padding(.horizontal, Spacing.lg)
      .padding(.top, Spacing.lg)
      .padding(.bottom, Spacing.sm)

      ScrollView {
        VStack(alignment: .leading, spacing: 0) {
          preview
          SectionLabel(text: s["editor.name"], first: true)
          TextField(s["editor.namePlaceholder"], text: Binding(get: { draft.title }, set: { draft.title = String($0.prefix(titleMax)) }))
            .font(TypeScale.body)
            .foregroundStyle(c.text)
            .padding(.horizontal, Spacing.md)
            .frame(minHeight: TOUCH_TARGET)
            .background(c.surfaceAlt, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).strokeBorder(c.border, lineWidth: 1))
            .submitLabel(.done)
            .accessibilityLabel(s["editor.name"])
          Text(s["editor.nameHint"]).font(TypeScale.caption).foregroundStyle(c.textMuted).padding(.top, Spacing.xs)

          SectionLabel(text: s["editor.date"])
          DateWheels(value: $draft.target)
          SectionLabel(text: s["editor.time"])
          TimeWheels(value: $draft.target, use24h: model.settings.use24h)
          if toInstant(draft.target) <= clock.now { pastNote }

          SectionLabel(text: s["editor.color"])
          colors
          SectionLabel(text: s["editor.icon"])
          icons

          if !isPinned {
            Toggle(isOn: $draft.pin) {
              VStack(alignment: .leading, spacing: 2) {
                Text(s["editor.pin"]).font(TypeScale.bodyStrong).foregroundStyle(c.text)
                Text(s["editor.pinHint"]).font(TypeScale.caption).foregroundStyle(c.textMuted)
              }
            }
            .tint(c.primary)
            .padding(Spacing.md)
            .background(c.surfaceAlt, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .padding(.top, Spacing.xl)
          }

          if countdown != nil {
            DangerButton(label: s["editor.delete"]) { confirmDelete = true }.padding(.top, Spacing.xl)
          }
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.bottom, Spacing.lg)
      }
      .scrollDismissesKeyboard(.interactively)

      Rectangle().fill(c.border).frame(height: 0.5)
      HStack(spacing: Spacing.md) {
        actionButton(s["common.cancel"], background: c.surfaceAlt, color: c.text) { dismiss() }
        actionButton(s["common.save"], background: c.primary, color: c.onPrimary) { save() }
      }
      .padding(.horizontal, Spacing.lg)
      .padding(.vertical, Spacing.md)
    }
    .background(c.surface.ignoresSafeArea())
    .alert(s["editor.deleteTitle"], isPresented: $confirmDelete) {
      Button(s["common.delete"], role: .destructive) { delete() }
      Button(s["common.cancel"], role: .cancel) {}
    } message: {
      Text(s["editor.deleteBody"])
    }
  }

  private var preview: some View {
    let c = theme.colors
    let previewCountdown = Countdown(id: "preview", title: draft.title.trimmingCharacters(in: .whitespacesAndNewlines), target: draft.target, color: draft.color, icon: draft.icon, createdAt: clock.now)
    let subtitle = countdownSubtitle(previewCountdown, theme.strings, use24h: model.settings.use24h)
    return HStack(spacing: Spacing.md) {
      HeroBubble(symbol: draft.icon.symbol, size: 40)
      VStack(alignment: .leading, spacing: 0) {
        Text(countdownTitle(previewCountdown, theme.lang)).font(TypeScale.bodyStrong).foregroundStyle(c.onHero).lineLimit(1)
        if !subtitle.isEmpty { Text(subtitle).font(TypeScale.caption).foregroundStyle(c.onHeroMuted).lineLimit(1) }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      Text(relativePhrase(remaining(draft.target, now: clock.now), theme.strings)).font(TypeScale.bodyStrong).foregroundStyle(c.onHero)
    }
    .padding(Spacing.md)
    .background(GradientFill(palette: CountdownPalette.of(draft.color), circles: [(0.96, 0.1, 0.3, 0.08)]))
    .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
  }

  private var pastNote: some View {
    HStack(alignment: .top, spacing: Spacing.sm) {
      Image(systemName: "info.circle").foregroundStyle(theme.colors.primary)
      Text(theme.strings["editor.pastNote"]).font(TypeScale.caption).foregroundStyle(theme.colors.text)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
    .padding(Spacing.md)
    .background(theme.colors.primarySoft, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    .padding(.top, Spacing.md)
  }

  private var colors: some View {
    LazyVGrid(columns: [GridItem(.adaptive(minimum: 46), spacing: 4)], alignment: .leading, spacing: 4) {
      ForEach(CountdownColor.allCases, id: \.self) { color in
        let active = color == draft.color
        let to = CountdownPalette.of(color).to
        Button { draft.color = color } label: {
          ZStack {
            Circle().strokeBorder(active ? to : .clear, lineWidth: 2.5).frame(width: 46, height: 46)
            Circle().fill(to).frame(width: 34, height: 34)
            if active { Image(systemName: "checkmark").font(.system(size: 15, weight: .heavy)).foregroundStyle(.white) }
          }
          .frame(width: 48, height: 48)
          .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(theme.strings["editor.colors.\(color.rawValue)"])
        .accessibilityAddTraits(active ? [.isSelected] : [])
      }
    }
  }

  private var icons: some View {
    LazyVGrid(columns: [GridItem(.adaptive(minimum: 48), spacing: Spacing.sm)], alignment: .leading, spacing: Spacing.sm) {
      ForEach(CountdownIcon.allCases, id: \.self) { icon in
        let active = icon == draft.icon
        Button { draft.icon = icon } label: {
          Image(systemName: icon.symbol)
            .font(.system(size: 20, weight: .medium))
            .foregroundStyle(active ? .white : theme.colors.textMuted)
            .frame(width: 48, height: 48)
            .background(active ? CountdownPalette.of(draft.color).to : theme.colors.surfaceAlt, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(theme.strings["editor.icons.\(icon.rawValue)"])
        .accessibilityAddTraits(active ? [.isSelected] : [])
      }
    }
  }

  private func actionButton(_ label: String, background: Color, color: Color, action: @escaping () -> Void) -> some View {
    Button(action: action) {
      Text(label)
        .font(TypeScale.bodyStrong)
        .foregroundStyle(color)
        .frame(maxWidth: .infinity, minHeight: TOUCH_TARGET)
        .background(background, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    }
    .buttonStyle(PressScale())
  }

  private func save() {
    let input = draft.input
    if let countdown {
      let pin = draft.pin && !isPinned
      model.updateCountdowns { state in
        let updated = state.update(countdown.id, input)
        return pin ? updated.pin(countdown.id) : updated
      }
      model.showToast(theme.strings["toast.saved"])
    } else {
      let pin = draft.pin
      model.updateCountdowns { $0.add(input, pin: pin, id: newCountdownId(), now: nowMs()) }
      model.showToast(theme.strings["toast.added"])
    }
    dismiss()
  }

  private func delete() {
    guard let countdown else { return }
    model.updateCountdowns { $0.remove(countdown.id, now: nowMs()) }
    model.showToast(theme.strings["toast.deleted"])
    dismiss()
  }
}

/// Side-by-side wheel pickers in SwiftUI share an over-wide touch area unless UIPickerView drops
/// its intrinsic width — the well-known workaround, so each wheel only turns under its own column.
extension UIPickerView {
  override open var intrinsicContentSize: CGSize {
    CGSize(width: UIView.noIntrinsicMetric, height: super.intrinsicContentSize.height)
  }
}

/// One labelled wheel.
private struct Wheel: View {
  @Environment(\.theme) private var theme
  let label: String
  let items: [String]
  @Binding var selection: Int

  var body: some View {
    VStack(spacing: Spacing.xs) {
      Text(label).font(TypeScale.caption).foregroundStyle(theme.colors.textMuted)
      Picker(label, selection: $selection) {
        ForEach(items.indices, id: \.self) { i in
          Text(items[i]).tag(i)
        }
      }
      .pickerStyle(.wheel)
      .frame(height: 160)
      .clipped()
      .accessibilityLabel(label)
    }
    .frame(maxWidth: .infinity)
  }
}

/// Day / month / year wheels. A day past the new month's end is clamped (31 → 30 Sep). Mirrors in
/// Arabic, so the day sits on the right as Arabic dates are read.
struct DateWheels: View {
  @Environment(\.theme) private var theme
  @Binding var value: LocalDateTime

  var body: some View {
    let lang = theme.lang
    let s = theme.strings
    // Day : month : year columns at 0.8 : 1.6 : 1, like v1 (month names are the longest).
    GeometryReader { geo in
      let unit = (geo.size.width - 2 * Spacing.xs) / 3.4
      HStack(spacing: Spacing.xs) {
        Wheel(label: s["editor.day"], items: (1...daysInMonth(value.year, value.month)).map { localizeDigits($0, lang) }, selection: Binding(
          get: { value.day - 1 },
          set: { value.day = $0 + 1 }
        ))
        .frame(width: unit * 0.8)
        Wheel(label: s["editor.month"], items: (1...12).map { monthName($0, lang) }, selection: Binding(
          get: { value.month - 1 },
          set: { set(month: $0 + 1) }
        ))
        .frame(width: unit * 1.6)
        Wheel(label: s["editor.year"], items: (YEAR_MIN...YEAR_MAX).map { localizeDigits($0, lang) }, selection: Binding(
          get: { value.year - YEAR_MIN },
          set: { set(year: YEAR_MIN + $0) }
        ))
        .frame(width: unit)
      }
    }
    .frame(height: 184)
  }

  private func set(year: Int? = nil, month: Int? = nil) {
    var next = value
    if let year { next.year = year }
    if let month { next.month = month }
    next.day = min(next.day, daysInMonth(next.year, next.month))
    value = next
  }
}

/// Hour / minute wheels, 24-hour ("18") or 12-hour ("6 PM") per Settings.
struct TimeWheels: View {
  @Environment(\.theme) private var theme
  @Binding var value: LocalDateTime
  let use24h: Bool

  var body: some View {
    let lang = theme.lang
    let s = theme.strings
    HStack(spacing: Spacing.xs) {
      Wheel(label: s["editor.hour"], items: (0...23).map { h in use24h ? localizeDigits(pad2(h), lang) : localizeDigits("\(hour12(h)) \(dayPeriod(h, lang))", lang) }, selection: $value.hour)
      Wheel(label: s["editor.minute"], items: (0...59).map { localizeDigits(pad2($0), lang) }, selection: $value.minute)
    }
    .padding(.horizontal, Spacing.xxl)
  }
}
