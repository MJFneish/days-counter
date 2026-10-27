package com.devlance.dayscounter.core

import com.devlance.dayscounter.core.CountdownTest.Companion.USER_TARGET
import com.devlance.dayscounter.core.CountdownTest.Companion.at
import com.devlance.dayscounter.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.util.TimeZone

/** Port of v1's phrase tests (countdown.test.ts "phrases" + present.test.ts), over the real locale
 * files. The iOS app has the same cases (PhrasesTests.swift). */
class PhrasesTest {
  companion object {
    @BeforeClass
    @JvmStatic
    fun beirut() {
      TimeZone.setDefault(CountdownTest.BEIRUT)
    }
  }

  private val en = Strings.of(Lang.EN)
  private val ar = Strings.of(Lang.AR)
  private val r = remaining(USER_TARGET, at(2026, 10, 23, 15))

  private val seed = Countdown("seed", "", USER_TARGET, CountdownColor.EMBER, CountdownIcon.CALENDAR, at(2026, 10, 23))
  private val named = seed.copy(id = "named", title = "Graduation", target = USER_TARGET.copy(hour = 18, minute = 30))

  @Test
  fun english() {
    assertEquals("10 months and 12 days", spanPhrase(USER_TARGET, r, Lang.EN))
    assertEquals("45 weeks and 1 day", weeksPhrase(r, Lang.EN))
    assertEquals("7,593", totalHours(r, Lang.EN))
    assertEquals("1 day", countPhrase(CountUnit.DAY, 1, Lang.EN))
    assertEquals("second", unitWord(CountUnit.SECOND, 1, Lang.EN))
    assertEquals("Sunday, 5 September 2027", formatLongDate(USER_TARGET, Lang.EN))
    assertEquals("5 Sep 2027", formatShortDate(USER_TARGET, Lang.EN))
    assertEquals("6:05 PM", formatClock(18, 5, Lang.EN, false))
    assertEquals("12:00 AM", formatClock(0, 0, Lang.EN, false))
    assertEquals("00:00", formatClock(0, 0, Lang.EN, true))
    assertEquals("62%", formatPercent(62, Lang.EN))
    assertEquals("2 years, 3 months and 5 days", joinParts(listOf("2 years", "3 months", "5 days"), Lang.EN))
    assertEquals("9,780", formatNumber(9780, Lang.EN))
    assertEquals("1,234,567", formatNumber(1234567, Lang.EN))
    assertEquals("-12", formatNumber(-12, Lang.EN))
  }

  @Test
  fun arabicPluralForms() {
    assertEquals("يوم واحد", countPhrase(CountUnit.DAY, 1, Lang.AR))
    assertEquals("يومان", countPhrase(CountUnit.DAY, 2, Lang.AR))
    assertEquals("٥ أيام", countPhrase(CountUnit.DAY, 5, Lang.AR))
    assertEquals("١٢ يومًا", countPhrase(CountUnit.DAY, 12, Lang.AR))
    assertEquals("٣١٦ يومًا", countPhrase(CountUnit.DAY, 316, Lang.AR))
    assertEquals("٣٠٠ يوم", countPhrase(CountUnit.DAY, 300, Lang.AR))
    assertEquals("٠ ساعة", countPhrase(CountUnit.HOUR, 0, Lang.AR))
    assertEquals("يومًا", unitWord(CountUnit.DAY, 316, Lang.AR))
    assertEquals("دقائق", unitWord(CountUnit.MINUTE, 7, Lang.AR))
    assertEquals("١٠ أشهر و١٢ يومًا", spanPhrase(USER_TARGET, r, Lang.AR))
    assertEquals("الأحد ٥ أيلول ٢٠٢٧", formatLongDate(USER_TARGET, Lang.AR))
    assertEquals("٥ أيلول ٢٠٢٧", formatShortDate(USER_TARGET, Lang.AR))
    assertEquals("٦:٠٥ م", formatClock(18, 5, Lang.AR, false))
    assertEquals("٦٢٪", formatPercent(62, Lang.AR))
    assertEquals("٩٬٧٨٠", formatNumber(9780, Lang.AR))
    assertEquals("سنتان و٣ أشهر و٥ أيام", joinParts(listOf("سنتان", "٣ أشهر", "٥ أيام"), Lang.AR))
  }

  @Test
  fun unnamedCountdownIsTitledByItsDate() {
    assertEquals("Sunday, 5 September 2027", countdownTitle(seed, Lang.EN))
    assertEquals("", countdownSubtitle(seed, en, true))
  }

  @Test
  fun namedCountdownShowsDateAndTimeUnderneath() {
    assertEquals("Graduation", countdownTitle(named, Lang.EN))
    assertEquals("Sunday, 5 September 2027 · at 18:30", countdownSubtitle(named, en, true))
    assertEquals("5 Sep 2027 · at 6:30 PM", countdownSubtitle(named, en, false, short = true))
    assertEquals("٥ أيلول ٢٠٢٧ · الساعة ٦:٣٠ م", countdownSubtitle(named, ar, false, short = true))
  }

  @Test
  fun relativePhrases() {
    assertEquals("in 316 days", relativePhrase(remaining(seed.target, at(2026, 10, 23, 15)), en))
    assertEquals("in 5 hours", relativePhrase(remaining(seed.target, at(2027, 9, 4, 19)), en))
    assertEquals(Headline(1, CountUnit.MINUTE), headline(remaining(seed.target, at(2027, 9, 4, 23, 59))))
    assertEquals("Today", relativePhrase(remaining(seed.target, at(2027, 9, 5, 9)), en))
    assertEquals("3 days ago", relativePhrase(remaining(seed.target, at(2027, 9, 8, 9)), en))
    assertEquals("بعد ٣١٦ يومًا", relativePhrase(remaining(seed.target, at(2026, 10, 23, 15)), ar))
    assertEquals("منذ ٣ أيام", relativePhrase(remaining(seed.target, at(2027, 9, 8, 9)), ar))
  }

  @Test
  fun versionsStayLeftToRightInArabic() {
    assertEquals("${Char(0x2066)}٢.٠.٠-alpha.٥${Char(0x2069)}", ltr(localizeDigits("2.0.0-alpha.5", Lang.AR)))
  }

  @Test
  fun stringsInterpolateAndFallBack() {
    assertEquals("Edit Graduation", en.t("home.edit", "name" to "Graduation"))
    assertEquals("Days Counter", en["app.name"])
    assertEquals("عدّاد الأيام", ar["app.name"])
    assertEquals("no.such.key", ar["no.such.key"])
  }
}
