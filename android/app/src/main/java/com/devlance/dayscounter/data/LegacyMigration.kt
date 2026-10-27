package com.devlance.dayscounter.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

/**
 * Reads what v1 (the React Native app) saved, for the first launch after the update. v1 kept two
 * zustand stores in AsyncStorage, which on Android is the SQLite database "RKStorage", table
 * "catalystLocalStorage" (key, value). Each value is `{"state": {...}, "version": 0}`; the state
 * objects have the shapes v2 persists (StoreJson), so they're handed over as-is. The old database
 * is left in place (read-only access, a few KB). Same approach as Mawakit's LegacyMigration.
 */
object LegacyMigration {
  data class Legacy(val settings: JSONObject?, val countdowns: JSONObject?)

  private const val DATABASE = "RKStorage"
  private const val TABLE = "catalystLocalStorage"
  const val KEY_SETTINGS = "days-counter-settings"
  const val KEY_COUNTDOWNS = "days-counter-countdowns"

  fun read(context: Context): Legacy {
    val file = context.getDatabasePath(DATABASE)
    if (!file.exists()) return Legacy(null, null)
    return try {
      SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        val values = HashMap<String, String>()
        db.rawQuery("SELECT key, value FROM $TABLE WHERE key IN ('$KEY_SETTINGS', '$KEY_COUNTDOWNS')", null).use { cursor ->
          while (cursor.moveToNext()) values[cursor.getString(0)] = cursor.getString(1)
        }
        Legacy(state(values[KEY_SETTINGS]), state(values[KEY_COUNTDOWNS]))
      }
    } catch (e: Exception) {
      Legacy(null, null)
    }
  }

  /** The `state` object inside a zustand persist value. */
  fun state(value: String?): JSONObject? = value?.let { runCatching { JSONObject(it).optJSONObject("state") }.getOrNull() }
}
