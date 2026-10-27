package com.devlance.dayscounter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import kotlinx.coroutines.delay

/** Brief confirmations ("Countdown added") — `Toasts.show(...)` from anywhere. */
object Toasts {
  internal var message by mutableStateOf<String?>(null)
  internal var id by mutableIntStateOf(0)

  fun show(text: String) {
    message = text
    id++
  }
}

private const val TOAST_VISIBLE_MS = 1800L

/** Mounted once above everything (v1's ToastHost): a pill above the tab bar. */
@Composable
fun ToastHost() {
  val c = appTheme().colors
  var shown by remember { mutableStateOf<String?>(null) }
  var visible by remember { mutableStateOf(false) }
  val id = Toasts.id
  LaunchedEffect(id) {
    val text = Toasts.message ?: return@LaunchedEffect
    shown = text
    visible = true
    delay(TOAST_VISIBLE_MS)
    visible = false
  }
  Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 88.dp), contentAlignment = Alignment.BottomCenter) {
    AnimatedVisibility(
      visible = visible,
      enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
      exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 },
    ) {
      Row(
        Modifier
          .clip(CircleShape)
          .background(c.text)
          .padding(horizontal = Spacing.lg, vertical = Spacing.md)
          .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LucideIcon(LucideIcons.CircleCheck, c.background, 18.dp)
        Text(shown.orEmpty(), style = Type.bodyStrong, color = c.background)
      }
    }
  }
}

/** "Delete this countdown?" — Cancel / a destructive action. */
@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = c.surface,
    titleContentColor = c.text,
    textContentColor = c.textMuted,
    title = { Text(title, style = Type.title) },
    text = { Text(body, style = Type.body) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(confirm, style = Type.bodyStrong, color = c.danger) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(theme.strings["common.cancel"], style = Type.bodyStrong, color = c.text) }
    },
  )
}
