package com.devlance.dayscounter.i18n

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The i18n files (identical copies in the iOS app — CI compares them) parse, and every key in one
 * language exists in the other. */
class LocalesTest {
  private fun keyPaths(lang: String): Set<String> {
    val out = HashSet<String>()
    fun walk(node: Any, prefix: String) {
      if (node is JSONObject) {
        for (key in node.keys()) walk(node.get(key), if (prefix.isEmpty()) key else "$prefix.$key")
      } else {
        out += prefix
      }
    }
    walk(JSONObject(File("src/main/assets/i18n/$lang.json").readText()), "")
    return out
  }

  @Test
  fun englishAndArabicHaveTheSameKeys() {
    val en = keyPaths("en")
    val ar = keyPaths("ar")
    assertTrue(en.size > 100)
    assertEquals("missing in ar.json", emptySet<String>(), en - ar)
    assertEquals("missing in en.json", emptySet<String>(), ar - en)
  }
}
