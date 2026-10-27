package com.devlance.dayscounter.data

import com.devlance.dayscounter.core.CountdownColor
import com.devlance.dayscounter.core.CountdownIcon
import com.devlance.dayscounter.core.CountdownTest
import com.devlance.dayscounter.core.CountdownTest.Companion.at
import com.devlance.dayscounter.core.Lang
import com.devlance.dayscounter.core.LocalDateTime
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.TimeZone

/** The countdown list's rules (v1's useCountdownStore) and the persisted JSON, including v1's own
 * stored values. The iOS app has the same cases (StoreTests.swift). */
class StoreTest {
  companion object {
    @BeforeClass
    @JvmStatic
    fun beirut() {
      TimeZone.setDefault(CountdownTest.BEIRUT)
    }

    /** Exactly what v1 1.0.1 stored on the emulator (AsyncStorage → RKStorage), 2026-10-27. */
    const val V1_SETTINGS = """{"state":{"language":"en","themeMode":"system","showSeconds":true,"countUp":true,"use24h":false},"version":0}"""
    const val V1_COUNTDOWNS = """{"state":{"countdowns":[{"id":"mvqw3prnwzmfi8","title":"","target":{"year":2027,"month":9,"day":5,"hour":0,"minute":0},"color":"ember","icon":"calendar","createdAt":1793117904179}],"pinnedId":"mvqw3prnwzmfi8","seeded":true},"version":0}"""
  }

  private val now = at(2026, 10, 27, 12)
  private val defaults = Settings(language = Lang.AR, use24h = true)
  private fun input(title: String, target: LocalDateTime, color: CountdownColor = CountdownColor.VIOLET) =
    CountdownInput(title, target, color, CountdownIcon.STAR)

  @Test
  fun firstLaunchSeedsTheUsersExampleOnce() {
    val seeded = CountdownsState().seeded("seed", now)
    assertTrue(seeded.seeded)
    assertEquals(1, seeded.countdowns.size)
    val c = seeded.countdowns[0]
    assertEquals("", c.title)
    assertEquals(SEED_TARGET, c.target)
    assertEquals(CountdownColor.EMBER, c.color)
    assertEquals(CountdownIcon.CALENDAR, c.icon)
    assertEquals(now, c.createdAt)
    assertEquals("seed", seeded.pinnedId)
    // Deleting everything doesn't bring it back.
    val emptied = seeded.removeAll()
    assertEquals(emptied, emptied.seeded("again", now))
    assertTrue(emptied.countdowns.isEmpty())
    assertNull(emptied.pinnedId)
  }

  @Test
  fun addTrimsAndPinsTheFirstOrWhenAsked() {
    var s = CountdownsState(seeded = true)
    s = s.add(input("  Trip  ", LocalDateTime(2027, 1, 10, 0, 0)), pin = false, id = "a", now = now)
    assertEquals("Trip", s.countdowns.single().title)
    assertEquals("a", s.pinnedId) // nothing was pinned yet
    s = s.add(input("Exam", LocalDateTime(2026, 12, 1, 9, 0)), pin = false, id = "b", now = now)
    assertEquals("a", s.pinnedId)
    s = s.add(input("Birthday", LocalDateTime(2027, 3, 3, 0, 0)), pin = true, id = "c", now = now)
    assertEquals("c", s.pinnedId)
    assertEquals("c", s.pinned?.id)
  }

  @Test
  fun updateKeepsIdAndCreationTime() {
    var s = CountdownsState(seeded = true).add(input("Trip", LocalDateTime(2027, 1, 10, 0, 0)), false, "a", now)
    s = s.update("a", CountdownInput(" Trip to Paris ", LocalDateTime(2027, 2, 1, 8, 30), CountdownColor.OCEAN, CountdownIcon.PLANE))
    val c = s.countdowns.single()
    assertEquals("a", c.id)
    assertEquals(now, c.createdAt)
    assertEquals("Trip to Paris", c.title)
    assertEquals(CountdownColor.OCEAN, c.color)
    assertEquals(CountdownIcon.PLANE, c.icon)
  }

  @Test
  fun removingThePinnedOneHandsItToTheNextDue() {
    var s = CountdownsState(seeded = true)
    s = s.add(input("Passed", LocalDateTime(2026, 1, 1, 0, 0)), false, "past", now)
    s = s.add(input("Far", LocalDateTime(2028, 1, 1, 0, 0)), false, "far", now)
    s = s.add(input("Soon", LocalDateTime(2026, 11, 2, 0, 0)), true, "soon", now)
    assertEquals("far", s.remove("soon", now).pinnedId)
    // Removing another one keeps the pin.
    assertEquals("soon", s.remove("far", now).pinnedId)
    // Pinning an unknown id does nothing.
    assertEquals(s, s.pin("nope"))
    assertEquals("past", s.pin("past").pinnedId)
  }

  @Test
  fun sortsUpcomingSoonestFirstThenPassedMostRecentFirst() {
    var s = CountdownsState(seeded = true)
    s = s.add(input("old", LocalDateTime(2025, 1, 1, 0, 0)), false, "old", now)
    s = s.add(input("far", LocalDateTime(2028, 1, 1, 0, 0)), false, "far", now)
    s = s.add(input("recent", LocalDateTime(2026, 10, 1, 0, 0)), false, "recent", now)
    s = s.add(input("soon", LocalDateTime(2026, 11, 2, 0, 0)), false, "soon", now)
    assertEquals(listOf("soon", "far", "recent", "old"), sortCountdowns(s.countdowns, now).map { it.id })
  }

  @Test
  fun jsonRoundTrips() {
    var s = CountdownsState().seeded("seed", now)
    s = s.add(CountdownInput("Graduation", LocalDateTime(2027, 6, 30, 18, 30), CountdownColor.GOLD, CountdownIcon.GRADUATION), true, "g", now)
    assertEquals(s, StoreJson.decodeCountdowns(JSONObject(StoreJson.encodeCountdowns(s).toString()), now))
    val settings = Settings(Lang.AR, ThemeMode.DARK, showSeconds = false, countUp = false, use24h = true)
    assertEquals(settings, StoreJson.decodeSettings(JSONObject(StoreJson.encodeSettings(settings).toString()), defaults.copy(language = Lang.EN)))
  }

  @Test
  fun decodesV1sStoredState() {
    val settings = StoreJson.decodeSettings(LegacyMigration.state(V1_SETTINGS), defaults)
    assertEquals(Settings(Lang.EN, ThemeMode.SYSTEM, showSeconds = true, countUp = true, use24h = false), settings)

    val state = StoreJson.decodeCountdowns(LegacyMigration.state(V1_COUNTDOWNS), now)
    assertTrue(state.seeded)
    assertEquals("mvqw3prnwzmfi8", state.pinnedId)
    val c = state.countdowns.single()
    assertEquals(LocalDateTime(2027, 9, 5, 0, 0), c.target)
    assertEquals(1793117904179L, c.createdAt)
    assertEquals(CountdownColor.EMBER, c.color)
    // Already seeded in v1: no second example.
    assertEquals(state, state.seeded("x", now))
  }

  @Test
  fun sanitizesWhatItDoesntUnderstand() {
    val json = JSONObject(
      """{"countdowns":[
        {"id":"ok","title":5,"target":{"year":2027,"month":2,"day":3,"hour":4,"minute":5},"color":"neon","icon":"rocket"},
        {"id":"bad-date","target":{"year":2027,"month":2,"day":30,"hour":0,"minute":0}},
        {"id":"bad-hour","target":{"year":2027,"month":2,"day":3,"hour":24,"minute":0}},
        {"id":"fraction","target":{"year":2027,"month":2,"day":3.5,"hour":0,"minute":0}},
        {"title":"no id","target":{"year":2027,"month":2,"day":3,"hour":0,"minute":0}},
        "junk"
      ],"pinnedId":"gone","seeded":"yes"}""",
    )
    val state = StoreJson.decodeCountdowns(json, now)
    val c = state.countdowns.single()
    assertEquals("ok", c.id)
    assertEquals("", c.title)
    assertEquals(CountdownColor.EMBER, c.color)
    assertEquals(CountdownIcon.CALENDAR, c.icon)
    assertEquals(now, c.createdAt)
    assertEquals("ok", state.pinnedId) // unknown pin → the first one
    assertFalse(state.seeded) // only a real `true` counts

    val settings = StoreJson.decodeSettings(JSONObject("""{"language":"fr","themeMode":"neon","showSeconds":"no","use24h":false}"""), defaults)
    assertEquals(defaults.copy(use24h = false), settings)
    assertEquals(defaults, StoreJson.decodeSettings(null, defaults))
    assertEquals(CountdownsState(), StoreJson.decodeCountdowns(null, now))
  }

  @Test
  fun idsLookLikeV1s() {
    val id = newCountdownId(1793117904179L)
    assertTrue(id.startsWith(1793117904179L.toString(36)))
    assertEquals(1793117904179L.toString(36).length + 6, id.length)
  }
}
