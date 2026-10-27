package com.devlance.dayscounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.core.formatLongDate
import com.devlance.dayscounter.core.fromInstant
import com.devlance.dayscounter.core.relativePhrase
import com.devlance.dayscounter.core.remaining
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.ui.LocalNow
import com.devlance.dayscounter.ui.components.Card
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme

/** Home: today's date and the countdowns. (The hero card, stats and list rows land in the Home
 * phase; this first version lists every countdown with its relative phrase.) */
@Composable
fun HomeScreen() {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val now = LocalNow.current
  val state by AppStore.countdowns.collectAsStateWithLifecycle()
  val today = fromInstant(now)

  Column(
    Modifier
      .fillMaxSize()
      .background(c.background)
      .windowInsetsPadding(WindowInsets.statusBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = Spacing.lg)
      .padding(top = Spacing.md, bottom = Spacing.xxxl),
    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
  ) {
    Text(s["home.title"], style = Type.displayMedium, color = c.text, modifier = Modifier.semantics { heading() })
    Text(formatLongDate(today, theme.lang), style = Type.caption, color = c.textMuted)
    if (state.countdowns.isEmpty()) {
      Text(s["home.emptyTitle"], style = Type.title, color = c.text, modifier = Modifier.padding(top = Spacing.xl))
    }
    for (countdown in state.countdowns) {
      Card {
        Text(countdownTitle(countdown, theme.lang), style = Type.bodyStrong, color = c.text)
        Text(relativePhrase(remaining(countdown.target, now), s), style = Type.caption, color = c.textMuted)
      }
    }
  }
}
