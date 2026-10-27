package com.devlance.dayscounter.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ITEM_HEIGHT = 44.dp
private const val VISIBLE_ROWS = 5

/**
 * iOS-style scroll wheel (v1's WheelPicker, which came from the Hijri converter): a snapping list
 * five rows tall with a selection band in the middle and fades at both ends. The centred row is
 * reported when the scroll settles; changes from outside (a day clamped by a shorter month) scroll
 * it into place. Tapping a row selects it. One picker for both languages and themes, inside the
 * editor sheet — no native dialog popping over it.
 */
@Composable
fun WheelPicker(items: List<String>, selectedIndex: Int, onChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
  val theme = appTheme()
  val c = theme.colors
  val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
  val scope = rememberCoroutineScope()
  val currentOnChange by rememberUpdatedState(onChange)
  val center by remember {
    derivedStateOf {
      val info = state.layoutInfo
      val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
      info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: state.firstVisibleItemIndex
    }
  }

  // Report the centred row once the wheel comes to rest.
  LaunchedEffect(state.isScrollInProgress) {
    if (!state.isScrollInProgress && center != selectedIndex) currentOnChange(center.coerceIn(0, items.size - 1))
  }
  // Follow changes made from outside.
  LaunchedEffect(selectedIndex, items.size) {
    if (!state.isScrollInProgress && center != selectedIndex) state.animateScrollToItem(selectedIndex.coerceIn(0, items.size - 1))
  }

  Box(
    modifier
      .height(ITEM_HEIGHT * VISIBLE_ROWS)
      .semantics {
        contentDescription = label
        stateDescription = items.getOrElse(selectedIndex) { "" }
        customActions = listOf(
          CustomAccessibilityAction("+") { if (selectedIndex < items.size - 1) currentOnChange(selectedIndex + 1); true },
          CustomAccessibilityAction("−") { if (selectedIndex > 0) currentOnChange(selectedIndex - 1); true },
        )
      },
  ) {
    // The selection band behind the middle row.
    Box(
      Modifier
        .align(Alignment.Center)
        .fillMaxWidth()
        .padding(horizontal = 2.dp)
        .height(ITEM_HEIGHT)
        .clip(RoundedCornerShape(Radius.md))
        .background(c.primarySoft),
    )
    LazyColumn(
      state = state,
      flingBehavior = rememberSnapFlingBehavior(state),
      contentPadding = PaddingValues(vertical = ITEM_HEIGHT * (VISIBLE_ROWS / 2)),
      modifier = Modifier.fillMaxSize(),
    ) {
      itemsIndexed(items) { index, text ->
        val distance = abs(index - center)
        Box(
          Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .clickable {
              scope.launch { state.animateScrollToItem(index) }
              currentOnChange(index)
            }
            .padding(horizontal = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text,
            style = if (distance == 0) Type.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold) else Type.body,
            color = if (distance == 0) c.text else c.textMuted.copy(alpha = if (distance == 1) 0.85f else 0.55f),
            maxLines = 1,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
    // Fades into the sheet's background at both ends.
    Box(
      Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .height(ITEM_HEIGHT * 2)
        .background(Brush.verticalGradient(listOf(c.surface.copy(alpha = 0.95f), Color.Transparent))),
    )
    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(ITEM_HEIGHT * 2)
        .background(Brush.verticalGradient(listOf(Color.Transparent, c.surface.copy(alpha = 0.95f)))),
    )
  }
}
