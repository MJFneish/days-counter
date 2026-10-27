package com.devlance.dayscounter.data

import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownColor
import com.devlance.dayscounter.core.CountdownIcon
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.core.LocalDateTime
import com.devlance.dayscounter.core.isValidDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * The persisted JSON — the same shapes as v1's zustand stores (`state` of `days-counter-settings`
 * and `days-counter-countdowns`), so v1's data is read by the same decoder. Decoding sanitizes like
 * v1's `merge`: anything malformed falls back to a default instead of failing the launch.
 */
object StoreJson {
  fun encodeSettings(s: Settings): JSONObject = JSONObject()
    .put("language", s.language.code)
    .put("themeMode", s.themeMode.id)
    .put("showSeconds", s.showSeconds)
    .put("countUp", s.countUp)
    .put("use24h", s.use24h)

  fun decodeSettings(o: JSONObject?, defaults: Settings): Settings {
    if (o == null) return defaults
    val language = o.optString("language").takeIf { it == "en" || it == "ar" }?.let(Lang::of)
    return Settings(
      language = language ?: defaults.language,
      themeMode = ThemeMode.of(o.optString("themeMode")) ?: defaults.themeMode,
      showSeconds = o.boolOrNull("showSeconds") ?: defaults.showSeconds,
      countUp = o.boolOrNull("countUp") ?: defaults.countUp,
      use24h = o.boolOrNull("use24h") ?: defaults.use24h,
    )
  }

  fun encodeCountdowns(state: CountdownsState): JSONObject {
    val list = JSONArray()
    for (c in state.countdowns) {
      list.put(
        JSONObject()
          .put("id", c.id)
          .put("title", c.title)
          .put(
            "target",
            JSONObject().put("year", c.target.year).put("month", c.target.month).put("day", c.target.day)
              .put("hour", c.target.hour).put("minute", c.target.minute),
          )
          .put("color", c.color.id)
          .put("icon", c.icon.id)
          .put("createdAt", c.createdAt),
      )
    }
    return JSONObject().put("countdowns", list).put("pinnedId", state.pinnedId ?: JSONObject.NULL).put("seeded", state.seeded)
  }

  fun decodeCountdowns(o: JSONObject?, now: Long): CountdownsState {
    if (o == null) return CountdownsState()
    val list = o.optJSONArray("countdowns")
    val countdowns = buildList {
      if (list != null) {
        for (i in 0 until list.length()) {
          val c = list.optJSONObject(i) ?: continue
          val id = (c.opt("id") as? String) ?: continue
          val target = decodeTarget(c.optJSONObject("target")) ?: continue
          add(
            Countdown(
              id = id,
              title = (c.opt("title") as? String) ?: "",
              target = target,
              color = CountdownColor.of(c.opt("color") as? String) ?: CountdownColor.EMBER,
              icon = CountdownIcon.of(c.opt("icon") as? String) ?: CountdownIcon.CALENDAR,
              createdAt = (c.opt("createdAt") as? Number)?.toLong() ?: now,
            ),
          )
        }
      }
    }
    val stored = o.opt("pinnedId") as? String
    val pinnedId = if (countdowns.any { it.id == stored }) stored else countdowns.firstOrNull()?.id
    return CountdownsState(countdowns, pinnedId, o.opt("seeded") == true)
  }

  private fun decodeTarget(t: JSONObject?): LocalDateTime? {
    if (t == null) return null
    fun int(key: String): Int? = (t.opt(key) as? Number)?.takeIf { it.toDouble() == Math.floor(it.toDouble()) }?.toInt()
    val year = int("year") ?: return null
    val month = int("month") ?: return null
    val day = int("day") ?: return null
    val hour = int("hour") ?: return null
    val minute = int("minute") ?: return null
    if (!isValidDate(year, month, day) || hour !in 0..23 || minute !in 0..59) return null
    return LocalDateTime(year, month, day, hour, minute)
  }

  private fun JSONObject.boolOrNull(key: String): Boolean? = opt(key) as? Boolean
}
