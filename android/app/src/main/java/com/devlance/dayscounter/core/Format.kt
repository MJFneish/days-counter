package com.devlance.dayscounter.core

private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

/** Arabic UI shows Arabic-Indic digits (٢٠٢٧), like the sibling apps. Done by hand: Android's "ar"
 * locale formats with Latin digits. */
fun localizeDigits(value: Any, lang: Lang): String {
  val s = value.toString()
  if (lang != Lang.AR) return s
  return buildString(s.length) { for (c in s) append(if (c in '0'..'9') ARABIC_INDIC[c - '0'] else c) }
}

/** Levantine month names (كانون الثاني…), as Lebanese calendars print them — same as the Hijri app. */
private val MONTHS_AR = listOf(
  "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
  "تمّوز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول",
)
private val MONTHS_EN = listOf(
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
)
private val MONTHS_EN_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val WEEKDAYS_AR = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
private val WEEKDAYS_EN = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

fun monthName(month: Int, lang: Lang): String = (if (lang == Lang.AR) MONTHS_AR else MONTHS_EN)[month - 1]

fun weekdayName(weekday: Int, lang: Lang): String = (if (lang == Lang.AR) WEEKDAYS_AR else WEEKDAYS_EN)[weekday]

/** "Sunday, 5 September 2027" / "الأحد ٥ أيلول ٢٠٢٧". */
fun formatLongDate(year: Int, month: Int, day: Int, lang: Lang): String {
  val weekday = weekdayName(weekdayOf(year, month, day), lang)
  return if (lang == Lang.AR) {
    localizeDigits("$weekday $day ${monthName(month, lang)} $year", lang)
  } else {
    "$weekday, $day ${monthName(month, lang)} $year"
  }
}

fun formatLongDate(t: LocalDateTime, lang: Lang): String = formatLongDate(t.year, t.month, t.day, lang)

/** "5 Sep 2027" / "٥ أيلول ٢٠٢٧" — Arabic has no customary short month names. */
fun formatShortDate(t: LocalDateTime, lang: Lang): String =
  if (lang == Lang.AR) {
    localizeDigits("${t.day} ${monthName(t.month, lang)} ${t.year}", lang)
  } else {
    "${t.day} ${MONTHS_EN_SHORT[t.month - 1]} ${t.year}"
  }

fun pad2(n: Int): String = n.toString().padStart(2, '0')

/** The AM/PM marker: "AM"/"PM", "ص"/"م". */
fun dayPeriod(hour: Int, lang: Lang): String =
  if (lang == Lang.AR) (if (hour < 12) "ص" else "م") else (if (hour < 12) "AM" else "PM")

/** 12-hour clock hour: 0 → 12, 13 → 1. */
fun hour12(hour: Int): Int = if (hour % 12 == 0) 12 else hour % 12

/** "00:00" (24h) or "12:00 AM" / "١٢:٠٠ ص" (12h). */
fun formatClock(hour: Int, minute: Int, lang: Lang, use24h: Boolean): String {
  if (use24h) return localizeDigits("${pad2(hour)}:${pad2(minute)}", lang)
  return localizeDigits("${hour12(hour)}:${pad2(minute)} ${dayPeriod(hour, lang)}", lang)
}

/** "62%" / "٦٢٪". */
fun formatPercent(percent: Int, lang: Lang): String =
  if (lang == Lang.AR) "${localizeDigits(percent, lang)}٪" else "$percent%"
