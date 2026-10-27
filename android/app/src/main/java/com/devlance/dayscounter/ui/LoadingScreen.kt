package com.devlance.dayscounter.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.ui.brand.AppLogo
import com.devlance.dayscounter.ui.brand.LogoArc
import com.devlance.dayscounter.ui.theme.Brand
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme

/** Must equal the splash emblem (120 dp in the middle of the 288 dp icon) so the hand-off doesn't jump. */
private val LOGO_SIZE = 120.dp
private val BAR_WIDTH = 160.dp

/** Deterministic sparks (fractions of the screen, radius dp, delay ms), the same every launch. */
private val SPARKS = listOf(
  floatArrayOf(0.12f, 0.14f, 2.2f, 0f),
  floatArrayOf(0.82f, 0.1f, 1.6f, 400f),
  floatArrayOf(0.68f, 0.22f, 2.4f, 900f),
  floatArrayOf(0.24f, 0.3f, 1.4f, 1300f),
  floatArrayOf(0.9f, 0.36f, 1.8f, 200f),
  floatArrayOf(0.08f, 0.52f, 2f, 1100f),
  floatArrayOf(0.86f, 0.62f, 1.5f, 600f),
  floatArrayOf(0.18f, 0.74f, 2.2f, 1500f),
  floatArrayOf(0.76f, 0.8f, 1.8f, 300f),
  floatArrayOf(0.44f, 0.88f, 1.4f, 1000f),
)

private val SINE = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * Animated launch screen (v1's LoadingScreen): drifting violet/ember glows, twinkling sparks, the
 * emblem with its gold countdown arc spinning around the calendar page, the app name and an
 * indeterminate bar. Its first frame is exactly the splash (same background, same 120 dp emblem,
 * arc at rest), so the system splash disappearing underneath is invisible. Animations follow the
 * system animator scale (off = still).
 */
@Composable
fun LoadingScreen() {
  val theme = appTheme()
  val transition = rememberInfiniteTransition(label = "loading")
  val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)), label = "spin")
  val breathe by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = SINE), RepeatMode.Reverse), label = "breathe")
  val bar by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing)), label = "bar")
  val drift1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = SINE), RepeatMode.Reverse), label = "glow1")
  val drift2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6400, easing = SINE), RepeatMode.Reverse), label = "glow2")
  val sparkTime by transition.animateFloat(0f, 4800f, infiniteRepeatable(tween(4800, easing = LinearEasing)), label = "sparks")
  val titleIn = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(200)
    titleIn.animateTo(1f, tween(500))
  }

  BoxWithConstraints(
    Modifier
      .fillMaxSize()
      .background(Brand.night)
      .semantics { contentDescription = theme.strings["loading.label"] },
    contentAlignment = Alignment.Center,
  ) {
    val titleTop = maxHeight / 2 + LOGO_SIZE / 2 + Spacing.xl
    Canvas(Modifier.fillMaxSize()) {
      fun glow(size: Float, left: Float, top: Float, color: Color, drift: Float, t: Float) {
        val scale = 1f + t * 0.12f
        val r = size / 2 * scale
        val center = Offset(left + size / 2 + t * drift, top + size / 2 - t * drift * 0.6f)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0f)), center, r), r, center)
      }
      val width = size.width
      val height = size.height
      glow(width * 1.1f, -width * 0.4f, -width * 0.35f, Brand.violet, 30.dp.toPx(), drift1)
      glow(width * 0.95f, width * 0.3f, height * 0.6f, Brand.ember, -26.dp.toPx(), drift2)
      for (spark in SPARKS) {
        // Twinkle: 0.3 → 1 over 1.1 s, back to 0.2 over 1.3 s, offset by each spark's delay.
        val phase = ((sparkTime - spark[3]) % 2400f + 2400f) % 2400f
        val alpha = if (phase < 1100f) 0.3f + 0.7f * (phase / 1100f) else 1f - 0.8f * ((phase - 1100f) / 1300f)
        drawCircle(Brand.light.copy(alpha = alpha), spark[2].dp.toPx(), Offset(spark[0] * width, spark[1] * height))
      }
    }

    Box(Modifier.size(LOGO_SIZE), contentAlignment = Alignment.Center) {
      AppLogo(LOGO_SIZE, Modifier.graphicsLayer { scaleX = 1f + breathe * 0.04f; scaleY = 1f + breathe * 0.04f }, arc = false)
      LogoArc(LOGO_SIZE, Modifier.graphicsLayer { rotationZ = spin })
    }

    Column(
      Modifier
        .align(Alignment.TopCenter)
        .padding(top = titleTop)
        .padding(horizontal = Spacing.xl)
        .graphicsLayer { alpha = titleIn.value; translationY = (1f - titleIn.value) * 16.dp.toPx() },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
      Text(theme.strings["app.name"], style = Type.displayMedium, color = Color.White, textAlign = TextAlign.Center)
      Text(theme.strings["loading.tagline"], style = Type.body, color = Brand.light.copy(alpha = 0.78f), textAlign = TextAlign.Center)
    }

    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 72.dp)
        .width(BAR_WIDTH)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(Brand.light.copy(alpha = 0.16f)),
    ) {
      Box(
        Modifier
          .width(BAR_WIDTH * 0.4f)
          .height(4.dp)
          .graphicsLayer { translationX = (-0.4f + bar * 1.4f) * BAR_WIDTH.toPx() }
          .clip(RoundedCornerShape(2.dp))
          .background(Brand.gold),
      )
    }
  }
}
