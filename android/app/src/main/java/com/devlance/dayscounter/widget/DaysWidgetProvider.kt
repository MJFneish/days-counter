package com.devlance.dayscounter.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.devlance.dayscounter.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor

/**
 * Home-screen (and, where the device allows it, lock-screen) widget for the pinned countdown.
 *
 * The day count is plain text; the H:MM:SS part is a `Chronometer` that ticks by itself, counting
 * down to the next day change (or up from the last one, after the date). The provider wakes itself
 * with an alarm when the day count changes, and one hour before that to switch the Chronometer to a
 * "0:MM:SS" format (by default it drops the hour under 60 minutes, which would read as H:MM).
 * Same scheduling approach as prayer-times-lb's widget.
 *
 * Four layouts share their view ids. Android 12+ gets all four as a size map and switches live
 * while the user resizes; older versions pick one from the reported size.
 */
class DaysWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
    render(context, manager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
    render(context, manager, intArrayOf(appWidgetId))
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    when (intent.action) {
      ACTION_REFRESH,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      Intent.ACTION_DATE_CHANGED,
      Intent.ACTION_LOCALE_CHANGED,
      Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAll(context)
    }
  }

  override fun onDisabled(context: Context) {
    alarmManager(context).cancel(refreshIntent(context))
  }

  private enum class Size { SMALL, SQUARE, WIDE, LARGE }

  companion object {
    const val ACTION_REFRESH = "com.devlance.dayscounter.widget.REFRESH"
    private const val HOUR_MS = 3_600_000L

    // Ideal sizes (dp) for the Android 12+ size map: the launcher shows the closest one that fits.
    private val SIZE_SMALL = SizeF(40f, 40f)
    private val SIZE_SQUARE = SizeF(100f, 100f)
    private val SIZE_WIDE = SizeF(180f, 50f)
    private val SIZE_LARGE = SizeF(200f, 110f)

    fun installedIds(context: Context): IntArray =
      AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, DaysWidgetProvider::class.java))

    fun refreshAll(context: Context) {
      render(context, AppWidgetManager.getInstance(context), installedIds(context))
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
      if (ids.isEmpty()) {
        alarmManager(context).cancel(refreshIntent(context))
        return
      }
      val now = System.currentTimeMillis()
      val payload = WidgetStore.load(context)
      val views = Size.entries.associateWith { buildViews(context, payload, now, it) }

      for (id in ids) {
        val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          RemoteViews(
            mapOf(
              SIZE_SMALL to views.getValue(Size.SMALL),
              SIZE_SQUARE to views.getValue(Size.SQUARE),
              SIZE_WIDE to views.getValue(Size.WIDE),
              SIZE_LARGE to views.getValue(Size.LARGE),
            ),
          )
        } else {
          views.getValue(sizeFromOptions(manager.getAppWidgetOptions(id)))
        }
        manager.updateAppWidget(id, remoteViews)
      }
      scheduleRefresh(context, nextRefreshAt(payload, now))
    }

    /** Pre-Android 12: portrait width is MIN_WIDTH, portrait height MAX_HEIGHT. Same thresholds
     * as the size map above. */
    private fun sizeFromOptions(options: Bundle): Size {
      val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
      val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
      if (w == 0 || h == 0) return Size.LARGE
      return when {
        w >= SIZE_LARGE.width && h >= SIZE_LARGE.height -> Size.LARGE
        w >= SIZE_SQUARE.width && h >= SIZE_SQUARE.height -> Size.SQUARE
        w >= SIZE_WIDE.width -> Size.WIDE
        else -> Size.SMALL
      }
    }

    private fun layoutFor(size: Size) = when (size) {
      Size.SMALL -> R.layout.widget_small
      Size.SQUARE -> R.layout.widget_square
      Size.WIDE -> R.layout.widget_wide
      Size.LARGE -> R.layout.widget_large
    }

    /** Resources in the app's language (not the phone's), for the plural unit words. */
    private fun localized(context: Context, lang: String): Context {
      val config = Configuration(context.resources.configuration)
      config.setLocale(Locale.forLanguageTag(lang))
      return context.createConfigurationContext(config)
    }

    private fun buildViews(context: Context, payload: WidgetPayload?, now: Long, size: Size): RemoteViews {
      val views = RemoteViews(context.packageName, layoutFor(size))
      launchAppIntent(context)?.let { views.setOnClickPendingIntent(android.R.id.background, it) }
      val hasTitle = size != Size.SMALL
      val hasDetail = size == Size.WIDE || size == Size.LARGE
      val hasChrono = size != Size.SMALL
      val hasProgress = size == Size.LARGE

      val countdown = payload?.countdown
      views.setInt(
        android.R.id.background,
        "setLayoutDirection",
        if (payload?.rtl == true) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
      )
      views.setInt(android.R.id.background, "setBackgroundResource", WidgetColors.background(countdown?.color ?: "ember"))

      if (payload == null || countdown == null) {
        // Never opened, or every countdown deleted: invite the user into the app.
        val message = payload?.strings?.empty?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.widget_open_app)
        if (hasTitle) views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_label))
        if (hasDetail) views.setViewVisibility(R.id.widget_detail, View.GONE)
        views.setViewVisibility(R.id.widget_days, View.GONE)
        views.setTextViewText(R.id.widget_unit, message)
        if (hasChrono) views.setViewVisibility(R.id.widget_chrono, View.GONE)
        if (hasProgress) {
          views.setViewVisibility(R.id.widget_progress, View.GONE)
          views.setViewVisibility(R.id.widget_percent, View.GONE)
        }
        return views
      }

      val strings = payload.strings
      val r = Countdown.remaining(countdown.target, now)
      val upcoming = r.status == Status.UPCOMING
      val showCount = upcoming || payload.countUp

      if (hasTitle) views.setTextViewText(R.id.widget_title, countdown.title)
      if (hasDetail) {
        views.setTextViewText(R.id.widget_detail, countdown.detail)
        views.setViewVisibility(R.id.widget_detail, if (countdown.detail.isEmpty()) View.GONE else View.VISIBLE)
      }

      if (showCount) {
        val unit = localized(context, payload.lang).resources.getQuantityString(R.plurals.widget_days, r.days)
        views.setViewVisibility(R.id.widget_days, View.VISIBLE)
        views.setTextViewText(R.id.widget_days, formatNumber(r.days, payload.lang))
        views.setTextViewText(
          R.id.widget_unit,
          if (r.status == Status.ARRIVED && size != Size.LARGE) strings.arrived
          else "$unit ${if (upcoming) strings.toGo else strings.since}",
        )
      } else {
        views.setViewVisibility(R.id.widget_days, View.GONE)
        views.setTextViewText(R.id.widget_unit, if (r.status == Status.ARRIVED) strings.arrived else strings.passed)
      }

      if (hasChrono) {
        if (showCount) {
          // Countdown: ticks down to the next day change. Count-up: ticks up from the last one.
          val offset = if (upcoming) r.nextChangeAt - now else now - r.boundaryAt
          val base = if (upcoming) SystemClock.elapsedRealtime() + offset else SystemClock.elapsedRealtime() - offset
          views.setChronometer(R.id.widget_chrono, base, if (offset < HOUR_MS) "0:%s" else null, true)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) views.setChronometerCountDown(R.id.widget_chrono, upcoming)
          views.setViewVisibility(R.id.widget_chrono, View.VISIBLE)
        } else {
          views.setViewVisibility(R.id.widget_chrono, View.GONE)
        }
      }

      if (hasProgress) {
        if (upcoming) {
          val p = Countdown.progress(countdown.target, countdown.createdAt, now)
          val percent = floor(p * 100).toInt()
          views.setProgressBar(R.id.widget_progress, 1000, (p * 1000).toInt().coerceAtLeast(20), false)
          views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
          views.setTextViewText(R.id.widget_percent, strings.progress.replace("{p}", formatPercent(percent, payload.lang)))
          views.setViewVisibility(R.id.widget_percent, View.VISIBLE)
        } else {
          views.setViewVisibility(R.id.widget_progress, View.GONE)
          views.setTextViewText(R.id.widget_percent, if (r.status == Status.ARRIVED) strings.arrived else "")
          views.setViewVisibility(R.id.widget_percent, if (r.status == Status.ARRIVED) View.VISIBLE else View.GONE)
        }
      }
      return views
    }

    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

    /** "9,780" / "٩٬٧٨٠" — same as formatNumber in src/i18n/plural.ts. Done by hand rather than
     * with NumberFormat: Android's "ar" locale formats with Latin digits (seen on the emulator),
     * while the app shows Arabic-Indic ones. */
    private fun formatNumber(n: Int, lang: String): String {
      val grouped = NumberFormat.getIntegerInstance(Locale.US).format(n)
      if (lang != "ar") return grouped
      return grouped.map { c ->
        when (c) {
          in '0'..'9' -> ARABIC_INDIC[c - '0']
          ',' -> '٬'
          else -> c
        }
      }.joinToString("")
    }

    /** "62%" / "٦٢٪" — same as formatPercent in src/core/format.ts. */
    private fun formatPercent(percent: Int, lang: String): String {
      val digits = formatNumber(percent, lang)
      return if (lang == "ar") "$digits٪" else "$digits%"
    }

    /** When the widget next needs redrawing: the day change, or one hour before it (to switch the
     * Chronometer to "0:MM:SS"); after the date, one hour after the last change, then the next. */
    private fun nextRefreshAt(payload: WidgetPayload?, now: Long): Long? {
      val countdown = payload?.countdown ?: return null
      val r = Countdown.remaining(countdown.target, now)
      return if (r.status == Status.UPCOMING) {
        val hourBefore = r.nextChangeAt - HOUR_MS
        if (hourBefore > now) hourBefore else r.nextChangeAt
      } else {
        if (!payload.countUp) return if (r.status == Status.ARRIVED) r.nextChangeAt else null
        val hourAfter = r.boundaryAt + HOUR_MS
        if (hourAfter > now) hourAfter else r.nextChangeAt
      }
    }

    /** Exact when the user has allowed "Alarms & reminders" (pre-granted on Android 12/12L),
     * otherwise a one-minute window; the 30-minute `updatePeriodMillis` is a further backstop. */
    private fun scheduleRefresh(context: Context, atMs: Long?) {
      val alarms = alarmManager(context)
      val pending = refreshIntent(context)
      alarms.cancel(pending)
      if (atMs == null) return
      val triggerAt = atMs + 500
      val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
      try {
        if (canExact) {
          alarms.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pending)
        } else {
          alarms.setWindow(AlarmManager.RTC, triggerAt, 60_000, pending)
        }
      } catch (e: SecurityException) {
        alarms.setWindow(AlarmManager.RTC, triggerAt, 60_000, pending)
      }
    }

    private fun alarmManager(context: Context): AlarmManager =
      context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun refreshIntent(context: Context): PendingIntent {
      val intent = Intent(context, DaysWidgetProvider::class.java).setAction(ACTION_REFRESH)
      return PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }

    private fun launchAppIntent(context: Context): PendingIntent? {
      val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
      return PendingIntent.getActivity(
        context, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
