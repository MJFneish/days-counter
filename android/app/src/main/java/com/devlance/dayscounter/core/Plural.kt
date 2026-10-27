package com.devlance.dayscounter.core

/**
 * Counted phrases ("3 days", "٣ أيام"). Arabic needs all six CLDR forms (zero/one/two/few/many/
 * other) to read naturally, so they're spelled out here instead of going through plural rules.
 * Same tables as v1's src/i18n/plural.ts; the widget uses the same forms through Android
 * `<plurals>` (values-ar/strings.xml).
 */
enum class CountUnit { YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND }

private class ArabicForms(val one: String, val two: String, val few: String, val many: String, val other: String)

/** Full phrases: "سنة واحدة", "سنتان", "٣ سنوات", "١١ سنة"… */
private val AR = mapOf(
  CountUnit.YEAR to ArabicForms("سنة واحدة", "سنتان", "{n} سنوات", "{n} سنة", "{n} سنة"),
  CountUnit.MONTH to ArabicForms("شهر واحد", "شهران", "{n} أشهر", "{n} شهرًا", "{n} شهر"),
  CountUnit.WEEK to ArabicForms("أسبوع واحد", "أسبوعان", "{n} أسابيع", "{n} أسبوعًا", "{n} أسبوع"),
  CountUnit.DAY to ArabicForms("يوم واحد", "يومان", "{n} أيام", "{n} يومًا", "{n} يوم"),
  CountUnit.HOUR to ArabicForms("ساعة واحدة", "ساعتان", "{n} ساعات", "{n} ساعة", "{n} ساعة"),
  CountUnit.MINUTE to ArabicForms("دقيقة واحدة", "دقيقتان", "{n} دقائق", "{n} دقيقة", "{n} دقيقة"),
  CountUnit.SECOND to ArabicForms("ثانية واحدة", "ثانيتان", "{n} ثوانٍ", "{n} ثانية", "{n} ثانية"),
)

/** The noun alone, for a label under a big number: "١٢" over "يومًا". */
private val AR_WORD = mapOf(
  CountUnit.YEAR to ArabicForms("سنة", "سنتان", "سنوات", "سنة", "سنة"),
  CountUnit.MONTH to ArabicForms("شهر", "شهران", "أشهر", "شهرًا", "شهر"),
  CountUnit.WEEK to ArabicForms("أسبوع", "أسبوعان", "أسابيع", "أسبوعًا", "أسبوع"),
  CountUnit.DAY to ArabicForms("يوم", "يومان", "أيام", "يومًا", "يوم"),
  CountUnit.HOUR to ArabicForms("ساعة", "ساعتان", "ساعات", "ساعة", "ساعة"),
  CountUnit.MINUTE to ArabicForms("دقيقة", "دقيقتان", "دقائق", "دقيقة", "دقيقة"),
  CountUnit.SECOND to ArabicForms("ثانية", "ثانيتان", "ثوانٍ", "ثانية", "ثانية"),
)

private val EN = mapOf(
  CountUnit.YEAR to ("year" to "years"),
  CountUnit.MONTH to ("month" to "months"),
  CountUnit.WEEK to ("week" to "weeks"),
  CountUnit.DAY to ("day" to "days"),
  CountUnit.HOUR to ("hour" to "hours"),
  CountUnit.MINUTE to ("minute" to "minutes"),
  CountUnit.SECOND to ("second" to "seconds"),
)

/** "9,780" / "٩٬٧٨٠". */
fun formatNumber(n: Long, lang: Lang): String {
  val digits = Math.abs(n).toString()
  val grouped = StringBuilder()
  for ((i, c) in digits.withIndex()) {
    if (i > 0 && (digits.length - i) % 3 == 0) grouped.append(if (lang == Lang.AR) '٬' else ',')
    grouped.append(c)
  }
  return localizeDigits((if (n < 0) "-" else "") + grouped, lang)
}

fun formatNumber(n: Int, lang: Lang): String = formatNumber(n.toLong(), lang)

enum class ArabicForm { ONE, TWO, FEW, MANY, OTHER }

fun arabicForm(n: Long): ArabicForm {
  val mod100 = n % 100
  return when {
    n == 1L -> ArabicForm.ONE
    n == 2L -> ArabicForm.TWO
    mod100 in 3..10 -> ArabicForm.FEW
    mod100 in 11..99 -> ArabicForm.MANY
    else -> ArabicForm.OTHER
  }
}

private fun ArabicForms.pick(form: ArabicForm): String = when (form) {
  ArabicForm.ONE -> one
  ArabicForm.TWO -> two
  ArabicForm.FEW -> few
  ArabicForm.MANY -> many
  ArabicForm.OTHER -> other
}

fun countPhrase(unit: CountUnit, n: Long, lang: Lang): String {
  if (lang == Lang.AR) {
    if (n == 0L) return "${localizeDigits(0, Lang.AR)} ${AR_WORD.getValue(unit).other}"
    return AR.getValue(unit).pick(arabicForm(n)).replace("{n}", formatNumber(n, Lang.AR))
  }
  val (one, many) = EN.getValue(unit)
  return "${formatNumber(n, Lang.EN)} ${if (n == 1L) one else many}"
}

fun countPhrase(unit: CountUnit, n: Int, lang: Lang): String = countPhrase(unit, n.toLong(), lang)

/** Unit word that agrees with `n`: "day"/"days", "يوم"/"يومان"/"أيام"/"يومًا". */
fun unitWord(unit: CountUnit, n: Long, lang: Lang): String {
  if (lang == Lang.AR) return if (n == 0L) AR_WORD.getValue(unit).other else AR_WORD.getValue(unit).pick(arabicForm(n))
  val (one, many) = EN.getValue(unit)
  return if (n == 1L) one else many
}

fun unitWord(unit: CountUnit, n: Int, lang: Lang): String = unitWord(unit, n.toLong(), lang)

/** Joins non-zero parts: "2 years, 3 months and 5 days" / "سنتان و٣ أشهر و٥ أيام". */
fun joinParts(parts: List<String>, lang: Lang): String {
  if (parts.size <= 1) return parts.joinToString("")
  if (lang == Lang.AR) return parts.joinToString(" و")
  return "${parts.dropLast(1).joinToString(", ")} and ${parts.last()}"
}
