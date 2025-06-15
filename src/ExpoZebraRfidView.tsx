import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoZebraRfidViewProps } from './ExpoZebraRfid.types';

const NativeView: React.ComponentType<ExpoZebraRfidViewProps> =
  requireNativeView('ExpoZebraRfid');

export default function ExpoZebraRfidView(props: ExpoZebraRfidViewProps) {
  return <NativeView {...props} />;
}
