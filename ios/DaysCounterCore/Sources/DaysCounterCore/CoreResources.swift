import Foundation

/// Where the bundled data files (the i18n texts) are read from: the package's `Resources` folder,
/// copied as-is into `Bundle.module` (the Android app reads the same files from its assets). Names
/// are relative to that folder, e.g. "i18n/en.json". Same helper as Mawakit's.
public enum CoreResources {
  /// URL of a bundled resource, or nil if it isn't there.
  public static func url(_ name: String) -> URL? {
    let path = "Resources/" + name
    let nsPath = path as NSString
    let directory = nsPath.deletingLastPathComponent
    let file = nsPath.lastPathComponent as NSString
    let ext = file.pathExtension
    let base = file.deletingPathExtension
    if let url = Bundle.module.url(forResource: base, withExtension: ext.isEmpty ? nil : ext, subdirectory: directory) {
      return url
    }
    // Flat bundles (SwiftPM on macOS) and full ones (Contents/Resources) both have a resource URL.
    if let root = Bundle.module.resourceURL {
      let candidate = root.appendingPathComponent(path)
      if FileManager.default.fileExists(atPath: candidate.path) { return candidate }
    }
    return nil
  }

  /// Contents of a bundled resource. A missing resource is a packaging error, so it stops loudly.
  public static func data(_ name: String) -> Data {
    guard let location = CoreResources.url(name), let bytes = try? Data(contentsOf: location) else {
      fatalError("DaysCounterCore resource missing: \(name)")
    }
    return bytes
  }
}
