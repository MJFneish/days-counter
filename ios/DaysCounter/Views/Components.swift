import DaysCounterCore
import SwiftUI

/// A rounded card on the surface colour: a soft shadow in light mode, a hairline border in dark.
struct Card<Content: View>: View {
  @Environment(\.theme) private var theme
  var spacing: CGFloat = 0
  @ViewBuilder var content: Content

  var body: some View {
    VStack(alignment: .leading, spacing: spacing) { content }
      .padding(Spacing.lg)
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(theme.colors.card, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
      .overlay {
        if theme.isDark {
          RoundedRectangle(cornerRadius: Radius.lg, style: .continuous).strokeBorder(theme.colors.border, lineWidth: 1)
        }
      }
      .shadow(color: theme.isDark ? .clear : theme.colors.cardShadow, radius: 6, y: 2)
  }
}

/// Uppercase section label (not in Arabic, which has no case).
struct SectionLabel: View {
  @Environment(\.theme) private var theme
  let text: String
  var first = false

  var body: some View {
    Text(theme.isRTL ? text : text.uppercased())
      .font(TypeScale.label)
      .tracking(theme.isRTL ? 0 : 0.6)
      .foregroundStyle(theme.colors.textMuted)
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(.top, first ? Spacing.lg : Spacing.xl)
      .padding(.bottom, Spacing.sm)
      .accessibilityAddTraits(.isHeader)
  }
}

/// Scales to 96 % while pressed (v1's PressableScale).
struct PressScale: ButtonStyle {
  func makeBody(configuration: Configuration) -> some View {
    configuration.label
      .scaleEffect(configuration.isPressed ? 0.96 : 1)
      .animation(.easeOut(duration: configuration.isPressed ? 0.09 : 0.14), value: configuration.isPressed)
  }
}

/// A rounded icon badge (Settings rows, list rows).
struct IconBadge: View {
  let symbol: String
  let tint: Color
  let background: Color
  var size: CGFloat = 40
  var iconSize: CGFloat = 18

  var body: some View {
    Image(systemName: symbol)
      .font(.system(size: iconSize, weight: .medium))
      .foregroundStyle(tint)
      .frame(width: size, height: size)
      .background(background, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
      .accessibilityHidden(true)
  }
}

/// A labelled switch row (icon, title, hint) — the shape every Settings toggle shares.
struct SwitchRow: View {
  @Environment(\.theme) private var theme
  let symbol: String
  let title: String
  let hint: String
  @Binding var isOn: Bool

  var body: some View {
    Toggle(isOn: $isOn) {
      HStack(spacing: Spacing.md) {
        IconBadge(symbol: symbol, tint: theme.colors.primary, background: theme.colors.primarySoft)
        VStack(alignment: .leading, spacing: 2) {
          Text(title).font(TypeScale.bodyStrong).foregroundStyle(theme.colors.text)
          Text(hint).font(TypeScale.caption).foregroundStyle(theme.colors.textMuted)
        }
      }
    }
    .tint(theme.colors.primary)
    .frame(minHeight: TOUCH_TARGET)
  }
}

/// A hairline divider between rows of a card.
struct HairlineDivider: View {
  @Environment(\.theme) private var theme
  var body: some View {
    Rectangle().fill(theme.colors.border).frame(height: 0.5).padding(.vertical, Spacing.md)
  }
}

/// Pill segmented control with a sliding selection indicator; mirrors with the layout direction.
struct SegmentedControl<Value: Hashable>: View {
  @Environment(\.theme) private var theme
  @Namespace private var namespace
  let options: [(value: Value, label: String)]
  @Binding var selection: Value

  var body: some View {
    HStack(spacing: 0) {
      ForEach(options.indices, id: \.self) { index in
        let option = options[index]
        let active = option.value == selection
        Button {
          withAnimation(.easeInOut(duration: 0.22)) { selection = option.value }
        } label: {
          Text(option.label)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(active ? theme.colors.onPrimary : theme.colors.text)
            .lineLimit(1)
            .frame(maxWidth: .infinity, minHeight: TOUCH_TARGET)
            .background {
              if active {
                Capsule().fill(theme.colors.primary).matchedGeometryEffect(id: "selection", in: namespace)
              }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(active ? [.isSelected] : [])
      }
    }
    .padding(4)
    .background(theme.colors.surfaceAlt, in: Capsule())
  }
}

/// Full-width outlined danger button (delete all / delete countdown).
struct DangerButton: View {
  @Environment(\.theme) private var theme
  let label: String
  let action: () -> Void

  var body: some View {
    Button(action: action) {
      HStack(spacing: Spacing.sm) {
        Image(systemName: "trash").font(.system(size: 16, weight: .semibold))
        Text(label).font(TypeScale.bodyStrong)
      }
      .foregroundStyle(theme.colors.danger)
      .frame(maxWidth: .infinity, minHeight: TOUCH_TARGET)
      .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).strokeBorder(theme.colors.danger, lineWidth: 1.5))
      .contentShape(Rectangle())
    }
    .buttonStyle(PressScale())
    .accessibilityLabel(label)
  }
}

/// Brief confirmations ("Countdown added") above the tab bar — v1's ToastHost.
struct ToastOverlay: View {
  @EnvironmentObject private var model: AppModel
  @Environment(\.theme) private var theme
  @State private var shown: String?

  var body: some View {
    VStack {
      Spacer()
      if let shown {
        HStack(spacing: Spacing.sm) {
          Image(systemName: "checkmark.circle.fill").font(.system(size: 16, weight: .semibold))
          Text(shown).font(TypeScale.bodyStrong)
        }
        .foregroundStyle(theme.colors.background)
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.md)
        .background(theme.colors.text, in: Capsule())
        .padding(.bottom, 88)
        .transition(.move(edge: .bottom).combined(with: .opacity))
        .accessibilityAddTraits(.updatesFrequently)
      }
    }
    .frame(maxWidth: .infinity)
    .allowsHitTesting(false)
    .onChange(of: model.toastId) { _, _ in
      guard let text = model.toast else { return }
      withAnimation(.easeOut(duration: 0.22)) { shown = text }
      let id = model.toastId
      DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
        guard model.toastId == id else { return }
        withAnimation(.easeIn(duration: 0.18)) { shown = nil }
      }
    }
  }
}
