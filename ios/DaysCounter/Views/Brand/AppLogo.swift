import SwiftUI

/// The emblem, on a 100×100 grid: a countdown ring three-quarters done (gold arc + knob) around a
/// calendar page with one day marked. Original artwork; the same numbers live in the Android app's
/// AppLogo.kt and android/scripts/generate-icons.js — keep them in sync.
enum Emblem {
  static let ringCenter = CGPoint(x: 50, y: 50)
  static let ringRadius: CGFloat = 34
  static let ringWidth: CGFloat = 7
  static let knob = CGPoint(x: 16, y: 50)
  static let knobRadius: CGFloat = 5.2

  /// The gold arc: 270° clockwise from 12 o'clock to 9 o'clock, as points (y grows downwards, so
  /// increasing angles turn clockwise on screen).
  static var arc: Path {
    var path = Path()
    let steps = 72
    for i in 0...steps {
      let angle: Double = (-90.0 + 270.0 * Double(i) / Double(steps)) * Double.pi / 180
      let p = CGPoint(x: ringCenter.x + ringRadius * CGFloat(Foundation.cos(angle)), y: ringCenter.y + ringRadius * CGFloat(Foundation.sin(angle)))
      if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
    }
    return path
  }

  static func circle(_ center: CGPoint, _ r: CGFloat) -> Path {
    Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: 2 * r, height: 2 * r))
  }

  static let arcGradient = Gradient(colors: [Brand.goldLight, Brand.goldDeep])

  static func drawArc(_ context: GraphicsContext) {
    context.stroke(
      arc,
      with: .linearGradient(arcGradient, startPoint: .zero, endPoint: CGPoint(x: 100, y: 100)),
      style: StrokeStyle(lineWidth: ringWidth, lineCap: .round, lineJoin: .round)
    )
    context.fill(circle(knob, knobRadius), with: .color(.white))
  }

  static func draw(_ context: GraphicsContext, arc withArc: Bool) {
    context.stroke(circle(ringCenter, ringRadius), with: .color(.white.opacity(0.22)), lineWidth: ringWidth)
    if withArc { drawArc(context) }
    context.fill(Path(roundedRect: CGRect(x: 32, y: 34, width: 36, height: 33), cornerRadius: 6), with: .color(.white))
    // The page's ember header: the top 10 units with the page's rounded top corners.
    context.fill(
      UnevenRoundedRectangle(topLeadingRadius: 6, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 6)
        .path(in: CGRect(x: 32, y: 34, width: 36, height: 10)),
      with: .color(Brand.ember)
    )
    for x in [40.0, 55.5] {
      context.fill(Path(roundedRect: CGRect(x: x, y: 30, width: 4.5, height: 8), cornerRadius: 2.25), with: .color(Color(hex: 0xFFE3D0)))
    }
    for (x, y) in [(41.0, 52.0), (50.0, 52.0), (59.0, 52.0), (41.0, 60.0), (50.0, 60.0)] {
      context.fill(circle(CGPoint(x: x, y: y), 2.8), with: .color(Color(hex: 0xE9D9F0)))
    }
    context.fill(circle(CGPoint(x: 59, y: 60), 3.4), with: .color(Brand.ember))
  }
}

/// The app emblem. `tile` = on the rounded dusk tile (Settings → About, empty state); `arc` = with the
/// gold progress arc (the loading screen turns it off and spins its own copy). Never mirrored.
struct AppLogo: View {
  let size: CGFloat
  var tile = false
  var arc = true

  var body: some View {
    Canvas { context, canvasSize in
      var ctx = context
      ctx.scaleBy(x: canvasSize.width / 100, y: canvasSize.height / 100)
      if tile {
        let rect = Path(roundedRect: CGRect(x: 0, y: 0, width: 100, height: 100), cornerRadius: 22)
        ctx.fill(rect, with: .linearGradient(Gradient(colors: [Brand.tileFrom, Brand.tileTo]), startPoint: .zero, endPoint: CGPoint(x: 100, y: 100)))
        ctx.fill(rect, with: .radialGradient(
          Gradient(colors: [Color(hex: 0xA855F7, opacity: 0.45), Color(hex: 0xA855F7, opacity: 0)]),
          center: CGPoint(x: 30, y: 25), startRadius: 0, endRadius: 70
        ))
        ctx.translateBy(x: 14, y: 14)
        ctx.scaleBy(x: 0.72, y: 0.72)
      }
      Emblem.draw(ctx, arc: arc)
    }
    .frame(width: size, height: size)
    .environment(\.layoutDirection, .leftToRight)
    .accessibilityHidden(true)
  }
}

/// The arc layer on its own, sized like `AppLogo` (the loading screen spins it).
struct LogoArc: View {
  let size: CGFloat

  var body: some View {
    Canvas { context, canvasSize in
      var ctx = context
      ctx.scaleBy(x: canvasSize.width / 100, y: canvasSize.height / 100)
      Emblem.drawArc(ctx)
    }
    .frame(width: size, height: size)
    .environment(\.layoutDirection, .leftToRight)
    .accessibilityHidden(true)
  }
}
