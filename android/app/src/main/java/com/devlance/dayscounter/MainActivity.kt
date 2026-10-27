package com.devlance.dayscounter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.ui.AppRoot
import com.devlance.dayscounter.ui.ClockProvider
import com.devlance.dayscounter.ui.theme.DaysCounterTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by AppStore.settings.collectAsStateWithLifecycle()
      DaysCounterTheme(settings) {
        ClockProvider(everySecond = settings.showSeconds) {
          AppRoot(onSystemBars = ::setSystemBars)
        }
      }
    }
  }

  /** Status/navigation bar icons: light on the night loading screen and in dark mode, dark otherwise
   * (the app's own theme can differ from the phone's). */
  private fun setSystemBars(lightIcons: Boolean) {
    val style = if (lightIcons) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
  }
}
