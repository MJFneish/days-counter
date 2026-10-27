package com.devlance.dayscounter.core

import com.devlance.dayscounter.i18n.Strings
import java.util.TimeZone

/** "10 months and 13 days" — the calendar span of the day count (empty when under a day). */
fun spanPhrase(target: LocalDateTime, r: Remaining, lang: Lang, zone: TimeZone = TimeZone.getDefault()): String {
  val span = spanOf(target, r, zone)
  val parts = buildList {
    if (span.years > 0) add(countPhrase(CountUnit.YEAR, span.years, lang))
    if (span.months > 0) add(countPhrase(CountUnit.MONTH, span.months, lang))
    if (span.days > 0) add(countPhrase(CountUnit.DAY, span.days, lang))
  }
  return joinParts(parts, lang)
}

/** "45 weeks and 1 day" (empty under a week). */
fun weeksPhrase(r: Remaining, lang: Lang): String {
  val weeks = r.days / 7
  if (weeks == 0) return ""
  val rest = r.days % 7
  return joinParts(listOfNotNull(countPhrase(CountUnit.WEEK, weeks, lang), if (rest > 0) countPhrase(CountUnit.DAY, rest, lang) else null), lang)
}

/** Whole hours in total, grouped: "7,593". */
fun totalHours(r: Remaining, lang: Lang): String = formatNumber(r.totalMs / HOUR_MS, lang)

/** The countdown's name, or its date when it has none ("Sunday, 5 September 2027"). */
fun countdownTitle(c: Countdown, lang: Lang): String = c.title.ifEmpty { formatLongDate(c.target, lang) }

/** The line under the title: the date (unless the title already is the date) and, when the target
 * isn't midnight, its time. */
fun countdownSubtitle(c: Countdown, strings: Strings, use24h: Boolean, short: Boolean = false): String {
  val lang = strings.lang
  val parts = buildList {
    if (c.title.isNotEmpty()) add(if (short) formatShortDate(c.target, lang) else formatLongDate(c.target, lang))
    if (c.target.hour != 0 || c.target.minute != 0) {
      add(strings.t("countdown.at", "time" to formatClock(c.target.hour, c.target.minute, lang, use24h)))
    }
  }
  return parts.joinToString(" · ")
}

data class Headline(val value: Int, val unit: CountUnit)

/** The biggest non-zero unit — "316 days", or "5 hours" on the last day. */
fun headline(r: Remaining): Headline = when {
  r.days > 0 -> Headline(r.days, CountUnit.DAY)
  r.hours > 0 -> Headline(r.hours, CountUnit.HOUR)
  r.minutes > 0 -> Headline(r.minutes, CountUnit.MINUTE)
  else -> Headline(r.seconds, CountUnit.SECOND)
}

/** "in 316 days" / "بعد ٣١٦ يومًا", "3 days ago" / "منذ ٣ أيام", "Today" / "اليوم". */
fun relativePhrase(r: Remaining, strings: Strings): String {
  if (r.status == CountdownStatus.ARRIVED) return strings["countdown.today"]
  val (value, unit) = headline(r)
  val phrase = countPhrase(unit, value, strings.lang)
  return if (r.status == CountdownStatus.UPCOMING) strings.t("countdown.in", "phrase" to phrase) else strings.t("countdown.ago", "phrase" to phrase)
}
