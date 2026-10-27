package com.devlance.dayscounter

import android.app.Application
import com.devlance.dayscounter.data.AppStore
import com.devlance.dayscounter.i18n.CoreAssets
import com.devlance.dayscounter.widget.WidgetSync

class DaysCounterApp : Application() {
  override fun onCreate() {
    super.onCreate()
    CoreAssets.opener = { name -> assets.open(name) }
    AppStore.init(this)
    WidgetSync.start(this)
  }
}
