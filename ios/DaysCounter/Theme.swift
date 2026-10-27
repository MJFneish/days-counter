import DaysCounterCore
import SwiftUI

extension Color {
  /// 0xRRGGBB.
  init(hex: UInt32, opacity: Double = 1) {
    self.init(
      .sRGB,
      red: Double((hex >> 16) & 0xFF) / 255,
      green: Double((hex >> 8) & 0xFF) / 255,
      blue: Double(hex & 0xFF) / 255,
      opacity: opacity
    )
  }
}

/// "Dusk" — the same tokens as the Android app's Theme.kt (and v1's colors.ts): an ember-orange
/// primary with a deep violet secondary. Every text-bearing pair is ≥ 4.5:1 in its theme.
struct AppColors {
  let background, surface, surfaceAlt, primary, primaryDark, primarySoft, onPrimary: Color
  let secondary, secondarySoft, text, textMuted, border, card, cardShadow, danger, dangerSoft: Color
  let heroFrom, heroTo, onHero, onHeroMuted, onHeroPanel, scrim, switchTrackOff: Color

  static let light = AppColors(
    background: Color(hex: 0xFFF8F4), surface: .white, surfaceAlt: Color(hex: 0xF8ECE6),
    primary: Color(hex: 0xC2410C), primaryDark: Color(hex: 0x9A3412), primarySoft: Color(hex: 0xFDE7DC), onPrimary: .white,
    secondary: Color(hex: 0x6D28D9), secondarySoft: Color(hex: 0xEDE5FD),
    text: Color(hex: 0x1E1428), textMuted: Color(hex: 0x6A5D72), border: Color(hex: 0xEEDFD6),
    card: .white, cardShadow: Color(hex: 0x782D12, opacity: 0.14), danger: Color(hex: 0xB91C1C), dangerSoft: Color(hex: 0xFDE2E2),
    heroFrom: Color(hex: 0x3B0764), heroTo: Color(hex: 0xC2410C), onHero: .white, onHeroMuted: .white.opacity(0.82),
    onHeroPanel: .white.opacity(0.16), scrim: Color(hex: 0x140A1E, opacity: 0.45), switchTrackOff: Color(hex: 0xD9C9C0)
  )

  static let dark = AppColors(
    background: Color(hex: 0x120B18), surface: Color(hex: 0x1C1323), surfaceAlt: Color(hex: 0x271B30),
    primary: Color(hex: 0xFF8A50), primaryDark: Color(hex: 0xF97316), primarySoft: Color(hex: 0x3A1E14), onPrimary: Color(hex: 0x2A0D00),
    secondary: Color(hex: 0xB794F6), secondarySoft: Color(hex: 0x2A1E45),
    text: Color(hex: 0xFBF3F8), textMuted: Color(hex: 0xB6A8BE), border: Color(hex: 0x33243D),
    card: Color(hex: 0x1E1526), cardShadow: .black.opacity(0.5), danger: Color(hex: 0xF87171), dangerSoft: Color(hex: 0x3B1717),
    heroFrom: Color(hex: 0x2E0752), heroTo: Color(hex: 0xB23A0C), onHero: .white, onHeroMuted: .white.opacity(0.8),
    onHeroPanel: .white.opacity(0.14), scrim: .black.opacity(0.6), switchTrackOff: Color(hex: 0x3E2F49)
  )
}

/// Fixed brand colors for the icon, the launch screen and the loading screen (same in both themes).
enum Brand {
  static let night = Color(hex: 0x170B26)
  static let violet = Color(hex: 0x6D28D9)
  static let ember = Color(hex: 0xF97316)
  static let gold = Color(hex: 0xFFC56E)
  static let goldLight = Color(hex: 0xFFE0A3)
  static let goldDeep = Color(hex: 0xFF9A4D)
  static let light = Color(hex: 0xFFF4EC)
  static let tileFrom = Color(hex: 0x3B0764)
  static let tileTo = Color(hex: 0xC2410C)
}

/// The Android app's type scale in points (system fonts: proper Arabic shaping for free), scaled by
/// Dynamic Type. Ticking numbers use monospaced digits so a changing digit never shifts its
/// neighbours.
enum TypeScale {
  private static func font(_ size: CGFloat, _ weight: Font.Weight, relativeTo style: UIFont.TextStyle = .body) -> Font {
    .system(size: UIFontMetrics(forTextStyle: style).scaledValue(for: size), weight: weight)
  }

  static var hero: Font { font(64, .heavy, relativeTo: .largeTitle).monospacedDigit() }
  static var displayMedium: Font { font(28, .bold, relativeTo: .largeTitle) }
  static var digits: Font { font(28, .bold, relativeTo: .title1).monospacedDigit() }
  static var title: Font { font(20, .bold, relativeTo: .title2) }
  static var subtitle: Font { font(17, .semibold, relativeTo: .headline) }
  static var body: Font { font(16, .regular) }
  static var bodyStrong: Font { font(16, .semibold) }
  static var caption: Font { font(13, .medium, relativeTo: .caption1) }
  static var label: Font { font(12, .bold, relativeTo: .caption2) }
}

/// 4/8 pt rhythm (the Android app's Spacing).
enum Spacing {
  static let xs: CGFloat = 4
  static let sm: CGFloat = 8
  static let md: CGFloat = 12
  static let lg: CGFloat = 16
  static let xl: CGFloat = 24
  static let xxl: CGFloat = 32
  static let xxxl: CGFloat = 48
}

enum Radius {
  static let sm: CGFloat = 8
  static let md: CGFloat = 14
  static let lg: CGFloat = 20
  static let xl: CGFloat = 28
}

/// Minimum touch target (iOS 44 pt).
let TOUCH_TARGET: CGFloat = 44

/// Per-countdown colors — the same values as the Android app's CountdownPalette.kt.
struct CountdownPalette {
  let from, to, light, dark, softLight, softDark: Color

  func accent(_ isDark: Bool) -> Color { isDark ? dark : light }
  func soft(_ isDark: Bool) -> Color { isDark ? softDark : softLight }

  static func of(_ color: CountdownColor) -> CountdownPalette {
    switch color {
    case .ember: return CountdownPalette(hex: 0x7C2D12, 0xC2410C, 0xC2410C, 0xFF8A50, 0xFDE7DC, 0x3A1E14)
    case .violet: return CountdownPalette(hex: 0x3B0764, 0x7C3AED, 0x6D28D9, 0xB794F6, 0xEDE5FD, 0x2A1E45)
    case .ocean: return CountdownPalette(hex: 0x0C4A6E, 0x0369A1, 0x0369A1, 0x7DD3FC, 0xE0F2FE, 0x0F2A3A)
    case .teal: return CountdownPalette(hex: 0x134E4A, 0x0F766E, 0x0F766E, 0x5EEAD4, 0xDDF5F1, 0x0F2E2B)
    case .rose: return CountdownPalette(hex: 0x881337, 0xE11D48, 0xBE123C, 0xFDA4AF, 0xFFE4E8, 0x3B1520)
    case .forest: return CountdownPalette(hex: 0x14532D, 0x15803D, 0x15803D, 0x86EFAC, 0xDCF5E4, 0x13301E)
    case .indigo: return CountdownPalette(hex: 0x312E81, 0x4F46E5, 0x4338CA, 0xA5B4FC, 0xE4E6FD, 0x1E1F45)
    case .gold: return CountdownPalette(hex: 0x78350F, 0xB45309, 0xB45309, 0xFCD34D, 0xFDF0D5, 0x3A2A10)
    case .graphite: return CountdownPalette(hex: 0x1E293B, 0x475569, 0x475569, 0xCBD5E1, 0xE8ECF1, 0x232B36)
    }
  }

  private init(hex from: UInt32, _ to: UInt32, _ light: UInt32, _ dark: UInt32, _ softLight: UInt32, _ softDark: UInt32) {
    self.from = Color(hex: from)
    self.to = Color(hex: to)
    self.light = Color(hex: light)
    self.dark = Color(hex: dark)
    self.softLight = Color(hex: softLight)
    self.softDark = Color(hex: softDark)
  }
}

/// SF Symbols for the countdown icons (the Android app draws the same choices with Lucide).
extension CountdownIcon {
  var symbol: String {
    switch self {
    case .calendar: return "calendar"
    case .star: return "star"
    case .heart: return "heart"
    case .cake: return "birthday.cake"
    case .plane: return "airplane"
    case .graduation: return "graduationcap"
    case .briefcase: return "briefcase"
    case .home: return "house"
    case .baby: return "stroller"
    case .trophy: return "trophy"
    case .gift: return "gift"
    case .flag: return "flag"
    }
  }
}

/// Everything a view needs to draw itself in the user's choices.
struct AppTheme {
  let colors: AppColors
  let isDark: Bool
  let strings: Strings

  var lang: Lang { strings.lang }
  var isRTL: Bool { lang == .ar }

  func t(_ key: String, _ args: (String, Any)...) -> String {
    var text = strings[key]
    for (name, value) in args { text = text.replacingOccurrences(of: "{{\(name)}}", with: "\(value)") }
    return text
  }

  static let preview = AppTheme(colors: .light, isDark: false, strings: Strings.of(.en))
}

private struct ThemeKey: EnvironmentKey {
  static let defaultValue = AppTheme.preview
}

extension EnvironmentValues {
  var theme: AppTheme {
    get { self[ThemeKey.self] }
    set { self[ThemeKey.self] = newValue }
  }
}

/// Applies the user's language (the layout direction flips instantly — the app's language is its
/// own setting, not the phone's) and light/dark choice.
struct Themed: ViewModifier {
  @EnvironmentObject var model: AppModel
  @Environment(\.colorScheme) private var systemScheme

  func body(content: Content) -> some View {
    let settings = model.settings
    let isDark = settings.themeMode == .system ? systemScheme == .dark : settings.themeMode == .dark
    let theme = AppTheme(colors: isDark ? .dark : .light, isDark: isDark, strings: Strings.of(settings.language))
    return content
      .environment(\.theme, theme)
      .environment(\.layoutDirection, settings.language == .ar ? .rightToLeft : .leftToRight)
      .environment(\.locale, Locale(identifier: settings.language.code))
      .preferredColorScheme(settings.themeMode == .system ? nil : (isDark ? .dark : .light))
      .tint(theme.colors.primary)
  }
}
