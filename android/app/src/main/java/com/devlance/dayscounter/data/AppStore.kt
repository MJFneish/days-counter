package com.devlance.dayscounter.data

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import com.devlance.dayscounter.core.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.Locale

/**
 * The app's persisted state — settings and countdowns — as observable flows, saved to
 * SharedPreferences on every change (v1: two zustand stores over AsyncStorage). Loaded
 * synchronously at startup (a few KB), so the first frame already has the user's language and
 * countdowns. On the first launch after updating from v1, the state is imported from v1's storage
 * (LegacyMigration). Same structure as Mawakit's AppStore.
 */
object AppStore {
  private const val PREFS = "days_counter"
  private const val KEY_SETTINGS = "settings"
  private const val KEY_COUNTDOWNS = "countdowns"

  private lateinit var prefs: SharedPreferences

  private val _settings = MutableStateFlow(Settings(language = Lang.EN, use24h = true))
  val settings: StateFlow<Settings> = _settings.asStateFlow()

  private val _countdowns = MutableStateFlow(CountdownsState())
  val countdowns: StateFlow<CountdownsState> = _countdowns.asStateFlow()

  @Synchronized
  fun init(context: Context) {
    if (::prefs.isInitialized) return
    prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    // First-launch defaults, as in v1: the phone's language and 12/24-hour clock.
    val defaults = Settings(
      language = if (Locale.getDefault().language == "ar") Lang.AR else Lang.EN,
      use24h = DateFormat.is24HourFormat(context),
    )

    if (!prefs.contains(KEY_SETTINGS) && !prefs.contains(KEY_COUNTDOWNS)) {
      // First launch of v2: carry v1's state over (if this install was v1), else start fresh.
      val legacy = LegacyMigration.read(context)
      prefs.edit()
        .apply { legacy.settings?.let { putString(KEY_SETTINGS, it.toString()) } }
        .apply { legacy.countdowns?.let { putString(KEY_COUNTDOWNS, it.toString()) } }
        .commit()
    }

    _settings.value = StoreJson.decodeSettings(read(KEY_SETTINGS), defaults)
    val loaded = StoreJson.decodeCountdowns(read(KEY_COUNTDOWNS), now)
    _countdowns.value = loaded.seeded(newCountdownId(now), now)
    saveSettings()
    saveCountdowns()
  }

  fun updateSettings(transform: (Settings) -> Settings) {
    _settings.value = transform(_settings.value)
    saveSettings()
  }

  fun updateCountdowns(transform: (CountdownsState) -> CountdownsState) {
    _countdowns.value = transform(_countdowns.value)
    saveCountdowns()
  }

  private fun read(key: String): JSONObject? = prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }

  private fun saveSettings() {
    prefs.edit().putString(KEY_SETTINGS, StoreJson.encodeSettings(_settings.value).toString()).apply()
  }

  private fun saveCountdowns() {
    prefs.edit().putString(KEY_COUNTDOWNS, StoreJson.encodeCountdowns(_countdowns.value).toString()).apply()
  }
}
