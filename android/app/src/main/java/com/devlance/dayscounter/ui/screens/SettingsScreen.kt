package com.devlance.dayscounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.BuildConfig
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.core.localizeDigits
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.data.ThemeMode
import com.devlance.dayscounter.ui.brand.AppLogo
import com.devlance.dayscounter.ui.components.Card
import com.devlance.dayscounter.ui.components.ConfirmDialog
import com.devlance.dayscounter.ui.components.DangerButton
import com.devlance.dayscounter.ui.components.Divider
import com.devlance.dayscounter.ui.components.SectionLabel
import com.devlance.dayscounter.ui.components.SegmentOption
import com.devlance.dayscounter.ui.components.SegmentedControl
import com.devlance.dayscounter.ui.components.SwitchRow
import com.devlance.dayscounter.ui.components.Toasts
import com.devlance.dayscounter.ui.icons.LucideIcon
import com.devlance.dayscounter.ui.icons.LucideIcons
import com.devlance.dayscounter.ui.theme.Radius
import com.devlance.dayscounter.ui.theme.Spacing
import com.devlance.dayscounter.ui.theme.Type
import com.devlance.dayscounter.ui.theme.appTheme

/**
 * Settings, in v1's order: language · appearance (follow-system switch, light/dark tiles otherwise)
 * · countdown options · home-screen widget (from the widget phase on) · data · about.
 */
@Composable
fun SettingsScreen() {
  val theme = appTheme()
  val c = theme.colors
  val s = theme.strings
  val settings by AppStore.settings.collectAsStateWithLifecycle()
  val hasCountdowns = AppStore.countdowns.collectAsStateWithLifecycle().value.countdowns.isNotEmpty()
  var confirmReset by remember { mutableStateOf(false) }

  Column(
    Modifier
      .fillMaxSize()
      .background(c.background)
      .windowInsetsPadding(WindowInsets.statusBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = Spacing.lg)
      .padding(top = Spacing.md, bottom = Spacing.xxxl),
  ) {
    Text(s["settings.title"], style = Type.displayMedium, color = c.text, modifier = Modifier.semantics { heading() })

    // Language
    SectionLabel(s["settings.language"], first = true)
    SegmentedControl(
      options = listOf(SegmentOption(Lang.EN, "English"), SegmentOption(Lang.AR, "العربية")),
      value = settings.language,
      onChange = { lang -> AppStore.updateSettings { it.copy(language = lang) } },
    )

    // Appearance — same interaction as the sibling apps: follow-system switch, tiles otherwise.
    SectionLabel(s["settings.appearance"])
    Card {
      val followSystem = settings.themeMode == ThemeMode.SYSTEM
      SwitchRow(LucideIcons.Contrast, s["settings.followSystem"], s["settings.followSystemHint"], followSystem) { on ->
        AppStore.updateSettings { it.copy(themeMode = if (on) ThemeMode.SYSTEM else if (theme.isDark) ThemeMode.DARK else ThemeMode.LIGHT) }
      }
      AnimatedVisibility(
        visible = !followSystem,
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
      ) {
        Row(Modifier.fillMaxWidth().padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
          for ((mode, key, icon) in listOf(Triple(ThemeMode.LIGHT, "settings.light", LucideIcons.Sun), Triple(ThemeMode.DARK, "settings.dark", LucideIcons.Moon))) {
            val active = settings.themeMode == mode
            val shape = RoundedCornerShape(Radius.md)
            Column(
              Modifier
                .weight(1f)
                .heightIn(min = 72.dp)
                .clip(shape)
                .background(if (active) c.primarySoft else Color.Transparent)
                .border(1.5.dp, if (active) c.primary else c.border, shape)
                .selectable(selected = active, role = Role.RadioButton) { AppStore.updateSettings { it.copy(themeMode = mode) } }
                .padding(vertical = Spacing.md),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
            ) {
              LucideIcon(icon, if (active) c.primary else c.textMuted, 22.dp)
              Text(s[key], style = Type.bodyStrong, color = if (active) c.primary else c.text)
            }
          }
        }
      }
    }

    // Countdown
    SectionLabel(s["settings.countdown"])
    Card {
      SwitchRow(LucideIcons.Timer, s["settings.showSeconds"], s["settings.showSecondsHint"], settings.showSeconds) { on ->
        AppStore.updateSettings { it.copy(showSeconds = on) }
      }
      Divider()
      SwitchRow(LucideIcons.History, s["settings.countUp"], s["settings.countUpHint"], settings.countUp) { on ->
        AppStore.updateSettings { it.copy(countUp = on) }
      }
      Divider()
      SwitchRow(LucideIcons.Clock, s["settings.clock24"], s["settings.clock24Hint"], settings.use24h) { on ->
        AppStore.updateSettings { it.copy(use24h = on) }
      }
    }

    // Data
    if (hasCountdowns) {
      SectionLabel(s["settings.data"])
      DangerButton(LucideIcons.Trash2, s["settings.reset"], onClick = { confirmReset = true })
    }

    // About
    SectionLabel(s["settings.about"])
    Card {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        AppLogo(52.dp, tile = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(s["app.name"], style = Type.subtitle, color = c.text)
          Text(s.t("settings.version", "version" to localizeDigits(BuildConfig.VERSION_NAME, theme.lang)), style = Type.caption, color = c.textMuted)
        }
      }
      Spacer(Modifier.height(Spacing.md))
      Text(s["settings.aboutBody"], style = Type.body, color = c.text)
      Text(s["settings.madeBy"], style = Type.caption, color = c.textMuted, modifier = Modifier.padding(top = Spacing.md))
    }
  }

  if (confirmReset) {
    ConfirmDialog(
      title = s["settings.resetTitle"],
      body = s["settings.resetBody"],
      confirm = s["common.delete"],
      onConfirm = {
        confirmReset = false
        AppStore.updateCountdowns { it.removeAll() }
        Toasts.show(s["toast.reset"])
      },
      onDismiss = { confirmReset = false },
    )
  }
}
