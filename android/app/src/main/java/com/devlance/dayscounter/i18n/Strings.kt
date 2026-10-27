package com.devlance.dayscounter.i18n

import com.devlance.dayscounter.core.Lang
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/** Where the bundled text files are read from: the APK's assets in the app (set at startup),
 * `src/main/assets` in unit tests. Same idea as Mawakit's CoreAssets. */
object CoreAssets {
  @Volatile
  var opener: (String) -> InputStream = { name -> File("src/main/assets/$name").inputStream() }

  fun readText(name: String): String = opener(name).bufferedReader().use { it.readText() }
}

/**
 * The app's texts in one language, from assets/i18n/<lang>.json (v1's i18next files, unchanged and
 * identical to the iOS app's): nested keys addressed with dots ("settings.language"), `{{name}}`
 * placeholders. The app's language is its own setting, independent of the phone's, and switches
 * instantly — so texts are looked up here rather than through Android string resources. Missing
 * keys fall back to English. Same class as Mawakit's.
 */
class Strings private constructor(val lang: Lang, private val table: Map<String, String>, private val fallback: Strings?) {
  operator fun get(key: String): String = table[key] ?: fallback?.get(key) ?: key

  fun t(key: String, vararg args: Pair<String, Any?>): String {
    var text = get(key)
    for ((name, value) in args) text = text.replace("{{$name}}", value.toString())
    return text
  }

  companion object {
    private val cache = ConcurrentHashMap<Lang, Strings>()

    fun of(lang: Lang): Strings = cache.getOrPut(lang) {
      Strings(lang, load(lang), if (lang == Lang.EN) null else of(Lang.EN))
    }

    private fun load(lang: Lang): Map<String, String> {
      val out = HashMap<String, String>()
      fun flatten(o: JSONObject, prefix: String) {
        for (key in o.keys()) {
          when (val value = o.get(key)) {
            is JSONObject -> flatten(value, "$prefix$key.")
            else -> out["$prefix$key"] = value.toString()
          }
        }
      }
      flatten(JSONObject(CoreAssets.readText("i18n/${lang.code}.json")), "")
      return out
    }
  }
}
