import { NativeModule, requireNativeModule } from 'expo';

import { ExpoZebraRfidModuleEvents } from './ExpoZebraRfid.types';

declare class ExpoZebraRfidModule extends NativeModule<ExpoZebraRfidModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoZebraRfidModule>('ExpoZebraRfid');
