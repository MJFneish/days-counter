package com.devlance.dayscounter.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import com.devlance.dayscounter.ui.theme.Brand

/*
 * The emblem, on a 100×100 grid: a countdown ring three-quarters done (gold arc + knob) around a
 * calendar page with one day marked. Original artwork; the same numbers live in
 * android/scripts/generate-icons.js (launcher icons + splash) and the iOS app's AppLogo.swift —
 * keep them in sync.
 */
private const val RING_CX = 50f
private const val RING_CY = 50f
private const val RING_R = 34f
private const val RING_WIDTH = 7f
/** 270° clockwise from 12 o'clock to 9 o'clock. */
private val ARC: Path = PathParser().parsePathString("M50 16 A34 34 0 1 1 16 50").toPath()
private val KNOB = Offset(16f, 50f)
private const val KNOB_R = 5.2f
private val HEADER: Path = PathParser().parsePathString("M32 44 V40 A6 6 0 0 1 38 34 H62 A6 6 0 0 1 68 40 V44 Z").toPath()
private val BINDERS = listOf(40f, 55.5f)
private val DOTS = listOf(Offset(41f, 52f), Offset(50f, 52f), Offset(59f, 52f), Offset(41f, 60f), Offset(50f, 60f))
private val MARKED_DOT = Offset(59f, 60f)

private val arcBrush = Brush.linearGradient(listOf(Brand.goldLight, Brand.goldDeep), Offset(0f, 0f), Offset(100f, 100f))

/** The gold arc + knob alone — the loading screen spins this layer around the still emblem. */
fun DrawScope.drawLogoArc() {
  drawPath(ARC, arcBrush, style = Stroke(RING_WIDTH, cap = StrokeCap.Round))
  drawCircle(Color.White, KNOB_R, KNOB)
}

private fun DrawScope.drawEmblem(arc: Boolean) {
  drawCircle(Color.White.copy(alpha = 0.22f), RING_R, Offset(RING_CX, RING_CY), style = Stroke(RING_WIDTH))
  if (arc) drawLogoArc()
  drawRoundRect(Color.White, Offset(32f, 34f), Size(36f, 33f), CornerRadius(6f, 6f))
  drawPath(HEADER, Brand.ember)
  for (x in BINDERS) drawRoundRect(Color(0xFFFFE3D0), Offset(x, 30f), Size(4.5f, 8f), CornerRadius(2.25f, 2.25f))
  for (dot in DOTS) drawCircle(Color(0xFFE9D9F0), 2.8f, dot)
  drawCircle(Brand.ember, 3.4f, MARKED_DOT)
}

/** Draws in a 100×100 coordinate space scaled to this canvas. */
private fun DrawScope.inLogoSpace(block: DrawScope.() -> Unit) {
  scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) { block() }
}

/**
 * The app emblem. `tile` = on the rounded dusk tile (Settings → About, empty state); `arc` = with
 * the gold progress arc (the loading screen turns it off and spins its own copy).
 */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier, tile: Boolean = false, arc: Boolean = true) {
  Canvas(modifier.size(size)) {
    inLogoSpace {
      if (tile) {
        drawRoundRect(Brush.linearGradient(listOf(Brand.tileFrom, Brand.tileTo), Offset.Zero, Offset(100f, 100f)), cornerRadius = CornerRadius(22f, 22f), size = Size(100f, 100f))
        drawRoundRect(
          Brush.radialGradient(listOf(Color(0xFFA855F7).copy(alpha = 0.45f), Color(0xFFA855F7).copy(alpha = 0f)), center = Offset(30f, 25f), radius = 70f),
          cornerRadius = CornerRadius(22f, 22f),
          size = Size(100f, 100f),
        )
        translate(14f, 14f) { scale(0.72f, 0.72f, pivot = Offset.Zero) { drawEmblem(arc) } }
      } else {
        drawEmblem(arc)
      }
    }
  }
}

/** The arc layer on its own, sized like `AppLogo` (for the loading screen's spinner). */
@Composable
fun LogoArc(size: Dp, modifier: Modifier = Modifier) {
  Canvas(modifier.size(size)) { inLogoSpace { drawLogoArc() } }
}
