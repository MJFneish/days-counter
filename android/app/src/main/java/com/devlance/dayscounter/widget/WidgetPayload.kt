package com.devlance.dayscounter.widget

import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.LocalDateTime
import com.devlance.dayscounter.core.countdownSubtitle
import com.devlance.dayscounter.core.countdownTitle
import com.devlance.dayscounter.i18n.Strings
import org.json.JSONObject

data class WidgetCountdown(
  val title: String,
  /** Second line: the date (when the title isn't already the date) and the time. */
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
  /** "{p} of the wait done" — the widget puts its own formatted percent in place of {p}. */
  val progress: String,
)

/**
 * What the widget draws from. It computes the countdown itself (core `remaining`), so this only
 * changes when the pinned countdown, the language, the count-up or clock setting does — not every
 * day. The JSON is v1's contract (src/widget/payload.ts), so a payload v1 left behind still renders
 * after the update until the app first opens.
 */
data class WidgetPayload(
  val lang: String,
  val rtl: Boolean,
  val countUp: Boolean,
  val countdown: WidgetCountdown?,
  val strings: WidgetStrings,
) {
  fun toJson(): JSONObject = JSONObject()
    .put("v", 1)
    .put("lang", lang)
    .put("rtl", rtl)
    .put("countUp", countUp)
    .put(
      "countdown",
      countdown?.let {
        JSONObject()
          .put("title", it.title)
          .put("detail", it.detail)
          .put(
            "target",
            JSONObject().put("year", it.target.year).put("month", it.target.month).put("day", it.target.day)
              .put("hour", it.target.hour).put("minute", it.target.minute),
          )
          .put("createdAt", it.createdAt)
          .put("color", it.color)
      } ?: JSONObject.NULL,
    )
    .put(
      "strings",
      JSONObject()
        .put("toGo", strings.toGo)
        .put("since", strings.since)
        .put("arrived", strings.arrived)
        .put("passed", strings.passed)
        .put("empty", strings.empty)
        .put("progress", strings.progress),
    )

  companion object {
    fun build(pinned: Countdown?, strings: Strings, countUp: Boolean, use24h: Boolean): WidgetPayload {
      val lang = strings.lang
      return WidgetPayload(
        lang = lang.code,
        rtl = lang.code == "ar",
        countUp = countUp,
        countdown = pinned?.let {
          WidgetCountdown(
            title = countdownTitle(it, lang),
            detail = countdownSubtitle(it, strings, use24h, short = true),
            target = it.target,
            createdAt = it.createdAt,
            color = it.color.id,
          )
        },
        strings = WidgetStrings(
          toGo = strings["countdown.toGo"],
          since = strings["countdown.since"],
          arrived = strings["countdown.arrived"],
          passed = strings["countdown.doneNoCountUp"],
          empty = strings["home.emptyTitle"],
          progress = strings.t("countdown.progress", "percent" to "{p}"),
        ),
      )
    }

    /** Reads a stored payload; null when it's missing or unreadable. */
    fun parse(json: String?): WidgetPayload? {
      if (json == null) return null
      return try {
        val root = JSONObject(json)
        val s = root.getJSONObject("strings")
        val c = root.optJSONObject("countdown")
        WidgetPayload(
          lang = root.optString("lang", "en"),
          rtl = root.optBoolean("rtl", false),
          countUp = root.optBoolean("countUp", true),
          countdown = c?.let {
            val t = it.getJSONObject("target")
            WidgetCountdown(
              title = it.optString("title"),
              detail = it.optString("detail"),
              target = LocalDateTime(t.getInt("year"), t.getInt("month"), t.getInt("day"), t.getInt("hour"), t.getInt("minute")),
              createdAt = it.optLong("createdAt", System.currentTimeMillis()),
              color = it.optString("color", "ember"),
            )
          },
          strings = WidgetStrings(
            toGo = s.optString("toGo"),
            since = s.optString("since"),
            arrived = s.optString("arrived"),
            passed = s.optString("passed"),
            empty = s.optString("empty"),
            progress = s.optString("progress", "{p}"),
          ),
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}
