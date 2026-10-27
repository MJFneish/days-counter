package com.devlance.dayscounter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.appTheme

/** Which bottom sheet is open (v1's useSheetStore). `null` id = a new countdown. */
object Sheets {
  data class EditorRequest(val countdownId: String?, val token: Int)

  var editor by mutableStateOf<EditorRequest?>(null)
    private set
  var picker by mutableStateOf(false)
  private var token by mutableIntStateOf(0)

  fun openEditor(countdownId: String?) {
    editor = EditorRequest(countdownId, ++token)
  }

  fun closeEditor() {
    editor = null
  }
}

/**
 * A bottom sheet drawn over the whole app (inside the activity window, above the tabs): scrim,
 * slide-in/out, the Back button and a tap on the scrim close it. Kept in the window rather than a
 * dialog/popup on purpose — v1's RN Modal laid its content out short under edge-to-edge, leaving
 * the bottom of Save/Cancel untouchable; here the sheet's buttons sit above the navigation bar and
 * the keyboard (imePadding).
 */
@Composable
fun BottomSheet(visible: Boolean, onClose: () -> Unit, maxHeightFraction: Float = 0.94f, content: @Composable ColumnScope.() -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  BackHandler(enabled = visible, onBack = onClose)
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val maxSheet = maxHeight * maxHeightFraction
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
      Box(
        Modifier
          .fillMaxSize()
          .background(c.scrim)
          .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
          .semantics { contentDescription = theme.strings["common.close"] },
      )
    }
    AnimatedVisibility(
      visible,
      modifier = Modifier.align(Alignment.BottomCenter),
      enter = slideInVertically(tween(260)) { it },
      exit = slideOutVertically(tween(200)) { it },
    ) {
      Column(
        Modifier
          .fillMaxWidth()
          .heightIn(max = maxSheet)
          .clip(RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl))
          .background(c.surface)
          // Taps on the sheet itself must not reach the scrim behind it.
          .clickable(remember { MutableInteractionSource() }, indication = null) {}
          .windowInsetsPadding(WindowInsets.navigationBars)
          .imePadding()
          .padding(top = Spacing.sm, bottom = Spacing.md),
      ) {
        Box(
          Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = Spacing.sm)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(c.border),
        )
        content()
      }
    }
  }
}
