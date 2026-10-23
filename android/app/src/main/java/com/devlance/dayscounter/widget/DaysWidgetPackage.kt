package com.devlance.dayscounter.widget

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

/** Registers the app-local `DaysWidget` Turbo Module (added by hand in MainApplication — app code
 * isn't autolinked). */
class DaysWidgetPackage : BaseReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
    if (name == DaysWidgetModule.NAME) DaysWidgetModule(reactContext) else null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider {
    mapOf(
      DaysWidgetModule.NAME to ReactModuleInfo(
        name = DaysWidgetModule.NAME,
        className = DaysWidgetModule.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = true,
      ),
    )
  }
}
