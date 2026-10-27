package com.devlance.dayscounter.widget

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps the widget's payload in step with the app (v1's useWidgetSync): whenever the pinned
 * countdown, the language or the count-up / clock setting changes, and each time the app comes to
 * the foreground (cheap, and it repairs a widget placed while the app was closed). Started once in
 * DaysCounterApp.
 */
object WidgetSync {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun start(context: Context) {
    val app = context.applicationContext
    scope.launch {
      combine(AppStore.settings, AppStore.countdowns) { settings, state ->
        WidgetPayload.build(state.pinned, Strings.of(settings.language), settings.countUp, settings.use24h)
      }.distinctUntilChanged().collect { payload ->
        if (WidgetStore.save(app, payload)) DaysWidgetProvider.refreshAll(app)
      }
    }
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStart(owner: LifecycleOwner) {
        scope.launch { DaysWidgetProvider.refreshAll(app) }
      }
    })
  }
}
