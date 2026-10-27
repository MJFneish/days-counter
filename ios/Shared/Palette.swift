import DaysCounterCore
import SwiftUI

// Shared by the app and the widget extension (both compile ios/Shared/).

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
