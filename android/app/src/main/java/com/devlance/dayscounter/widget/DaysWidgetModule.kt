package com.devlance.dayscounter.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

/**
 * Turbo Native Module `DaysWidget` (spec: src/native/NativeDaysWidget.ts → codegen →
 * NativeDaysWidgetSpec). The app pushes the pinned countdown here; the provider renders it.
 */
class DaysWidgetModule(reactContext: ReactApplicationContext) : NativeDaysWidgetSpec(reactContext) {

  override fun setPayload(json: String) {
    val context = reactApplicationContext
    WidgetStore.save(context, json)
    DaysWidgetProvider.refreshAll(context)
  }

  override fun requestPin(promise: Promise) {
    val context = reactApplicationContext
    val manager = AppWidgetManager.getInstance(context)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !manager.isRequestPinAppWidgetSupported) {
      promise.resolve(false)
      return
    }
    promise.resolve(manager.requestPinAppWidget(ComponentName(context, DaysWidgetProvider::class.java), null, null))
  }

  override fun isRequestPinSupported(promise: Promise) {
    val manager = AppWidgetManager.getInstance(reactApplicationContext)
    promise.resolve(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported)
  }

  override fun getInstalledCount(promise: Promise) {
    promise.resolve(DaysWidgetProvider.installedIds(reactApplicationContext).size.toDouble())
  }

  companion object {
    const val NAME = NativeDaysWidgetSpec.NAME
  }
}
