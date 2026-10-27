// swift-tools-version:5.9
// The countdown engine and app logic shared by the iOS app and its widget: a 1:1 port of the
// Android app's Kotlin core (android/app/src/main/java/com/devlance/dayscounter/core), with the
// same unit tests. Same layout as Mawakit's MawaqitCore.
import PackageDescription

let package = Package(
  name: "DaysCounterCore",
  platforms: [.iOS(.v17), .macOS(.v14)],
  products: [
    .library(name: "DaysCounterCore", targets: ["DaysCounterCore"]),
  ],
  targets: [
    .target(
      name: "DaysCounterCore",
      resources: [.copy("Resources")]
    ),
    .testTarget(
      name: "DaysCounterCoreTests",
      dependencies: ["DaysCounterCore"]
    ),
  ]
)
