import { registerWebModule, NativeModule } from 'expo';

import { ExpoZebraRfidModuleEvents } from './ExpoZebraRfid.types';

class ExpoZebraRfidModule extends NativeModule<ExpoZebraRfidModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoZebraRfidModule, 'ExpoZebraRfidModule');
