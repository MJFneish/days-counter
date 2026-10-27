import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

val appVersion = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }

android {
  namespace = "com.devlance.dayscounter"
  compileSdk = 36

  defaultConfig {
    // Same id and signing key as v1 (the React Native app), so v2 installs over it as an update and
    // keeps its countdowns, settings and placed widgets.
    applicationId = "com.devlance.dayscounter"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersion.getProperty("VERSION_CODE").toInt()
    versionName = appVersion.getProperty("VERSION_NAME")
  }

  signingConfigs {
    // v1's builds (debug and release) were signed with the RN template's debug keystore — the
    // public "android" debug key. An update must carry the same signature, so v2 keeps it.
    create("dayscounter") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("dayscounter")
    }
    release {
      signingConfig = signingConfigs.getByName("dayscounter")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.kotlinx.coroutines.android)
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.foundation)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui.tooling.preview)
  debugImplementation(libs.compose.ui.tooling)

  testImplementation(libs.junit)
  // android.jar's org.json is a stub on the JVM; the real one for unit tests.
  testImplementation(libs.org.json)
}
