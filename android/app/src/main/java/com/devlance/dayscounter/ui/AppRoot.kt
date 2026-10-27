package com.devlance.dayscounter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devlance.dayscounter.ui.components.ToastHost
import com.devlance.dayscounter.ui.editor.EditorSheet
import com.devlance.dayscounter.ui.settings.CountdownPickerSheet
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIconData
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.screens.HomeScreen
import com.devlance.dayscounter.ui.screens.SettingsScreen
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How screens move around: switch tab (0 = Home, 1 = Settings). */
class Navigator(private val pager: PagerState, private val scope: CoroutineScope) {
  fun goToTab(index: Int) {
    scope.launch { pager.animateScrollToPage(index) }
  }
}

val LocalNavigator = compositionLocalOf<Navigator> { error("no navigator") }

private data class Tab(val icon: LucideIconData, val labelKey: String)

private val TABS = listOf(
  Tab(LucideIcons.Hourglass, "tabs.home"),
  Tab(LucideIcons.Settings2, "tabs.settings"),
)

/** The loading screen stays up at least this long even when startup is instant — a flash reads as a
 * glitch, not as speed (same value as v1 and the sibling apps). */
private const val MIN_LOADING_VISIBLE_MS = 1200L

/**
 * WhatsApp-style bottom tabs (v1's MainTabs): Home and Settings side by side in a pager that follows
 * the finger 1:1 and snaps to the nearest page (a fast fling goes to the neighbour). In Arabic the
 * pages AND the tab bar mirror together (the layout direction flips both). The loading screen
 * covers everything at launch, then fades out.
 */
@Composable
fun AppRoot(onSystemBars: (lightIcons: Boolean) -> Unit) {
  val theme = appTheme()
  val pager = rememberPagerState(pageCount = { TABS.size })
  val scope = rememberCoroutineScope()
  val navigator = remember(pager) { Navigator(pager, scope) }
  var loading by rememberSaveable { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    delay(MIN_LOADING_VISIBLE_MS)
    loading = false
  }
  BackHandler(enabled = pager.currentPage != 0) { navigator.goToTab(0) }
  LaunchedEffect(loading, theme.isDark) { onSystemBars(loading || theme.isDark) }

  androidx.compose.runtime.CompositionLocalProvider(LocalNavigator provides navigator) {
    Box(Modifier.fillMaxSize().background(theme.colors.background)) {
      Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1, key = { it }) { page ->
          when (page) {
            0 -> HomeScreen()
            else -> SettingsScreen()
          }
        }
        TabBar(pager.currentPage) { navigator.goToTab(it) }
      }
      // Sheets cover the tabs and the tab bar; toasts and the loading screen go above them.
      EditorSheet()
      CountdownPickerSheet()
      ToastHost()
      AnimatedVisibility(visible = loading, enter = androidx.compose.animation.EnterTransition.None, exit = fadeOut(tween(380))) {
        LoadingScreen()
      }
    }
  }
}

@Composable
private fun TabBar(current: Int, onSelect: (Int) -> Unit) {
  val theme = appTheme()
  val c = theme.colors
  Column(Modifier.fillMaxWidth().background(c.surface)) {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.border))
    Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).selectableGroup()) {
      TABS.forEachIndexed { index, tab ->
        val active = current == index
        val color = if (active) c.primary else c.textMuted
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .selectable(selected = active, role = Role.Tab) { onSelect(index) }
            .padding(top = Spacing.sm, bottom = Spacing.xs),
        ) {
          // The pill behind the active icon (v1 kept it painted and toggled its opacity).
          Box(
            Modifier
              .clip(RoundedCornerShape(15.dp))
              .background(if (active) c.primarySoft else Color.Transparent)
              .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
          ) {
            LucideIcon(tab.icon, color, 22.dp, strokeWidth = if (active) 2.4f else 2f)
          }
          Text(theme.strings[tab.labelKey], style = Type.caption.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium), color = color)
        }
      }
    }
  }
}
