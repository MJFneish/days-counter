import { TurboModuleRegistry, type TurboModule } from 'react-native';

/**
 * Turbo Native Module behind the home-screen widget (Android only — `DaysWidgetModule.kt`).
 * Codegen reads this file (package.json → codegenConfig) and generates `NativeDaysWidgetSpec`,
 * which the Kotlin module extends. `get` (not `getEnforcing`) so iOS, which has no native side,
 * gets `null` and the app simply skips widget calls.
 */
export interface Spec extends TurboModule {
  /** Stores the widget payload (JSON, see src/widget/payload.ts) and redraws every placed widget. */
  setPayload(json: string): void;
  /** Asks the launcher to place a widget (Android 8+). Resolves false when the launcher can't. */
  requestPin(): Promise<boolean>;
  /** Whether the launcher supports `requestPin`. */
  isRequestPinSupported(): Promise<boolean>;
  /** How many widgets are on the home screen right now. */
  getInstalledCount(): Promise<number>;
}

export default TurboModuleRegistry.get<Spec>('DaysWidget');
