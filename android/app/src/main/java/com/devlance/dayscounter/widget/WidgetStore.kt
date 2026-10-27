package com.devlance.dayscounter.widget

import android.content.Context

/** Persists the widget payload (WidgetPayload JSON). SharedPreferences survive reboots and app
 * kills, so the widget keeps working without the app running. Same file and key as v1
 * ("days_widget" / "payload"), so a placed widget carries over the update. */
object WidgetStore {
  private const val PREFS = "days_widget"
  private const val KEY_PAYLOAD = "payload"

  /** Returns true when the stored payload changed. */
  fun save(context: Context, payload: WidgetPayload): Boolean {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val json = payload.toJson().toString()
    if (prefs.getString(KEY_PAYLOAD, null) == json) return false
    prefs.edit().putString(KEY_PAYLOAD, json).commit()
    return true
  }

  fun load(context: Context): WidgetPayload? =
    WidgetPayload.parse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PAYLOAD, null))
}
