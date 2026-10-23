package com.devlance.dayscounter.widget

import android.content.Context
import org.json.JSONObject

data class WidgetCountdown(
  val title: String,
  val detail: String,
  val target: LocalDateTime,
  val createdAt: Long,
  val color: String,
)

data class WidgetStrings(
  val toGo: String,
  val since: String,
  val arrived: String,
  val passed: String,
  val empty: String,
  val progress: String,
)

data class WidgetPayload(
  val lang: String,
  val rtl: Boolean,
  val countUp: Boolean,
  val countdown: WidgetCountdown?,
  val strings: WidgetStrings,
)

/** Persists the payload JSON the app pushes (`WidgetPayload` in src/widget/payload.ts — keep in
 * sync). SharedPreferences survive reboots and app kills, so the widget keeps working without the
 * app running. Same approach as prayer-times-lb's widget. */
object WidgetStore {
  private const val PREFS = "days_widget"
  private const val KEY_PAYLOAD = "payload"

  fun save(context: Context, json: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PAYLOAD, json).commit()
  }

  fun load(context: Context): WidgetPayload? {
    val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PAYLOAD, null)
      ?: return null
    return try {
      parse(JSONObject(json))
    } catch (e: Exception) {
      null
    }
  }

  private fun parse(root: JSONObject): WidgetPayload {
    val s = root.getJSONObject("strings")
    val strings = WidgetStrings(
      toGo = s.optString("toGo"),
      since = s.optString("since"),
      arrived = s.optString("arrived"),
      passed = s.optString("passed"),
      empty = s.optString("empty"),
      progress = s.optString("progress", "{p}"),
    )
    val c = root.optJSONObject("countdown")
    val countdown = c?.let {
      val t = it.getJSONObject("target")
      WidgetCountdown(
        title = it.optString("title"),
        detail = it.optString("detail"),
        target = LocalDateTime(t.getInt("year"), t.getInt("month"), t.getInt("day"), t.getInt("hour"), t.getInt("minute")),
        createdAt = it.optLong("createdAt", System.currentTimeMillis()),
        color = it.optString("color", "ember"),
      )
    }
    return WidgetPayload(
      lang = root.optString("lang", "en"),
      rtl = root.optBoolean("rtl", false),
      countUp = root.optBoolean("countUp", true),
      countdown = countdown,
      strings = strings,
    )
  }
}
