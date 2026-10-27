import DaysCounterCore
import SwiftUI

/// Settings, in the Android app's order: language · appearance (follow-system switch, light/dark
/// tiles otherwise) · countdown options · home-screen widget (from the widget phase on) · data ·
/// about.
struct SettingsView: View {
  @EnvironmentObject private var model: AppModel
  @Environment(\.theme) private var theme
  @State private var confirmReset = false

  private var c: AppColors { theme.colors }
  private var s: Strings { theme.strings }

  private func binding<T>(_ keyPath: WritableKeyPath<Settings, T>) -> Binding<T> {
    Binding(get: { model.settings[keyPath: keyPath] }, set: { value in model.updateSettings { $0[keyPath: keyPath] = value } })
  }

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 0) {
        Text(s["settings.title"])
          .font(TypeScale.displayMedium)
          .foregroundStyle(c.text)
          .accessibilityAddTraits(.isHeader)

        SectionLabel(text: s["settings.language"], first: true)
        SegmentedControl(options: [(value: Lang.en, label: "English"), (value: Lang.ar, label: "العربية")], selection: binding(\.language))

        SectionLabel(text: s["settings.appearance"])
        Card {
          let followSystem = model.settings.themeMode == .system
          SwitchRow(symbol: "circle.lefthalf.filled", title: s["settings.followSystem"], hint: s["settings.followSystemHint"], isOn: Binding(
            get: { followSystem },
            set: { on in
              let isDark = theme.isDark
              withAnimation(.easeInOut(duration: 0.2)) { model.updateSettings { $0.themeMode = on ? .system : (isDark ? .dark : .light) } }
            }
          ))
          if !followSystem {
            HStack(spacing: Spacing.sm) {
              themeTile(.light, "settings.light", "sun.max")
              themeTile(.dark, "settings.dark", "moon")
            }
            .padding(.top, Spacing.md)
            .transition(.opacity.combined(with: .move(edge: .top)))
          }
        }

        SectionLabel(text: s["settings.countdown"])
        Card {
          SwitchRow(symbol: "timer", title: s["settings.showSeconds"], hint: s["settings.showSecondsHint"], isOn: binding(\.showSeconds))
          HairlineDivider()
          SwitchRow(symbol: "clock.arrow.circlepath", title: s["settings.countUp"], hint: s["settings.countUpHint"], isOn: binding(\.countUp))
          HairlineDivider()
          SwitchRow(symbol: "clock", title: s["settings.clock24"], hint: s["settings.clock24Hint"], isOn: binding(\.use24h))
        }

        if !model.countdowns.countdowns.isEmpty {
          SectionLabel(text: s["settings.data"])
          DangerButton(label: s["settings.reset"]) { confirmReset = true }
        }

        SectionLabel(text: s["settings.about"])
        Card {
          HStack(spacing: Spacing.md) {
            AppLogo(size: 52, tile: true)
            VStack(alignment: .leading, spacing: 2) {
              Text(s["app.name"]).font(TypeScale.subtitle).foregroundStyle(c.text)
              Text(theme.t("settings.version", ("version", localizeDigits(appVersion, theme.lang))))
                .font(TypeScale.caption)
                .foregroundStyle(c.textMuted)
            }
          }
          .padding(.bottom, Spacing.md)
          Text(s["settings.aboutBody"]).font(TypeScale.body).foregroundStyle(c.text).fixedSize(horizontal: false, vertical: true)
          Text(s["settings.madeBy"]).font(TypeScale.caption).foregroundStyle(c.textMuted).padding(.top, Spacing.md)
        }
      }
      .padding(.horizontal, Spacing.lg)
      .padding(.top, Spacing.md)
      .padding(.bottom, Spacing.xxxl)
    }
    .background(c.background.ignoresSafeArea())
    .alert(s["settings.resetTitle"], isPresented: $confirmReset) {
      Button(s["common.delete"], role: .destructive) {
        model.updateCountdowns { $0.removeAll() }
        model.showToast(s["toast.reset"])
      }
      Button(s["common.cancel"], role: .cancel) {}
    } message: {
      Text(s["settings.resetBody"])
    }
  }

  /// The full version ("2.0.0-alpha.4") — DCVersionName in Info.plist, written by `yarn bump`.
  private var appVersion: String {
    Bundle.main.object(forInfoDictionaryKey: "DCVersionName") as? String
      ?? Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
  }

  private func themeTile(_ mode: ThemeMode, _ key: String, _ symbol: String) -> some View {
    let active = model.settings.themeMode == mode
    return Button {
      model.updateSettings { $0.themeMode = mode }
    } label: {
      VStack(spacing: Spacing.xs) {
        Image(systemName: symbol).font(.system(size: 20, weight: .medium))
        Text(s[key]).font(TypeScale.bodyStrong)
      }
      .foregroundStyle(active ? c.primary : c.text)
      .frame(maxWidth: .infinity, minHeight: 72)
      .background(active ? c.primarySoft : .clear, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
      .overlay(RoundedRectangle(cornerRadius: Radius.md, style: .continuous).strokeBorder(active ? c.primary : c.border, lineWidth: 1.5))
      .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
    .accessibilityLabel(s[key])
    .accessibilityAddTraits(active ? [.isSelected] : [])
  }
}
