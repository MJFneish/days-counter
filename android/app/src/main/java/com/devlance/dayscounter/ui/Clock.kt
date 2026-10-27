package com.devlance.dayscounter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** The app's one clock (epoch ms). Every countdown on screen reads it, so they all tick together. */
val LocalNow = compositionLocalOf { System.currentTimeMillis() }

/**
 * Ticks on whole seconds — or whole minutes when Settings → "Show seconds" is off — aligned to the
 * wall clock, so a countdown to 00:00:00 flips exactly on the second. Stops while the app is in
 * the background and catches up the moment it returns. Mounted once around the app (v1's
 * `useClockDriver`).
 */
@Composable
fun ClockProvider(everySecond: Boolean, content: @Composable () -> Unit) {
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(everySecond, lifecycle) {
    val step = if (everySecond) 1000L else 60_000L
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      while (true) {
        val t = System.currentTimeMillis()
        now = t
        // +5 ms so the tick lands just after the boundary, never a hair before it.
        delay(step - t % step + 5)
      }
    }
  }
  CompositionLocalProvider(LocalNow provides now, content = content)
}
