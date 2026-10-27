package com.devlance.dayscounter.widget

import com.devlance.dayscounter.core.Countdown
import com.devlance.dayscounter.core.CountdownColor
import com.devlance.dayscounter.core.CountdownIcon
import com.devlance.dayscounter.core.CountdownTest
import com.devlance.dayscounter.core.CountdownTest.Companion.at
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.core.LocalDateTime
import com.devlance.dayscounter.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.TimeZone

/** v1's widget payload test (present.test.ts), the JSON round trip, v1's own stored payload, and
 * when the widget wakes up next. */
class WidgetPayloadTest {
  companion object {
    @BeforeClass
    @JvmStatic
    fun beirut() {
      TimeZone.setDefault(CountdownTest.BEIRUT)
    }

    /** What v1 1.0.1 stored in shared_prefs/days_widget.xml on the emulator (2026-10-27). */
    const val V1_PAYLOAD = """{"v":1,"lang":"en","rtl":false,"countUp":true,"countdown":{"title":"Sunday, 5 September 2027","detail":"","target":{"year":2027,"month":9,"day":5,"hour":0,"minute":0},"createdAt":1793117904179,"color":"ember"},"strings":{"toGo":"to go","since":"since","arrived":"The day is here!","passed":"This date has passed","empty":"No countdowns yet","progress":"{p} of the wait done"}}"""
  }

  private val named = Countdown("named", "Graduation", LocalDateTime(2027, 9, 5, 18, 30), CountdownColor.EMBER, CountdownIcon.CALENDAR, at(2026, 10, 23))

  @Test
  fun carriesTheWallClockTargetAndTheLocalizedStrings() {
    val ar = Strings.of(Lang.AR)
    val p = WidgetPayload.build(named, ar, countUp = true, use24h = true)
    assertEquals("ar", p.lang)
    assertTrue(p.rtl)
    assertTrue(p.countUp)
    assertEquals("Graduation", p.countdown?.title)
    assertEquals(named.target, p.countdown?.target)
    assertEquals("ember", p.countdown?.color)
    assertEquals(named.createdAt, p.countdown?.createdAt)
    assertEquals("٥ أيلول ٢٠٢٧ · الساعة ١٨:٣٠", p.countdown?.detail)
    assertEquals("مضى {p} من الانتظار", p.strings.progress)
    assertEquals(ar["countdown.toGo"], p.strings.toGo)
  }

  @Test
  fun isEmptyButValidWithNoCountdown() {
    val en = Strings.of(Lang.EN)
    val p = WidgetPayload.build(null, en, countUp = false, use24h = true)
    assertNull(p.countdown)
    assertEquals(en["home.emptyTitle"], p.strings.empty)
    assertEquals(p, WidgetPayload.parse(p.toJson().toString()))
  }

  @Test
  fun roundTripsAndReadsV1sPayload() {
    val p = WidgetPayload.build(named, Strings.of(Lang.EN), countUp = true, use24h = false)
    assertEquals(p, WidgetPayload.parse(p.toJson().toString()))
    val v1 = WidgetPayload.parse(V1_PAYLOAD)!!
    assertEquals("Sunday, 5 September 2027", v1.countdown?.title)
    assertEquals(LocalDateTime(2027, 9, 5, 0, 0), v1.countdown?.target)
    assertEquals("{p} of the wait done", v1.strings.progress)
    // The payload a fresh v2 builds for the same countdown is v1's, string for string.
    val same = Countdown("x", "", LocalDateTime(2027, 9, 5, 0, 0), CountdownColor.EMBER, CountdownIcon.CALENDAR, 1793117904179L)
    assertEquals(v1, WidgetPayload.build(same, Strings.of(Lang.EN), countUp = true, use24h = false))
    assertNull(WidgetPayload.parse("not json"))
    assertNull(WidgetPayload.parse(null))
  }

  @Test
  fun wakesAnHourBeforeEachDayChangeAndAtIt() {
    val p = WidgetPayload.build(named, Strings.of(Lang.EN), countUp = true, use24h = true)
    // 23 Oct 15:00 → the day changes at 18:30; wake at 17:30 (to show 0:MM:SS), then at 18:30.
    assertEquals(at(2026, 10, 23, 17, 30), DaysWidgetProvider.nextRefreshAt(p, at(2026, 10, 23, 15)))
    assertEquals(at(2026, 10, 23, 18, 30), DaysWidgetProvider.nextRefreshAt(p, at(2026, 10, 23, 17, 45)))
    // After the date, counting up: an hour after the last change, then the next change.
    assertEquals(at(2027, 9, 6, 19, 30), DaysWidgetProvider.nextRefreshAt(p, at(2027, 9, 6, 19)))
    assertEquals(at(2027, 9, 7, 18, 30), DaysWidgetProvider.nextRefreshAt(p, at(2027, 9, 6, 20)))
    // Without count-up: only the end of the "arrived" day, then nothing.
    val noCountUp = p.copy(countUp = false)
    assertEquals(at(2027, 9, 6, 18, 30), DaysWidgetProvider.nextRefreshAt(noCountUp, at(2027, 9, 5, 20)))
    assertNull(DaysWidgetProvider.nextRefreshAt(noCountUp, at(2027, 9, 7, 9)))
    assertNull(DaysWidgetProvider.nextRefreshAt(null, at(2027, 9, 7, 9)))
  }
}
