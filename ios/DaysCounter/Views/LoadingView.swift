import SwiftUI

/// Animated launch screen (the Android app's LoadingScreen and v1's): drifting violet/ember glows,
/// twinkling sparks, the emblem with its gold countdown arc spinning around the calendar page, the
/// app name and an indeterminate bar. Its first frame is the launch screen (night background, the
/// 120 pt emblem in the middle), so the hand-off is invisible. Reduce Motion keeps it still.
struct LoadingView: View {
  @Environment(\.theme) private var theme
  @Environment(\.accessibilityReduceMotion) private var reduceMotion
  @State private var started = Date()
  @State private var titleShown = false

  private static let logoSize: CGFloat = 120
  private static let barWidth: CGFloat = 160

  private struct Spark {
    let x: CGFloat
    let y: CGFloat
    let radius: CGFloat
    let delay: Double
  }

  /// Deterministic sparks (fractions of the screen, radius pt, delay s), the same every launch.
  private static let sparks: [Spark] = [
    Spark(x: 0.12, y: 0.14, radius: 2.2, delay: 0), Spark(x: 0.82, y: 0.1, radius: 1.6, delay: 0.4),
    Spark(x: 0.68, y: 0.22, radius: 2.4, delay: 0.9), Spark(x: 0.24, y: 0.3, radius: 1.4, delay: 1.3),
    Spark(x: 0.9, y: 0.36, radius: 1.8, delay: 0.2), Spark(x: 0.08, y: 0.52, radius: 2, delay: 1.1),
    Spark(x: 0.86, y: 0.62, radius: 1.5, delay: 0.6), Spark(x: 0.18, y: 0.74, radius: 2.2, delay: 1.5),
    Spark(x: 0.76, y: 0.8, radius: 1.8, delay: 0.3), Spark(x: 0.44, y: 0.88, radius: 1.4, delay: 1.0),
  ]

  var body: some View {
    TimelineView(.animation(paused: reduceMotion)) { timeline in
      GeometryReader { geo in
        scene(t: reduceMotion ? 0 : timeline.date.timeIntervalSince(started), size: geo.size)
      }
    }
    .ignoresSafeArea()
    .environment(\.layoutDirection, .leftToRight)
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(theme.strings["loading.label"])
    .onAppear {
      withAnimation(.easeOut(duration: 0.5).delay(0.2)) { titleShown = true }
    }
  }

  private func scene(t: Double, size: CGSize) -> some View {
    let w = size.width
    let h = size.height
    return ZStack {
      Brand.night
      glow(size: w * 1.1, x: -w * 0.4, y: -w * 0.35, color: Brand.violet, drift: 30, p: wave(t, 10.4))
      glow(size: w * 0.95, x: w * 0.3, y: h * 0.6, color: Brand.ember, drift: -26, p: wave(t, 12.8))
      ForEach(Self.sparks.indices, id: \.self) { i in
        spark(Self.sparks[i], t: t, w: w, h: h)
      }
      emblem(t: t).position(x: w / 2, y: h / 2)
      title.frame(width: w).position(x: w / 2, y: h / 2 + Self.logoSize / 2 + Spacing.xl + 30)
      bar(t: t).position(x: w / 2, y: h - 72)
    }
  }

  /// 0…1…0 smoothly over `period` seconds.
  private func wave(_ t: Double, _ period: Double) -> CGFloat {
    CGFloat((1 - cos(2 * Double.pi * t / period)) / 2)
  }

  private func glow(size: CGFloat, x: CGFloat, y: CGFloat, color: Color, drift: CGFloat, p: CGFloat) -> some View {
    let center = CGPoint(x: x + size / 2 + p * drift, y: y + size / 2 - p * drift * 0.6)
    return Circle()
      .fill(RadialGradient(colors: [color.opacity(0.5), color.opacity(0)], center: .center, startRadius: 0, endRadius: size / 2))
      .frame(width: size, height: size)
      .scaleEffect(1 + 0.12 * p)
      .position(center)
  }

  /// Twinkle: 0.3 → 1 over 1.1 s, back to 0.2 over 1.3 s, offset by the spark's delay.
  private func spark(_ s: Spark, t: Double, w: CGFloat, h: CGFloat) -> some View {
    var phase = (t - s.delay).truncatingRemainder(dividingBy: 2.4)
    if phase < 0 { phase += 2.4 }
    let alpha: Double = reduceMotion ? 0.3 : (phase < 1.1 ? 0.3 + 0.7 * phase / 1.1 : 1 - 0.8 * (phase - 1.1) / 1.3)
    return Circle()
      .fill(Brand.light.opacity(alpha))
      .frame(width: s.radius * 2, height: s.radius * 2)
      .position(x: s.x * w, y: s.y * h)
  }

  private func emblem(t: Double) -> some View {
    ZStack {
      AppLogo(size: Self.logoSize, arc: false).scaleEffect(1 + 0.04 * wave(t, 3.2))
      LogoArc(size: Self.logoSize).rotationEffect(.degrees(spin(t)))
    }
  }

  private var title: some View {
    VStack(spacing: Spacing.xs) {
      Text(theme.strings["app.name"]).font(TypeScale.displayMedium).foregroundStyle(.white)
      Text(theme.strings["loading.tagline"]).font(TypeScale.body).foregroundStyle(Brand.light.opacity(0.78))
    }
    .multilineTextAlignment(.center)
    .padding(.horizontal, Spacing.xl)
    .opacity(titleShown ? 1 : 0)
    .offset(y: titleShown ? 0 : 16)
  }

  private func bar(t: Double) -> some View {
    let x: CGFloat = reduceMotion ? 0 : (CGFloat(-0.4) + 1.4 * barPhase(t)) * Self.barWidth
    return Capsule()
      .fill(Brand.light.opacity(0.16))
      .frame(width: Self.barWidth, height: 4)
      .overlay(alignment: .leading) {
        Capsule().fill(Brand.gold).frame(width: Self.barWidth * 0.4, height: 4).offset(x: x)
      }
      .clipShape(Capsule())
  }

  /// One turn every 1.8 s, easing in and out (v1's cubic in-out).
  private func spin(_ t: Double) -> Double {
    let x = t.truncatingRemainder(dividingBy: 1.8) / 1.8
    let eased = x < 0.5 ? 4 * x * x * x : 1 - pow(-2 * x + 2, 3) / 2
    return 360 * eased
  }

  /// 0…1 every 1.3 s, quadratic in-out.
  private func barPhase(_ t: Double) -> CGFloat {
    let x = t.truncatingRemainder(dividingBy: 1.3) / 1.3
    return CGFloat(x < 0.5 ? 2 * x * x : 1 - pow(-2 * x + 2, 2) / 2)
  }
}
